package com.popov.egeanswers.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.support.annotation.MainThread
import com.popov.egeanswers.ActionLiveData
import com.popov.egeanswers.model.VariantUI

abstract class VariantsViewModel(isOfflineOnly: Boolean, app: Application) : AndroidViewModel(app) {
    abstract val varsLoadingErrorSnackbar: ActionLiveData<String>

    abstract fun search(varNumber: Int, @MainThread done: (isSuccess: Boolean, isNotFound: Boolean, varYear: Int) -> Unit)
    abstract fun getVariantsLiveData(): LiveData<List<VariantUI>>
}