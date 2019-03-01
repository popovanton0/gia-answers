package com.popov.egeanswers.ui

import android.app.ActivityManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import co.zsmb.materialdrawerkt.builders.accountHeader
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import co.zsmb.materialdrawerkt.draweritems.badgeable.secondaryItem
import co.zsmb.materialdrawerkt.draweritems.divider
import com.google.firebase.analytics.FirebaseAnalytics
import com.mikepenz.materialdrawer.Drawer
import com.popov.egeanswers.BuildConfig
import com.popov.egeanswers.R
import com.popov.egeanswers.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.longToast
import org.jetbrains.anko.startActivity
import java.util.*

class MainActivity : AppCompatActivity(), AnkoLogger {

    private lateinit var m: MainViewModel

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var sp: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(myToolbar)

        sp = defaultSharedPreferences

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        m = ViewModelProviders.of(this).get(MainViewModel::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(ActivityManager.TaskDescription(
                    getString(R.string.app_name),
                    BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher),
                    Color.parseColor("#388E3C")
            ))
        }

        m.startIntro.observe(this, Observer {
            startActivity(Intent(this, IntroActivity::class.java))
        })

        val userClass = try {
            defaultSharedPreferences.getString("user_class", "")!!.toInt()
        } catch (e: Exception) {
            10
        }

        // material drawer
        var drawer: Drawer? = null
        drawer = drawer {
            closeOnClick = true
            toolbar = myToolbar

            accountHeader {
                background = if (resources.configuration.locale == Locale("ru", "RU"))
                    R.drawable.header_goal_ru else R.drawable.header_goal_en
            }

            actionBarDrawerToggleAnimated = true

            onItemClick { pos ->
                drawer?.closeDrawer()
                when (pos) {
                    6 -> startActivity<SettingsActivity>()
                    7 -> openYandexDialogsSkill()
                    8 -> startActivity<AboutActivity>()
                    9 -> {
                        longToast(defaultSharedPreferences.all.map { "${it.key} : ${it.value}" }.joinToString(separator = "\n"))
                    }
                    else -> {
                        m.setFragment(pos)
                        return@onItemClick true
                    }
                }
                false
            }
            fun ege() = primaryItem(R.string.drawer_item_ege_variants) { icon = R.drawable.ic_book_open_grey_600; iconTintingEnabled = true }
            fun oge() = primaryItem(R.string.drawer_item_oge_variants) { icon = R.drawable.ic_book_open_outline_grey_600; iconTintingEnabled = true }
            fun offlineEge() = primaryItem(R.string.drawer_item_downloaded_ege_variants) { icon = R.drawable.ic_offline_pin_grey_600; iconTintingEnabled = true }
            fun offlineOge() = primaryItem(R.string.drawer_item_downloaded_oge_variants) { icon = R.drawable.ic_offline_pin_grey_600; iconTintingEnabled = true }

            when (userClass) {
                8, 9 -> {
                    oge()
                    ege()
                    offlineOge()
                    offlineEge()
                }
                else -> {
                    ege()
                    oge()
                    offlineEge()
                    offlineOge()
                }
            }

            divider {}
            secondaryItem(R.string.title_activity_settings) { icon = R.drawable.ic_settings_grey_600; selectable = false }
            secondaryItem(R.string.drawer_item_alice) { icon = R.drawable.ic_keyboard_voice_grey_600; selectable = false }
            secondaryItem(R.string.drawer_item_about) { icon = R.drawable.ic_info_outline_grey_24dp; selectable = false }
            if (BuildConfig.DEBUG) secondaryItem("Get SP") { icon = R.drawable.ic_bug_report_grey_600; selectable = false }
        }

        m.getFragmentLiveData().observe(this, Observer {

            val fragment = when (userClass) {
                8, 9 -> when (it) {
                    1 -> OGEVariantsFragment()
                    2 -> EGEVariantsFragment()
                    3 -> OfflineOGEVariantsFragment()
                    4 -> OfflineEGEVariantsFragment()
                    else -> throw IllegalArgumentException("This fragment number does not exist: $it")
                }
                else -> when (it) {
                    1 -> EGEVariantsFragment()
                    2 -> OGEVariantsFragment()
                    3 -> OfflineEGEVariantsFragment()
                    4 -> OfflineOGEVariantsFragment()
                    else -> throw IllegalArgumentException("This fragment number does not exist: $it")
                }
            }

            myToolbar.title = when (userClass) {
                8, 9 -> when (it) {
                    1, 3 -> getString(R.string.title_oge)
                    else -> getString(R.string.title_ege)
                }
                else -> when (it) {
                    1, 3 -> getString(R.string.title_ege)
                    else -> getString(R.string.title_oge)
                }
            }
            drawer.setSelectionAtPosition(it)
            supportFragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit()
        })

        m.noInternetSnackbar.observe(this, Observer {
            Snackbar.make(main_activity_root_layout, R.string.no_internet, Snackbar.LENGTH_LONG)
                    .show()
        })

        /*m.betaDialog.observe(this, Observer {
            if (it == null) return@Observer
            alert("""Скорее всего точно последняя бета-версия). Были внедрены следующие фичи:
                |• Придумано название на русском языке
                |• Исправлен баг с 2019 годом
                |• Анимация при показе ответов на 2 часть
                |• Оптимизирован процесс блюра картинки с ответами на 2 часть (renderscript)
                |• Белый текст названия приложения в меню многозадачности
                |• Остановка анимации загрузки варианта при ошибке
                |Требуется проверить стабильность работы на устройствах разных версий Android, желательно на старых (4.0)""".trimMargin(),
                    "Сообщение для бета-тестеров") {
                positiveButton("OK") {}
            }.show()
        })*/
    }

    private fun openYandexDialogsSkill() {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "alice-skill-in-drawer")
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "url")
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

        val skillUri = Uri.parse(getString(R.string.yandex_dialogs_skill_url))
        val aliceIntent = Intent(Intent.ACTION_VIEW, skillUri)
        aliceIntent.component = ComponentName(
                "ru.yandex.searchplugin",
                "ru.yandex.searchplugin.browser.BrowserProxyActivity")
        try {
            startActivity(aliceIntent)
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, skillUri))
        }
    }
}