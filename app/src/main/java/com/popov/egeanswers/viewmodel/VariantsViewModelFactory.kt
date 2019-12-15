package com.popov.egeanswers.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class VariantsViewModelFactory (
        private val isOffline: Boolean,
        private val application: Application
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val constructor = modelClass.getConstructor(Boolean::class.java, Application::class.java)
        return constructor.newInstance(isOffline, application)
    }
}