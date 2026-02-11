package com.shopkeeper.pro.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shopkeeper.pro.MainActivity
import com.shopkeeper.pro.R
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginFragmentTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        // Clear any existing user data before each test
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("user_prefs", 0).edit().clear().apply()
    }

    @Test
    fun loginScreen_isDisplayedCorrectly() {
        // Verify all login UI elements are visible
        onView(withId(R.id.etUsername)).check(matches(isDisplayed()))
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()))
        onView(withId(R.id.btnLogin)).check(matches(isDisplayed()))
        onView(withId(R.id.btnDemoLogin)).check(matches(isDisplayed()))
    }

    @Test
    fun emptyUsername_showsError() {
        // Leave username empty
        onView(withId(R.id.etUsername)).perform(clearText())
        onView(withId(R.id.etPassword)).perform(typeText("password"), closeSoftKeyboard())

        // Click login
        onView(withId(R.id.btnLogin)).perform(click())

        // Verify error is shown
        onView(withText("Please enter username"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun emptyPassword_showsError() {
        // Enter username but leave password empty
        onView(withId(R.id.etUsername)).perform(typeText("admin"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(clearText())

        // Click login
        onView(withId(R.id.btnLogin)).perform(click())

        // Verify error is shown
        onView(withText("Please enter password"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun validCredentials_navigatesToDashboard() {
        // Enter valid credentials
        onView(withId(R.id.etUsername)).perform(typeText("admin"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText("password"), closeSoftKeyboard())

        // Click login
        onView(withId(R.id.btnLogin)).perform(click())

        // Wait for navigation
        Thread.sleep(2000)

        // Verify we're on the dashboard
        // The bottom navigation should be visible after successful login
        onView(withId(R.id.navigation_dashboard))
            .check(matches(isDisplayed()))
    }

    @Test
    fun demoLogin_createsUserAndNavigates() {
        // Click demo login button
        onView(withId(R.id.btnDemoLogin)).perform(click())

        // Wait for demo user creation and navigation
        Thread.sleep(2000)

        // Verify we're on the dashboard
        onView(withId(R.id.navigation_dashboard))
            .check(matches(isDisplayed()))
    }

    @Test
    fun progressBar_shownDuringLogin() {
        // Enter credentials
        onView(withId(R.id.etUsername)).perform(typeText("admin"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText("password"), closeSoftKeyboard())

        // Click login
        onView(withId(R.id.btnLogin)).perform(click())

        // Immediately check if progress bar is visible
        onView(withId(R.id.progressBar))
            .check(matches(isDisplayed()))
    }

    @Test
    fun loginButton_isDisabledDuringLogin() {
        // Enter credentials
        onView(withId(R.id.etUsername)).perform(typeText("admin"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText("password"), closeSoftKeyboard())

        // Click login
        onView(withId(R.id.btnLogin)).perform(click())

        // Check that login button is disabled
        onView(withId(R.id.btnLogin))
            .check(matches(not(isEnabled())))
    }
}