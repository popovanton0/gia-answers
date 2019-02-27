package com.popov.egeanswers.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.danielstone.materialaboutlibrary.MaterialAboutActivity
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import com.google.firebase.analytics.FirebaseAnalytics
import com.popov.egeanswers.R
import com.popov.egeanswers.dao.LarinEGEVariantDao
import com.popov.egeanswers.dao.LarinOGEVariantDao
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast
import java.io.File
import java.io.IOException


class AboutActivity : MaterialAboutActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(ActivityManager.TaskDescription(
                    getString(R.string.app_name),
                    BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher),
                    resources.getColor(R.color.colorPrimaryDark)
            ))
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
    }

    override fun getMaterialAboutList(context: Context): MaterialAboutList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(ActivityManager.TaskDescription(
                    getString(R.string.app_name),
                    BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher),
                    Color.parseColor("#388E3C")
            ))
        }
        return MaterialAboutList.Builder()
                .addCard(MaterialAboutCard.Builder()
                        .addItem(MaterialAboutTitleItem.Builder()
                                .text(R.string.app_name)
                                .desc(R.string.description)
                                .icon(R.mipmap.ic_launcher)
                                .build())
                        .addItem(MaterialAboutActionItem.Builder()
                                .text(R.string.version_text)
                                .subText(packageManager.getPackageInfo(packageName, 0).versionName)
                                .icon(R.drawable.ic_info_outline_grey_24dp)
                                .build())
                        .build())
                .addCard(MaterialAboutCard.Builder()
                        //.title("")
                        .addItem(MaterialAboutActionItem.Builder()
                                .text(R.string.privacy_policy)
                                .icon(R.drawable.ic_shield_lock_grey_600)
                                .setOnClickAction {
                                    val bundle = Bundle()
                                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "privacy-policy")
                                    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "url")
                                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

                                    startActivity(Intent(Intent.ACTION_VIEW,
                                            Uri.parse(getString(R.string.privacy_policy_url)))
                                    )
                                }
                                .build())
                        .addItem(MaterialAboutActionItem.Builder()
                                .text(R.string.terms_and_conditions)
                                .icon(R.drawable.ic_file_document_grey_600)
                                .setOnClickAction {
                                    val bundle = Bundle()
                                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "terms-and-conditions")
                                    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "url")
                                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

                                    startActivity(Intent(Intent.ACTION_VIEW,
                                            Uri.parse(getString(R.string.terms_and_conditions_url)))
                                    )
                                }
                                .build())
                        .addItem(MaterialAboutActionItem.Builder()
                                .text(R.string.about_launcher_icon_license)
                                .subText(R.string.about_launcher_icon_description)
                                .icon(R.drawable.ic_launcher_foreground)
                                .build())
                        .build())
                .addCard(MaterialAboutCard.Builder()
                        .title(R.string.author_title)
                        .addItem(MaterialAboutActionItem.Builder()
                                .text(R.string.author_name)
                                .subText(R.string.author_profession)
                                .icon(R.drawable.ic_person_grey)
                                .build()
                        )
                        .addItem(MaterialAboutActionItem.Builder()
                                .text(R.string.github)
                                .icon(R.drawable.ic_github_circle_grey_24dp)
                                .setOnClickAction {
                                    val bundle = Bundle()
                                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "github")
                                    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "url")
                                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

                                    startActivity(Intent(Intent.ACTION_VIEW,
                                            Uri.parse(getString(R.string.author_on_github_url)))
                                    )
                                }
                                .build())
                        .build())
                /*.addCard(MaterialAboutCard.Builder()
                        .title(R.string.about_icons_title)
                        .addItem(MaterialAboutActionItem.Builder()
                                .text(R.string.about_launcher_icon_license)
                                .subText(R.string.about_launcher_icon_description)
                                .icon(R.drawable.ic_launcher_foreground)
                                .build())
                        .build())*/
                .addCard(MaterialAboutCard.Builder()
                        .title(R.string.used_space)
                        .addItem(MaterialAboutActionItem.Builder()
                                .text(R.string.total_used_space)
                                .subText((fileSize(filesDir) / 1024 / 1024).toString() + "MB")
                                .icon(R.drawable.ic_memory_grey_600)
                                .build())
                        .addItem(MaterialAboutActionItem.Builder()
                                .text(R.string.delete_all_title)
                                .icon(R.drawable.ic_delete_grey_600)
                                .setOnClickAction {
                                    alert(R.string.delete_all_message, R.string.delete_all_title) {
                                        positiveButton(R.string.yes) {
                                            val daoEGE = LarinEGEVariantDao()
                                            val daoOGE = LarinOGEVariantDao()
                                            if (daoEGE.deleteAll() && daoOGE.deleteAll()) toast(R.string.deletion_successful)
                                            else toast(R.string.deletion_failed)
                                            daoEGE.close()
                                            daoOGE.close()
                                        }
                                        negativeButton(R.string.no) {}
                                    }.show()
                                }
                                .build())
                        .build())
                .build()
    }

    override fun getActivityTitle(): CharSequence {
        return getString(R.string.drawer_item_about)
    }

    private fun fileSize(root: File?): Long {
        if (root == null) {
            return 0
        }
        if (root.isFile) {
            return root.length()
        }
        try {
            if (isSymlink(root)) {
                return 0
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return 0
        }

        var length: Long = 0
        val files = root.listFiles() ?: return 0
        for (file in files) length += fileSize(file)
        return length
    }

    private fun isSymlink(file: File): Boolean {
        val canon: File = if (file.parent == null) file
        else File(file.parentFile.canonicalFile, file.name)
        return canon.canonicalFile != canon.absoluteFile
    }
}
