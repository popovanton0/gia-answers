package com.popov.egeanswers.util

import androidx.lifecycle.LiveData
import io.realm.RealmModel
import io.realm.RealmResults

fun <T : RealmModel> RealmResults<T>.asLiveData(): LiveData<List<T>> = RealmResultsLiveData(this)

fun <T : RealmModel> T.asLiveData(): LiveData<T> = RealmModelLiveData(this)