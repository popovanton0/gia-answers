package com.popov.egeanswers.viewmodel

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.annotation.MainThread
import com.google.firebase.analytics.FirebaseAnalytics
import com.popov.egeanswers.ActionLiveData
import com.popov.egeanswers.LarinApi
import com.popov.egeanswers.R
import com.popov.egeanswers.dao.LarinOGEVariantDao
import com.popov.egeanswers.model.LarinOGEVariant
import com.popov.egeanswers.model.VariantType
import com.popov.egeanswers.model.VariantUI
import kotlinx.coroutines.*
import org.jetbrains.anko.collections.forEachReversedByIndex
import java.util.*

class OGEVariantsViewModel(private val isOfflineOnly: Boolean, private val app: Application) : VariantsViewModel(isOfflineOnly, app) {

    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val variants = MutableLiveData<List<VariantUI>>()
    override val varsLoadingErrorSnackbar = ActionLiveData<String>()
    private var varsLoadingJob: Job? = null
    private var varsSearchingJob: Job? = null

    private val dao = LarinOGEVariantDao()
    private val api = LarinApi().OGE()

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private val offlineVars = dao.getAllLiveData()

    private val offlineVarsOnlineObserver = Observer<List<LarinOGEVariant>> { dbVars ->
        if (dbVars == null) return@Observer
        variants.postValue(
                variants.value!!.map { uiVar ->
                    val isOffline = dbVars.firstOrNull { uiVar.number == it.number } != null
                    uiVar.copy(isOffline = isOffline)
                }
        )
    }

    private val offlineVarsOfflineObserver = Observer<List<LarinOGEVariant>> { dbVars ->
        if (dbVars == null) return@Observer
        variants.postValue(dbVars
                .asSequence()
                .sortedByDescending { it.number }
                .map { larinOGEVariant ->
                    VariantUI(
                            number = larinOGEVariant.number,
                            year = larinOGEVariant.year,
                            publicationDate = larinOGEVariant.publicationDate,
                            isOffline = true,
                            type = larinOGEVariant.type
                    )
                }.toList())
    }

    init {
        firebaseAnalytics = FirebaseAnalytics.getInstance(app)
        varsLoadingJob = GlobalScope.async(Dispatchers.Main, CoroutineStart.DEFAULT) {
            try {
                // "currentYear + 1" because of september - december posts of (year + 1) variants
                loadOGEVariants(currentYear + 1, LarinApi.OGE_START_YEAR)
            } catch (ignored: Exception) {
                variants.value = emptyList()
                try {
                    loadOGEVariants(currentYear, LarinApi.OGE_START_YEAR)
                } catch (e: Exception) {
                    varsLoadingErrorSnackbar.sendAction(
                            if (!e.message.isNullOrBlank()) app.getString(R.string.variants_loading_error) + ": " + e.message
                            else app.getString(R.string.variants_loading_error)
                    )
                }
            }
        }
    }

    private suspend fun loadOGEVariants(nowYear: Int, startYear: Int) {
        offlineVars.removeObserver(offlineVarsOnlineObserver)
        offlineVars.removeObserver(offlineVarsOfflineObserver)
        if (!isOfflineOnly) {
            val offline = dao.getAll().map { it.number }

            val vars = mutableListOf<VariantUI>()
            // why it is here? if (variants.value != null) vars.addAll(variants.value!!)

            for (year in nowYear downTo startYear) {
                val varNumbers = api.getVarNumbers(year)
                varNumbers.forEachReversedByIndex { number ->
                    vars.add(VariantUI(
                            number = number,
                            year = year,
                            publicationDate = Calendar.getInstance().apply { set(Calendar.YEAR, year) }.time,
                            isOffline = offline.contains(number),
                            type = VariantType.OGE
                    ))
                }
                variants.postValue(vars)
            }

            if (vars.isEmpty()) throw Exception()
            offlineVars.observeForever(offlineVarsOnlineObserver)
        } else offlineVars.observeForever(offlineVarsOfflineObserver)
    }

    override fun search(varNumber: Int, @MainThread done: (isSuccess: Boolean, isNotFound: Boolean, varYear: Int) -> Unit) {
        varsSearchingJob = GlobalScope.async(Dispatchers.Main, CoroutineStart.DEFAULT) {
            try {
                val bundle = Bundle()
                bundle.putInt(FirebaseAnalytics.Param.ITEM_ID, varNumber)
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "oge")
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle)

                val isActive = varsLoadingJob?.isActive
                if (isActive != null && isActive) {
                    done(false, false, -1)
                    return@async
                }
                val foundVar = variants.value?.find { it.number == varNumber }
                if (foundVar == null) {
                    done(false, true, -1)
                    return@async
                }
                done(true, false, foundVar.year)
            } catch (e: Exception) {
                done(false, false, -1)
            }
        }
    }

    override fun getVariantsLiveData(): LiveData<List<VariantUI>> = variants

    override fun onCleared() {
        super.onCleared()
        varsLoadingJob?.cancel()
        varsSearchingJob?.cancel()
        dao.close()
    }
}