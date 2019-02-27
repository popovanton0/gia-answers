package com.popov.egeanswers.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import com.google.firebase.analytics.FirebaseAnalytics
import com.popov.egeanswers.ActionLiveData
import com.popov.egeanswers.model.UserType
import org.jetbrains.anko.defaultSharedPreferences
import java.util.*

class IntroUserParamsViewModel(private val app: Application) : AndroidViewModel(app) {

    val userType = MutableLiveData<UserType>()
    val userClass = MutableLiveData<Int>()
    val doneButton = ActionLiveData<Byte>()

    private val sp = app.defaultSharedPreferences
    private var firebaseAnalytics = FirebaseAnalytics.getInstance(app)

    init {
        userType.postValue(UserType.STUDENT)
        userClass.postValue(9)

        doneButton.observeForever {
            if (it == null) return@observeForever
            sp.edit().apply {
                putString("user_role", userType.value!!.name)

                if (userType.value == UserType.STUDENT) {
                    putString("user_class", userClass.value!!.toString())
                    putString("user_class_set_date", Calendar.getInstance().timeInMillis.toString())
                } else {
                    remove("user_class")
                    remove("user_class_set_date")
                }
                apply()
            }

            firebaseAnalytics.setUserProperty("user_role", userType.value!!.name)
            if (userType.value == UserType.STUDENT) {
                firebaseAnalytics.setUserProperty("user_class", userClass.value!!.toString())
                firebaseAnalytics.setUserProperty("user_class_set_date", Calendar.getInstance().timeInMillis.toString())
            } else {
                firebaseAnalytics.setUserProperty("user_class", null)
                firebaseAnalytics.setUserProperty("user_class_set_date", null)
            }
        }
    }
}
