package com.popov.egeanswers.viewmodel

import android.app.Application
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider

class VariantViewModelFactory(
        private val application: Application,
        private val varNumber: Int,
        private val varYear: Int = 0
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = try {
        modelClass.getConstructor(Application::class.java, Int::class.java, Int::class.java)
                .newInstance(application, varNumber, varYear)
    } catch (e: NoSuchMethodException) {
        modelClass.getConstructor(Application::class.java, Int::class.java)
                .newInstance(application, varNumber)
    }
}