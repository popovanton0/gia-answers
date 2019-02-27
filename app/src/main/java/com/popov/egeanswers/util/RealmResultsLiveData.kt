package com.popov.egeanswers.util

import android.arch.lifecycle.LiveData
import io.realm.RealmChangeListener
import io.realm.RealmModel
import io.realm.RealmResults

class RealmResultsLiveData<T : RealmModel>(private val results: RealmResults<T>) : LiveData<List<T>>() {
    private val listener = RealmChangeListener<RealmResults<T>> {
        setValue(if (it.isValid) it else null)
    }

    override fun onActive() {
        results.addChangeListener(listener)
    }

    override fun onInactive() {
        results.removeChangeListener(listener)
    }

}
