package com.popov.egeanswers.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

fun <T : Any?> LiveData<T>.observeNotNull(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    this.observe(lifecycleOwner, Observer { if (it != null) observer.onChanged(it) })
}