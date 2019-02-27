package com.popov.egeanswers.util

import android.arch.lifecycle.LiveData
import io.realm.RealmChangeListener
import io.realm.RealmModel
import io.realm.kotlin.addChangeListener
import io.realm.kotlin.isValid
import io.realm.kotlin.removeChangeListener

class RealmModelLiveData<T : RealmModel>(private val realmModel: T) : LiveData<T>() {
    private val listener = RealmChangeListener<T> {
        setValue(if (it.isValid()) it else null)
    }

    override fun onActive() {
        realmModel.addChangeListener(listener)
    }

    override fun onInactive() {
        realmModel.removeChangeListener(listener)
    }

}