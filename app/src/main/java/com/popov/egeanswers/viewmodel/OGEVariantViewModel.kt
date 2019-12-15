package com.popov.egeanswers.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.analytics.FirebaseAnalytics
import com.popov.egeanswers.ActionLiveData
import com.popov.egeanswers.LarinApi
import com.popov.egeanswers.R
import com.popov.egeanswers.dao.LarinOGEVariantDao
import com.popov.egeanswers.model.LarinOGEVariant
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class OGEVariantViewModel(private val app: Application,
                          private val varNumber: Int
) : AndroidViewModel(app) {
    val share = ActionLiveData<String>()
    val stopLoadingAnimation = ActionLiveData<Byte>()
    val downloadDone = ActionLiveData<Boolean>()
    val deletionDone = ActionLiveData<Boolean>()
    val isOffline = MutableLiveData<Boolean>()
    val answersPanelState = MutableLiveData<Int>()

    private val pdfBytes = MutableLiveData<ByteArray>()
    private val answers = MutableLiveData<List<String>>()

    private val dao = LarinOGEVariantDao()
    private val api = LarinApi().OGE()

    private var variant: LarinOGEVariant? = null

    fun setAnswers(answers: List<String>) = this.answers.postValue(answers)
    fun getPdfBytesLiveData(): LiveData<ByteArray> = pdfBytes
    fun getAnswersLiveData(): LiveData<List<String>> = answers

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    init {
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            answersPanelState.postValue(BottomSheetBehavior.STATE_COLLAPSED)

            firebaseAnalytics = FirebaseAnalytics.getInstance(app)

            variant = suspendCoroutine { cont ->
                val data = dao.getVariant(varNumber)
                var observer: Observer<LarinOGEVariant>? = null
                observer = Observer {
                    data.removeObserver(observer!!)
                    cont.resume(it)
                }
                data.observeForever(observer)
            }

            isOffline.postValue(variant != null)

            if (variant == null) {
                // pdf
                try {
                    pdfBytes.postValue(api.getPdf(varNumber))
                } catch (e: Exception) {
                    app.toast(R.string.pdf_loading_error)
                    stopLoadingAnimation.sendAction(0)
                }

                // answers
                try {
                    answers.postValue(api.getAnswers(varNumber))
                } catch (e: Exception) {
                    app.toast(R.string.part2Answers_downloading_error)
                }
            } else {
                pdfBytes.postValue(variant!!.pdf.copyOf())
                answers.postValue(variant!!.answers.toList())

                try {
                    val answers = api.getAnswers(varNumber)
                    dao.setAnswers(varNumber, answers)
                    this@OGEVariantViewModel.answers.postValue(answers)
                } catch (ignored: Exception) {
                }
            }
        }
    }

    fun share() {
        if (answers.value == null || answers.value!!.isEmpty()) {
            return
        }
        var shareBody = "${app.getString(R.string.larin)} ${app.getString(R.string.variant_oge)} $varNumber\n"
        shareBody += "http://alexlarin.net/gia/trvar${varNumber}_oge.html\n"
        for (i in 0 until answers.value!!.size) shareBody += "${i + 1}. ${answers.value!![i]}\n"
        share.sendAction(shareBody)

        val bundle = Bundle()
        bundle.putInt(FirebaseAnalytics.Param.ITEM_ID, varNumber)
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "oge")
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, bundle)
    }

    fun offlineButton() {
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            if (variant == null) {
                try {
                    if (pdfBytes.value == null || answers.value!!.isEmpty()) {
                        downloadDone.sendAction(false)
                        return@launch
                    }
                    val year = api.getYear(varNumber, Calendar.getInstance().get(Calendar.YEAR))
                    variant = dao.createVariant(
                            number = varNumber,
                            year = year,
                            pdf = pdfBytes.value!!,
                            answers = answers.value!!.toMutableList(),
                            publicationDate = Calendar.getInstance().apply { set(Calendar.YEAR, year) }.time
                    )
                    downloadDone.sendAction(true)
                    isOffline.postValue(true)

                    val bundle = Bundle()
                    bundle.putInt(FirebaseAnalytics.Param.ITEM_ID, varNumber)
                    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "oge")
                    firebaseAnalytics.logEvent("download", bundle)
                } catch (e: Exception) {
                    downloadDone.sendAction(false)
                    isOffline.postValue(false)
                }
            } else {
                if (dao.deleteVariant(varNumber)) {
                    variant = null
                    deletionDone.sendAction(true)
                    isOffline.postValue(false)

                    val bundle = Bundle()
                    bundle.putInt(FirebaseAnalytics.Param.ITEM_ID, varNumber)
                    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "oge")
                    firebaseAnalytics.logEvent("delete", bundle)
                } else {
                    deletionDone.sendAction(false)
                    isOffline.postValue(true)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        dao.close()
    }
}