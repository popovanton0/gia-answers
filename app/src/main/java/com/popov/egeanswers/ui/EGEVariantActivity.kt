package com.popov.egeanswers.ui

import android.app.ActivityManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetBehavior.STATE_COLLAPSED
import android.support.design.widget.BottomSheetBehavior.STATE_EXPANDED
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.webkit.JavascriptInterface
import android.widget.ImageView
import com.github.barteksc.pdfviewer.PDFView
import com.google.firebase.analytics.FirebaseAnalytics
import com.popov.egeanswers.AnswersAdapter
import com.popov.egeanswers.BlurBuilder
import com.popov.egeanswers.R
import com.popov.egeanswers.viewmodel.EGEVariantViewModel
import com.popov.egeanswers.viewmodel.VariantViewModelFactory
import kotlinx.android.synthetic.main.activity_ege_variant.*
import org.jetbrains.anko.share
import org.jetbrains.anko.toast

class EGEVariantActivity : AppCompatActivity() {

    private lateinit var m: EGEVariantViewModel
    private lateinit var menu: Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ege_variant)
        setSupportActionBar(myToolbar)

        var varNumber = intent.getIntExtra("varNumber", 1)
        var varYear = intent.getIntExtra("varYear", 1)

        intent.data?.path?.apply {
            varNumber = replaceBeforeLast('/', "").removePrefix("/trvar").removeSuffix(".html").toInt()
            varYear = replaceAfterLast('/', "").removeSuffix("/").replaceBeforeLast('/', "").removePrefix("/").toInt()
        }

        m = ViewModelProviders
                .of(this, VariantViewModelFactory(this.application, varNumber, varYear))
                .get(EGEVariantViewModel::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(ActivityManager.TaskDescription(
                    getString(R.string.app_name),
                    BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher),
                    resources.getColor(R.color.colorPrimaryDark)
            ))
        }

        supportActionBar?.title = getString(R.string.variant_ege) + varNumber.toString()

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
        answersView.layoutManager = /*if (isLandscape) GridLayoutManager(this, 2) else */LinearLayoutManager(this)
        val part1Answers = mutableListOf<String>()
        answersView.adapter = AnswersAdapter(part1Answers)

        m.getPart1AnswersLiveData().observe(this, Observer {
            if (it == null) return@Observer
            part1Answers.clear()
            part1Answers.addAll(it)
            answersView.adapter?.notifyDataSetChanged()
        })

        m.loadWebView.observe(this, Observer {
            if (it == null) return@Observer
            webView.settings.javaScriptEnabled = true
            webView.setNetworkAvailable(true)

            webView.addJavascriptInterface(MyJavaScriptInterface(), "INTERFACE")
            webView.loadData(getHTML(it.first, it.second), "text/html", "UTF-8")
        })

        m.getPart2AnswersBytesLiveData().observe(this, Observer {
            if (it == null) return@Observer
            part2answersImageView.setOnClickListener { }

            var onClick: View.OnClickListener? = null
            onClick = View.OnClickListener {
                part2answersImageView.setOnClickListener { }

                val isBlurring = showPart2answersTextView.visibility != View.VISIBLE
                val unBlurredImage = m.getPart2AnswersBytesLiveData().value
                        ?: return@OnClickListener // is not possible
                val unBlurredImageBitmap = BitmapFactory.decodeByteArray(unBlurredImage, 0, unBlurredImage.size)

                if (isBlurring) {
                    val blurredImageBitmap = BlurBuilder.blur(this, unBlurredImageBitmap)
                    part2answersImageView.setOnClickListener { }
                    setImageWithAnimation(part2answersImageView, blurredImageBitmap) {
                        showPart2answersTextView.visibility = View.VISIBLE
                        part2answersImageView.setOnClickListener(onClick)
                    }
                } else {
                    showPart2answersTextView.visibility = View.GONE

                    part2answersImageView.setOnClickListener { }
                    setImageWithAnimation(part2answersImageView, unBlurredImageBitmap) {
                        part2answersImageView.setOnClickListener(onClick)
                    }
                }
            }

            part2answersImageView.setOnClickListener { }

            val unBlurredImageBitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            val blurredImageBitmap = BlurBuilder.blur(this, unBlurredImageBitmap)
            setImageWithAnimation(part2answersImageView, blurredImageBitmap) {
                part2answersImageView.setOnClickListener(onClick)
            }

            showPart2answersTextView.visibility = View.VISIBLE
        })

        /*m.setActivityResult.observe(this, Observer {
            if (it == null) return@Observer
            setResult(it.first, it.second)
        })*/

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
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "ege-video")
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "url")
            FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.video_answers_ege_playlist_url))))
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

    internal inner class MyJavaScriptInterface {
        @JavascriptInterface
        fun sendAnswers(answers: String) = m.setPart1Answers(answers.split(';'))
    }

    private fun setImageWithAnimation(imageView: ImageView, bitmap: Bitmap, onSetted: () -> Unit) {
        val animOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        val animIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        //animIn.duration = 0
        animOut.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                imageView.setImageBitmap(bitmap)
                animIn.setAnimationListener(object : AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}
                    override fun onAnimationRepeat(animation: Animation) {}
                    override fun onAnimationEnd(animation: Animation) {
                        onSetted()
                    }
                })
                imageView.startAnimation(animIn)
            }
        })
        imageView.startAnimation(animOut)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.variant, menu)
        this.menu = menu
        try {
            menu.findItem(R.id.offline).icon = if (m.isOffline.value!!)
                ResourcesCompat.getDrawable(resources, R.drawable.ic_delete_white, null)
            else ResourcesCompat.getDrawable(resources, R.drawable.ic_file_download_white_24dp, null)
        } catch (e: Exception) {
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

    private fun getHTML(year: Int, varNumber: Int): String {
        try {
            val res = resources
            val inputStream = res.openRawResource(R.raw.html)
            val b = ByteArray(inputStream.available())
            inputStream.read(b)
            return String(b).replace("YEAR", year.toString()).replace("VAR_NUMBER", varNumber.toString())
        } catch (e: Exception) {
            throw e
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfView.recycle()
        val state = BottomSheetBehavior.from(bottomSheet).state
        if (state == STATE_COLLAPSED || state == STATE_EXPANDED)
            m.answersPanelState.postValue(state)
    }
}