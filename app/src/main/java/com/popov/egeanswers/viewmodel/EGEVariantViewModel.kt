package com.popov.egeanswers.viewmodel

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.analytics.FirebaseAnalytics
import com.popov.egeanswers.ActionLiveData
import com.popov.egeanswers.larinApi.LarinApi
import com.popov.egeanswers.R
import com.popov.egeanswers.dao.LarinEGEVariantDao
import com.popov.egeanswers.larinApi.EgeApi
import com.popov.egeanswers.larinApi.LarinApi.Companion.baseUrl
import com.popov.egeanswers.model.LarinEGEVariant
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class EGEVariantViewModel(private val app: Application,
                          private val varNumber: Int,
                          private val varYear: Int
) : AndroidViewModel(app) {
    val loadWebView = ActionLiveData<Pair<Int, Int>>()
    val share = ActionLiveData<String>()
    val stopLoadingAnimation = ActionLiveData<Byte>()
    val downloadDone = ActionLiveData<Boolean>()
    val deletionDone = ActionLiveData<Boolean>()
    val isOffline = MutableLiveData<Boolean>()
    val answersPanelState = MutableLiveData<Int>()

    private val pdfBytes = MutableLiveData<ByteArray>()
    private val part1Answers = MutableLiveData<List<String>>()
    private val part2AnswersBytes = MutableLiveData<ByteArray>()

    private val dao = LarinEGEVariantDao()
    private val api = EgeApi()

    private var variant: LarinEGEVariant? = null

    fun setPart1Answers(answers: List<String>) = part1Answers.postValue(answers)
    fun getPdfBytesLiveData(): LiveData<ByteArray> = pdfBytes
    fun getPart1AnswersLiveData(): LiveData<List<String>> = part1Answers
    fun getPart2AnswersBytesLiveData(): LiveData<ByteArray> = part2AnswersBytes

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    init {
        viewModelScope.launch(Dispatchers.Main) {
            answersPanelState.postValue(BottomSheetBehavior.STATE_COLLAPSED)

            firebaseAnalytics = FirebaseAnalytics.getInstance(app)

            variant = suspendCoroutine { cont ->
                val data = dao.getVariant(varNumber)
                var observer: Observer<LarinEGEVariant>? = null
                observer = Observer {
                    data.removeObserver(observer!!)
                    cont.resume(it)
                }
                data.observeForever(observer)
            }

            isOffline.postValue(variant != null)

            part1Answers.observeForever {
                if (it == null) return@observeForever
                launch(Dispatchers.Main) { dao.setPart1Answers(varNumber, it) }
            }

            if (variant == null) {
                loadWebView.sendAction(varYear to varNumber)

                // pdf
                try {
                    pdfBytes.postValue(api.getPdf(varNumber, varYear))
                } catch (e: Exception) {
                    app.toast(R.string.pdf_loading_error)
                    stopLoadingAnimation.sendAction(0)
                }

                // part 2 answers
                try {
                    part2AnswersBytes.postValue(api.getPart2Answers(varNumber, varYear))
                } catch (e: Exception) {
                    app.toast(R.string.part2Answers_downloading_error)
                    Log.e("EGEVariantViewModel", app.getString(R.string.part2Answers_downloading_error), e)
                }
            } else {
                pdfBytes.postValue(variant!!.pdf.copyOf())
                part1Answers.postValue(variant!!.part1Answers.toList())
                part2AnswersBytes.postValue(variant!!.part2Answers.copyOf())

                try {
                    val part2Answers = api.getPart2Answers(varNumber, varYear)
                    dao.setPart2Answers(varNumber, part2Answers)
                    part2AnswersBytes.postValue(part2Answers)
                } catch (e: Exception) {
                    Log.e("EGEVariantViewModel", app.getString(R.string.part2Answers_downloading_error), e)
                }
            }
        }
    }

    fun share() {
        if (part1Answers.value == null || part1Answers.value!!.isEmpty()) {
            return
        }
        val part2Url = if (varNumber >= 285) "$baseUrl/ege/$varYear/trvar${varNumber}_0.png"
        else "$baseUrl/ege/$varYear/trvar$varNumber.png"

        var shareBody = "${app.getString(R.string.larin)} ${app.getString(R.string.variant_ege)} $varNumber\n"
        shareBody += "$baseUrl/ege/$varYear/trvar$varNumber.html\n"
        for (i in part1Answers.value!!.indices) shareBody += "${i + 1}. ${part1Answers.value!![i]}\n"
        shareBody += "${app.getString(R.string.part2_share)} $part2Url"
        share.sendAction(shareBody)

        val bundle = Bundle()
        bundle.putInt(FirebaseAnalytics.Param.ITEM_ID, varNumber)
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "ege")
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, bundle)
    }

    fun offlineButton() {
        viewModelScope.launch(Dispatchers.Main) {
            if (variant == null) {
                try {
                    if (pdfBytes.value == null || part2AnswersBytes.value == null || part1Answers.value!!.isEmpty()) {
                        downloadDone.sendAction(false)
                        return@launch
                    }
                    variant = dao.createVariant(
                            varNumber,
                            varYear,
                            pdfBytes.value!!,
                            part1Answers.value!!.toMutableList(),
                            Calendar.getInstance().apply { set(Calendar.YEAR, varYear) }.time,
                            part2AnswersBytes.value!!
                    )
                    downloadDone.sendAction(true)
                    isOffline.postValue(true)

                    val bundle = Bundle()
                    bundle.putInt(FirebaseAnalytics.Param.ITEM_ID, varNumber)
                    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "ege")
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
                    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "ege")
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