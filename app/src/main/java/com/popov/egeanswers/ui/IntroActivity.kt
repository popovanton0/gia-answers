package com.popov.egeanswers.ui

import android.app.ActivityManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.paolorotolo.appintro.AppIntro2
import com.github.paolorotolo.appintro.AppIntroFragment
import com.github.paolorotolo.appintro.model.SliderPage
import com.popov.egeanswers.R
import java.util.*


class IntroActivity : AppIntro2() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(ActivityManager.TaskDescription(
                    getString(R.string.app_name),
                    BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher),
                    Color.parseColor("#388E3C")
            ))
        }

        showSkipButton(false)

        val welcomePage = SliderPage().apply {
            title = getString(R.string.app_name)
            description = getString(R.string.welcome_page_description)
            imageDrawable = R.drawable.ic_launcher_web
            bgColor = resources.getColor(R.color.primary)
        }

        val navDrawerPage = SliderPage().apply {
            title = getString(R.string.nav_drawer_page_title)
            description = getString(R.string.nav_drawer_page_desciption)
            imageDrawable = if (resources.configuration.locale == Locale("ru", "RU"))
                R.drawable.nav_drawer_tutorial_ru
            else R.drawable.nav_drawer_tutorial_en
            bgColor = resources.getColor(R.color.primary) // maybe another color
        }

        val downloadShareAndPage = SliderPage().apply {
            title = getString(R.string.download_and_share_page_title)
            description = getString(R.string.download_and_share_page_description)
            imageDrawable = R.drawable.download_and_share_tutorial
            bgColor = resources.getColor(R.color.download_and_share)
        }

        addSlide(AppIntroFragment.newInstance(welcomePage))
        addSlide(AppIntroFragment.newInstance(navDrawerPage))
        addSlide(AppIntroFragment.newInstance(downloadShareAndPage))
        addSlide(IntroUserParamsFragment())
    }

    override fun onDonePressed(currentFragment: Fragment) {
        super.onDonePressed(currentFragment)
        currentFragment.activity?.finish()
    }
}