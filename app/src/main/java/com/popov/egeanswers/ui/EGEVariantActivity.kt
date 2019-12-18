package com.popov.egeanswers.ui

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.webkit.JavascriptInterface
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.barteksc.pdfviewer.PDFView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.firebase.analytics.FirebaseAnalytics
import com.popov.egeanswers.AnswersAdapter
import com.popov.egeanswers.Blur
import com.popov.egeanswers.R
import com.popov.egeanswers.viewmodel.EGEVariantViewModel
import com.popov.egeanswers.viewmodel.VariantViewModelFactory
import kotlinx.android.synthetic.main.activity_ege_variant.*
import org.jetbrains.anko.configuration
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

        val isDarkMode = when (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            else -> error("Unreachable")
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

            //mPdfView.nightMode(isDarkMode)
            mPdfView.recycle()
            mPdfView.fromBytes(it)
                    .onRender {
                        varLoadingProgressBar.visibility = View.GONE
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) reportFullyDrawn()
                    }
                    /*.onRender { _, _, _ ->
                        mPdfView.apply { if (width / optimalPageWidth > zoom) fitToWidth() }
                    }
                    .onPageScroll { _, _ ->
                        mPdfView.apply { if (width / optimalPageWidth > zoom) fitToWidth() }
                    }*/
                    .nightMode(isDarkMode)
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
            part2answersImageView.setOnClickListener(null)

            var onClick: View.OnClickListener? = null
            onClick = View.OnClickListener {
                part2answersImageView.setOnClickListener(null)

                val isBlurred = showPart2answersTextView.visibility == View.VISIBLE
                val unBlurredImageBitmap = getAnswersBitmap(m.getPart2AnswersBytesLiveData().value, isDarkMode)
                        ?: return@OnClickListener

                if (isBlurred) {
                    showPart2answersTextView.visibility = View.GONE

                    setImageWithAnimation(part2answersImageView, unBlurredImageBitmap) {
                        part2answersImageView.setOnClickListener(onClick)
                    }
                } else {
                    val blurredImageBitmap = Blur.blur(this, unBlurredImageBitmap)
                    setImageWithAnimation(part2answersImageView, blurredImageBitmap) {
                        showPart2answersTextView.visibility = View.VISIBLE
                        part2answersImageView.setOnClickListener(onClick)
                    }
                }
            }

            val unBlurredImageBitmap = getAnswersBitmap(m.getPart2AnswersBytesLiveData().value, isDarkMode)
                    ?: return@Observer
            val blurredImageBitmap = Blur.blur(this, unBlurredImageBitmap)
            setImageWithAnimation(part2answersImageView, blurredImageBitmap) {
                part2answersImageView.setOnClickListener(onClick)
            }

            showPart2answersTextView.visibility = View.VISIBLE
        })

        m.share.observe(this, Observer {
            if (it == null) return@Observer
            share(it)
        })

        m.stopLoadingAnimation.observe(this, Observer {
            if (it == null) return@Observer
            varLoadingProgressBar.visibility = View.GONE
        })

        videoButton.setCompoundDrawablesWithIntrinsicBounds(resources.getDrawable(R.drawable.ic_play_circle_green), null, null, null)
        videoButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "ege-video")
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "url")
            FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.video_answers_ege_playlist_url))))
        }

        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        if (m.answersPanelState.value != null) {
            bottomSheetBehavior.state = m.answersPanelState.value!!
            when (m.answersPanelState.value!!) {
                STATE_COLLAPSED -> answersPanelArrowImageView.rotation = 0f
                STATE_EXPANDED -> answersPanelArrowImageView.rotation = 180f
            }
        }

        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(p0: View, percent: Float) {
                answersPanelArrowImageView.rotation = 360 - percent * 180 // degrees
            }

            @SuppressLint("SwitchIntDef")
            override fun onStateChanged(p0: View, state: Int) {
                m.answersPanelState.postValue(state)
                when (state) {
                    STATE_COLLAPSED -> answersPanelArrowImageView.rotation = 0f
                    STATE_EXPANDED -> answersPanelArrowImageView.rotation = 180f
                }
            }
        })

        answersPanel.setOnClickListener {
            bottomSheetBehavior.state =
                    if (m.answersPanelState.value == STATE_COLLAPSED) STATE_EXPANDED
                    else STATE_COLLAPSED
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

    override fun onBackPressed() {
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        if (bottomSheetBehavior.state == STATE_EXPANDED)
            bottomSheetBehavior.state = STATE_COLLAPSED
        else super.onBackPressed()
    }

    internal inner class MyJavaScriptInterface {
        @JavascriptInterface
        fun sendAnswers(answers: String) = m.setPart1Answers(answers.split(';'))
    }

    private fun setImageWithAnimation(imageView: ImageView, bitmap: Bitmap, onSet: () -> Unit) {
        val animOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        val animIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        animIn.duration = 200
        animOut.duration = 200
        animOut.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                imageView.setImageBitmap(bitmap)
                animIn.setAnimationListener(object : AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}
                    override fun onAnimationRepeat(animation: Animation) {}
                    override fun onAnimationEnd(animation: Animation) {
                        onSet()
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

    private fun Bitmap.invert(): Bitmap {
        val bitmap: Bitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val matrixGrayscale = ColorMatrix()
        matrixGrayscale.setSaturation(0f)
        val matrixInvert = ColorMatrix()
        matrixInvert.set(
                floatArrayOf(
                        -0.82f, 0.0f, 0.0f, 0.0f, 255.0f,
                        0.0f, -0.82f, 0.0f, 0.0f, 255.0f,
                        0.0f, 0.0f, -0.82f, 0.0f, 255.0f,
                        0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                )
        )
        matrixInvert.preConcat(matrixGrayscale)
        val filter = ColorMatrixColorFilter(matrixInvert)
        paint.colorFilter = filter
        canvas.drawBitmap(this, 0f, 0f, paint)
        return bitmap
    }

    private fun getAnswersBitmap(byteArray: ByteArray?, isDarkMode: Boolean): Bitmap? {
        byteArray ?: return null
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size) ?: return null
        return if (isDarkMode) bitmap.invert() else bitmap
    }
}