package com.popov.egeanswers.ui

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.popov.egeanswers.R
import com.popov.egeanswers.model.UserType
import kotlinx.android.synthetic.main.activity_settings.*
import org.jetbrains.anko.defaultSharedPreferences
import java.util.*



class SettingsActivity : AppCompatActivity() {

    private lateinit var sp: SharedPreferences
    private var userClass: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)

        supportActionBar?.title = getString(R.string.title_activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        sp = defaultSharedPreferences
        userClass = sp.getString("user_class", "")!!
        supportFragmentManager.beginTransaction()
                .replace(R.id.placeholder, SettingsFragment())
                .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()

        val firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val userType = sp.getString("user_role", "")
        val userClass =
                if (userType == UserType.STUDENT.name) sp.getString("user_class", "")!!
                else ""
        val userClassSetDate = if (userType == UserType.STUDENT.name) {
            if (this.userClass == userClass) sp.getString("user_class_set_date", "")!!
            else Calendar.getInstance().timeInMillis.toString()
        } else ""

        sp.edit().apply {
            if (userClassSetDate.isNotEmpty()) putString("user_class_set_date", userClassSetDate)
            if (userClass.isEmpty()) remove("user_class")
            if (userClassSetDate.isEmpty()) remove("user_class_set_date")
        }.apply()

        firebaseAnalytics.apply {
            setUserProperty("user_role", userType)
            setUserProperty("user_class", if (userClass.isNotEmpty()) userClass else null)
            setUserProperty("user_class_set_date", if (userClassSetDate.isNotEmpty())
                userClassSetDate else null)
        }
    }
}
