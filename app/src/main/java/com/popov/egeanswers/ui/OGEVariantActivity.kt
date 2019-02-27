package com.popov.egeanswers.ui

import android.app.ActivityManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetBehavior.STATE_COLLAPSED
import android.support.design.widget.BottomSheetBehavior.STATE_EXPANDED
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.github.barteksc.pdfviewer.PDFView
import com.google.firebase.analytics.FirebaseAnalytics
import com.popov.egeanswers.AnswersAdapter
import com.popov.egeanswers.R
import com.popov.egeanswers.viewmodel.OGEVariantViewModel
import com.popov.egeanswers.viewmodel.VariantViewModelFactory
import kotlinx.android.synthetic.main.activity_oge_variant.*
import org.jetbrains.anko.share
import org.jetbrains.anko.toast


class OGEVariantActivity : AppCompatActivity() {

    private lateinit var m: OGEVariantViewModel
    private lateinit var menu: Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_oge_variant)
        setSupportActionBar(myToolbar)

        var varNumber = intent.getIntExtra("varNumber", 1)

        intent.data?.path?.apply {
            varNumber = replaceBeforeLast('/', "").removePrefix("/trvar").removeSuffix("_oge.html").toInt()
        }

        m = ViewModelProviders
                .of(this, VariantViewModelFactory(this.application, varNumber))
                .get(OGEVariantViewModel::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(ActivityManager.TaskDescription(
                    getString(R.string.app_name),
                    BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher),
                    resources.getColor(R.color.colorPrimaryDark)
            ))
        }

        supportActionBar?.title = getString(R.string.variant_oge) + varNumber.toString()

        m.getPdfBytesLiveData().observe(this, Observer {
            if (it == null) return@Observer
            // Hack because of a bug in PDFview; It crashes when you load a second PDF
            // https://github.com/JoanZapata/android-pdfview/issues/75#issuecomment-73664568
            val group = pdfView.parent as ViewGroup
            group.removeView(pdfView)
            val mPdfView = PDFView(this, null)
            mPdfView.id = R.id.pdfView
            mPdfView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            group.addView(mPdfView)

            mPdfView.recycle()
            mPdfView.fromBytes(it)
                    .onLoad { varLoadingProgressBar.visibility = View.GONE }
                    .load()
        })

        // set recycler view
        val isLandscape = resources.configuration.orientation == ORIENTATION_LANDSCAPE
        answersView.layoutManager = GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)
        val answers = mutableListOf<String>()
        answersView.adapter = AnswersAdapter(answers)

        m.getAnswersLiveData().observe(this, Observer {
            if (it == null) return@Observer
            answers.clear()
            answers.addAll(it)
            answersView.adapter?.notifyDataSetChanged()
        })

        m.share.observe(this, Observer {
            if (it == null) return@Observer
            share(it)
        })

        m.stopLoadingAnimation.observe(this, Observer {
            if (it == null) return@Observer
            varLoadingProgressBar.visibility = View.GONE
        })

        videoButton.setCompoundDrawablesWithIntrinsicBounds(resources.getDrawable(R.drawable.ic_play_circle_green), null, null, null )
        videoButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "oge-video")
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "url")
            FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.video_answers_oge_playlist_url))))
        }

        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        m.answersPanelState.observe(this, Observer {
            if (it == null) return@Observer
            bottomSheetBehavior.state = it
        })

        answersPanel.setOnClickListener {
            m.answersPanelState.postValue(
                    if (m.answersPanelState.value == STATE_COLLAPSED) STATE_EXPANDED
                    else STATE_COLLAPSED
            )
        }

        m.isOffline.observe(this, Observer {
            if (it == null) return@Observer
            try {
                menu.findItem(R.id.offline).icon =
                        ResourcesCompat.getDrawable(resources, if (it) R.drawable.ic_delete_white
                        else R.drawable.ic_file_download_white_24dp, null)
            } catch (ignored: Exception) {
            }
        })

        m.downloadDone.observe(this, Observer {
            if (it == null) return@Observer
            if (it) toast(R.string.variant_saved)
            else toast(R.string.variant_not_saved)

            menu.findItem(R.id.offline).isEnabled = true
        })

        m.deletionDone.observe(this, Observer {
            if (it == null) return@Observer
            if (it) toast(R.string.deletion_successful)
            else toast(R.string.deletion_failed)

            menu.findItem(R.id.offline).isEnabled = true
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.variant, menu)
        this.menu = menu
        try {
            menu.findItem(R.id.offline).icon = if (m.isOffline.value!!)
                ResourcesCompat.getDrawable(resources, R.drawable.ic_delete_white, null)
            else ResourcesCompat.getDrawable(resources, R.drawable.ic_file_download_white_24dp, null)
        } catch (ignored: Exception) {
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.share -> {
            m.share()
            true
        }
        R.id.offline -> {
            m.offlineButton()
            item.isEnabled = false
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfView.recycle()
        val state = BottomSheetBehavior.from(bottomSheet).state
        if (state == STATE_COLLAPSED || state == STATE_EXPANDED)
            m.answersPanelState.postValue(state)
    }
}