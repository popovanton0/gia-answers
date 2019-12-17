package com.popov.egeanswers

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.github.barteksc.pdfviewer.PDFView
import com.popov.egeanswers.dbMigrations.Migration11
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import io.realm.Realm
import io.realm.RealmConfiguration


class MyApp : MultiDexApplication() {

    private lateinit var refWatcher: RefWatcher

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        refWatcher = LeakCanary.install(this)
        Realm.init(this)
        val realmConfig = RealmConfiguration.Builder()
                .schemaVersion(1)
                .migration(Migration11())
                .build()
        Realm.setDefaultConfiguration(realmConfig)
    }

    companion object {
        fun getRefWatcher(context: Context): RefWatcher {
            val application = context.applicationContext as MyApp
            return application.refWatcher
        }
    }
}