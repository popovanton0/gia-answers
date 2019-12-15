package com.popov.egeanswers.ui

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.popov.egeanswers.R
import com.popov.egeanswers.model.UserType
import org.jetbrains.anko.defaultSharedPreferences


class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(p0: Bundle?, p1: String?) {
        addPreferencesFromResource(R.xml.preferences)
        onSharedPreferenceChanged(this.activity!!.defaultSharedPreferences, "user_role")
    }

    override fun onResume() {
        super.onResume()
        //re-register the preferenceChange listener
        preferenceScreen.sharedPreferences
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {
        val preference = findPreference<Preference>(key)
        if (preference is ListPreference) {
            if (preference.key == "user_role") {
                findPreference<Preference>("user_class")?.isEnabled = preference.value != UserType.TEACHER.name
                /*if (preference.value == UserType.STUDENT.name)
                    if (findPreference("user_class") == null)
                        sp.edit()
                                .putString("user_class", resources.getStringArray(R.array.list_user_class_values)[0])
                                .apply()*/
            }
        }
    }

    override fun onPause() {
        super.onPause()
        //unregister the preference change listener
        preferenceScreen.sharedPreferences
                .unregisterOnSharedPreferenceChangeListener(this)
    }
}
