package com.popov.egeanswers.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.popov.egeanswers.ActionLiveData
import com.popov.egeanswers.BuildConfig
import com.popov.egeanswers.LarinApi
import com.popov.egeanswers.R
import com.popov.egeanswers.ui.EGEVariantActivity
import com.popov.egeanswers.ui.OGEVariantActivity
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.connectivityManager
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.intentFor
import java.util.*

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val fragment = MutableLiveData<Int>()
    val noInternetSnackbar = ActionLiveData<Byte>()
    val betaDialog = ActionLiveData<Byte>()
    val startIntro = ActionLiveData<Byte>()
    private val api = LarinApi()
    private var sp = app.defaultSharedPreferences


    init {
        sp.edit().remove("isFirstStart").apply()
        if (!sp.contains("is_first_start")) startIntro.sendAction(0)
        setFragment(1)
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            try {
                setupShortcuts()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (!sp.contains("is_first_start")) {
            betaDialog.sendAction(0)
        }
        sp.edit().putBoolean("is_first_start", false).apply()
        if (BuildConfig.DEBUG) getAndPrintFCMToken()

        val gradeIncrementation = incrementGrade()
        if (gradeIncrementation > 0) {
            val userClass = sp.getString("user_class", "")!!.toInt()
            sp.edit()
                    .putString("user_class", (userClass + gradeIncrementation).toString())
                    .putString("user_class_set_date", Calendar.getInstance().timeInMillis.toString())
                    .apply()
        }
    }

    private fun incrementGrade(): Int {
        try {
            if (sp.getString("user_class", "")!!.isEmpty()) return 0
            if (sp.getString("user_class_set_date", "")!!.isEmpty()) return 0
            try {
                sp.getString("user_class", "")!!.toInt()
                sp.getString("user_class_set_date", "")!!.toLong()
            } catch (e: Exception) {
                return 0
            }

            if (sp.getString("user_class", "")!!.toInt() > 10) return 0

            val calendar = Calendar.getInstance()
            val userClassSetDate = sp.getString("user_class_set_date", "")!!.toLong()
            calendar.timeInMillis = userClassSetDate
            val year = calendar[Calendar.YEAR]
            val currentYear = Calendar.getInstance()[Calendar.YEAR]
            val isBeforeSwitchingMonth = calendar[Calendar.MONTH] < Calendar.JUNE
            val isBeforeCurrentSwitchingMonth = Calendar.getInstance()[Calendar.MONTH] < Calendar.JUNE
            val pastYears = currentYear - year
            return if (pastYears == 0)
                if (isBeforeSwitchingMonth) if (!isBeforeCurrentSwitchingMonth) 1 else 0
                else 0
            else if (pastYears > 0)
                if (isBeforeSwitchingMonth)
                    if (isBeforeCurrentSwitchingMonth) pastYears
                    else pastYears + 1
                else
                    if (isBeforeCurrentSwitchingMonth) pastYears - 1
                    else pastYears
            else throw IllegalStateException("Time traveller detected!")
        } catch (e: Exception) {
            return 0
        }
    }

    private fun getAndPrintFCMToken() {
        FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w("LARIN", "getInstanceId failed", task.exception)
                        return@OnCompleteListener
                    }

                    // Get new Instance ID token
                    val token = task.result?.token
                    val msg = "Token: $token"

                    // Log and toast
                    Log.d("LARIN", msg)
                    Toast.makeText(app.baseContext, msg, Toast.LENGTH_SHORT).show()
                })
    }

    fun getFragmentLiveData(): LiveData<Int> = fragment

    fun setFragment(pos: Int) {
        if (fragment.value == pos) return
        if (isOnline()) fragment.postValue(pos)
        else {
            when (pos) {
                1 -> fragment.postValue(3)
                2 -> fragment.postValue(4)
                else -> fragment.postValue(pos)
            }
            noInternetSnackbar.sendAction(0)
        }
    }

    private suspend fun setupShortcuts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && isOnline()) {
            val shortcuts = mutableListOf<ShortcutInfo>()

            var yearOfVar: Int

            val varNumberEGE: Int = try {
                yearOfVar = currentYear + 1
                val latestVarNumber = api.EGE().getLatestVarNumber(currentYear + 1)
                if (latestVarNumber == 0) throw Exception()
                latestVarNumber
            } catch (e: Exception) {
                yearOfVar = currentYear
                api.EGE().getLatestVarNumber(currentYear)
            }

            if (varNumberEGE == 0) return
            shortcuts += ShortcutInfo.Builder(app, "id$varNumberEGE")
                    .setShortLabel(varNumberEGE.toString() + " " + app.getString(R.string.ege_variant_before))
                    .setLongLabel(varNumberEGE.toString() + " " + app.getString(R.string.ege_variant_before))
                    .setIcon(Icon.createWithResource(app,
                            R.drawable.ic_shortcut_format_align_left))
                    .setIntent(app.intentFor<EGEVariantActivity>(
                            "varNumber" to varNumberEGE,
                            "varYear" to yearOfVar)
                            .setAction(Intent.ACTION_VIEW)
                    )
                    .build()

            val varNumberOGE: Int = try {
                yearOfVar = currentYear + 1
                val latestVarNumber = api.OGE().getLatestVarNumber(currentYear + 1)
                if (latestVarNumber == 0) throw Exception()
                latestVarNumber
            } catch (e: Exception) {
                yearOfVar = currentYear
                api.OGE().getLatestVarNumber(currentYear)
            }

            if (varNumberOGE == 0) return
            shortcuts += ShortcutInfo.Builder(app, "id$varNumberOGE")
                    .setShortLabel(varNumberOGE.toString() + " " + app.getString(R.string.oge_variant_before))
                    .setLongLabel(varNumberOGE.toString() + " " + app.getString(R.string.oge_variant_before))
                    .setIcon(Icon.createWithResource(app,
                            R.drawable.ic_shortcut_format_align_left))
                    .setIntent(app.intentFor<OGEVariantActivity>(
                            "varNumber" to varNumberOGE,
                            "varYear" to yearOfVar)
                            .setAction(Intent.ACTION_VIEW)
                    )
                    .build()

            app.getSystemService(ShortcutManager::class.java).dynamicShortcuts = shortcuts
        }
    }

    private fun isOnline(): Boolean = app
            .connectivityManager
            .activeNetworkInfo
            ?.isConnectedOrConnecting
            ?: false
}