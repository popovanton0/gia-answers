package com.popov.egeanswers.ui


import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.Espresso.pressBack
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.View
import android.view.ViewGroup
import com.linkedin.android.testbutler.TestButler
import com.popov.egeanswers.R
import com.popov.egeanswers.ui.TestUtils.withRecyclerView
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsInstanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun mainActivityTest() {
        Thread.sleep(3000)
        TestButler.setWifiState(false)
        TestButler.setWifiState(true)
        TestButler.setWifiState(false)
        TestButler.setWifiState(true)
        // open first variant
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        onView(withRecyclerView(R.id.variantsView)
                .atPositionOnView(1, R.id.publicationDateTextView))
                .check(matches(anyOf(
                        withText(currentYear.toString()),
                        withText((currentYear + 1).toString())
                )))

        onView(withRecyclerView(R.id.variantsView)
                .atPositionOnView(1, R.id.variantNameTextView))
                .check(matches(withText(startsWith("Variant #"))))

        val recyclerView = onView(
                allOf(withId(R.id.variantsView),
                        childAtPosition(
                                withId(R.id.variantsRootLayout),
                                0)))
        recyclerView.perform(actionOnItemAtPosition<ViewHolder>(0, click()))
        // delay
        Thread.sleep(2500)

        // open answers panel
        onView(withId(R.id.answersLabelTextView))
                .perform(swipeUp())

        Thread.sleep(2500)

        // show part2answers
        val appCompatImageView = onView(
                allOf(withId(R.id.part2answersImageView),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nestedScrollView),
                                        0),
                                0),
                        isDisplayed()))
        appCompatImageView.perform(click())
        appCompatImageView.perform(click())

        // download variant
        val actionMenuItemView = onView(
                allOf(withId(R.id.offline), withContentDescription("Download"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.myToolbar),
                                        1),
                                1),
                        isDisplayed()))
        actionMenuItemView.perform(click())

        val textView3 = onView(
                allOf(withId(R.id.answer_text), withText("№ 1: 30"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.answersView),
                                        0),
                                0),
                        isDisplayed()))
        textView3.check(matches(withText(startsWith("№ 1: "))))

        val textView4 = onView(
                allOf(withId(R.id.answer_text), withText("№ 7: 2"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.answersView),
                                        6),
                                0),
                        isDisplayed()))
        textView4.check(matches(withText("№ 7: 2")))

        pressBack()
        // delay

        recyclerView.perform(actionOnItemAtPosition<ViewHolder>(0, click()))

        // delay
        Thread.sleep(2500)

        onView(withId(R.id.answersLabelTextView))
                .perform(swipeUp())

        val textView5 = onView(
                allOf(withId(R.id.answer_text), withText("№ 7: 2"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.answersView),
                                        6),
                                0),
                        isDisplayed()))
        textView5.check(matches(isDisplayed()))

        pressBack()
        // delay


        recyclerView.perform(actionOnItemAtPosition<ViewHolder>(1, click()))
        // delay
        Thread.sleep(2500)

        onView(withId(R.id.answersLabelTextView))
                .perform(swipeUp())

        val appCompatImageView3 = onView(
                allOf(withId(R.id.part2answersImageView),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nestedScrollView),
                                        0),
                                0),
                        isDisplayed()))
        appCompatImageView3.perform(click())
        appCompatImageView3.perform(click())

        pressBack()

        val floatingActionButton = onView(
                allOf(withId(R.id.searchFAB),
                        childAtPosition(
                                allOf(withId(R.id.variantsRootLayout),
                                        childAtPosition(
                                                withId(R.id.flContent),
                                                0)),
                                1),
                        isDisplayed()))
        floatingActionButton.perform(click())

        val editText = onView(
                allOf(childAtPosition(
                        allOf(withId(android.R.id.custom),
                                childAtPosition(
                                        withClassName(`is`("android.widget.FrameLayout")),
                                        0)),
                        0),
                        isDisplayed()))
        editText.perform(click())
        editText.perform(replaceText("220"), closeSoftKeyboard())


        val appCompatButton = onView(
                allOf(withId(android.R.id.button1), withText("Search"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.ScrollView")),
                                        0),
                                3)))
        appCompatButton.perform(scrollTo(), click())
        // delay
        Thread.sleep(300)

        val textView6 = onView(
                allOf(withText("Variant #220"),
                        childAtPosition(
                                allOf(withId(R.id.myToolbar),
                                        childAtPosition(
                                                IsInstanceOf.instanceOf(android.view.ViewGroup::class.java),
                                                0)),
                                0),
                        isDisplayed()))
        textView6.check(matches(withText("Variant #220")))

        val textView8 = onView(
                allOf(withId(R.id.answer_text), withText("№ 1: 84"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.answersView),
                                        0),
                                0),
                        isDisplayed()))
        textView8.check(matches(withText("№ 1: 84")))

        val textView9 = onView(
                allOf(withId(R.id.answer_text), withText("№ 7: 0"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.answersView),
                                        6),
                                0),
                        isDisplayed()))
        textView9.check(matches(withText("№ 7: 0")))

        pressBack()
    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
