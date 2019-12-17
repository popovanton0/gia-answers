package com.popov.egeanswers.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import com.google.firebase.analytics.FirebaseAnalytics
import com.popov.egeanswers.ActionLiveData
import com.popov.egeanswers.dao.LarinOGEVariantDao
import com.popov.egeanswers.larinApi.LarinApi
import com.popov.egeanswers.larinApi.OgeApi
import com.popov.egeanswers.model.VariantUI
import kotlinx.coroutines.Job
import java.util.*

abstract class VariantsViewModel(isOfflineOnly: Boolean, app: Application) : AndroidViewModel(app) {
    protected val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    protected val _variants = MutableLiveData<List<VariantUI>>()
    val variants: LiveData<List<VariantUI>> = _variants
    val varsLoadingErrorSnackbar = ActionLiveData<String>()
    protected var varsLoadingJob: Job? = null
    protected var varsSearchingJob: Job? = null

    protected val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(app)

    protected abstract val api: LarinApi

    protected abstract suspend fun loadVariants(nowYear: Int, startYear: Int)
    abstract fun search(varNumber: Int, @MainThread done: (isSuccess: Boolean, isNotFound: Boolean, varYear: Int) -> Unit)
}