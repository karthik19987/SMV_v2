package com.shopkeeper.pro.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shopkeeper.pro.MainActivity
import com.shopkeeper.pro.R
import org.hamcrest.CoreMatchers.containsString
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SalesFragmentTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        // Create demo user and navigate to sales
        onView(withId(R.id.btnDemoLogin)).perform(click())
        Thread.sleep(2000)

        // Navigate to sales tab
        onView(withId(R.id.navigation_sales)).perform(click())
        Thread.sleep(500)
    }

    @Test
    fun salesScreen_displaysAllProducts() {
        // Verify all product fields are visible
        onView(withText("Podipp")).check(matches(isDisplayed()))
        onView(withText("Chilly")).check(matches(isDisplayed()))
        onView(withText("Malli")).check(matches(isDisplayed()))
        onView(withText("Unda")).check(matches(isDisplayed()))

        // Verify quantity and price fields exist
        onView(withId(R.id.etPodippQty)).check(matches(isDisplayed()))
        onView(withId(R.id.etPodippPrice)).check(matches(isDisplayed()))
    }

    @Test
    fun enteringPrice_withoutQuantity_defaultsToOne() {
        // Enter only price for Podipp
        onView(withId(R.id.etPodippPrice))
            .perform(typeText("50"), closeSoftKeyboard())

        // Check that total shows ₹50 (1 x 50)
        onView(withId(R.id.tvPodippTotal))
            .check(matches(withText(containsString("50"))))
    }

    @Test
    fun enteringQuantityAndPrice_calculatesTotal() {
        // Enter quantity and price for Chilly
        onView(withId(R.id.etChillyQty))
            .perform(typeText("2.5"), closeSoftKeyboard())
        onView(withId(R.id.etChillyPrice))
            .perform(typeText("80"), closeSoftKeyboard())

        // Check that total shows ₹200 (2.5 x 80)
        onView(withId(R.id.tvChillyTotal))
            .check(matches(withText(containsString("200"))))
    }

    @Test
    fun grandTotal_updatesWithAllItems() {
        // Enter values for multiple items
        onView(withId(R.id.etPodippPrice))
            .perform(typeText("50"), closeSoftKeyboard())  // 1 x 50 = 50

        onView(withId(R.id.etChillyQty))
            .perform(typeText("2"), closeSoftKeyboard())
        onView(withId(R.id.etChillyPrice))
            .perform(typeText("80"), closeSoftKeyboard())  // 2 x 80 = 160

        // Grand total should be ₹210
        onView(withId(R.id.tvTotalAmount))
            .check(matches(withText(containsString("210"))))
    }

    @Test
    fun clearButton_showsConfirmationDialog() {
        // Enter some values
        onView(withId(R.id.etPodippPrice))
            .perform(typeText("50"), closeSoftKeyboard())

        // Click clear button
        onView(withId(R.id.btnClearAll)).perform(click())

        // Verify dialog is shown
        onView(withText("Clear All"))
            .check(matches(isDisplayed()))
        onView(withText("Are you sure you want to clear all fields?"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun confirmClear_clearsAllFields() {
        // Enter some values
        onView(withId(R.id.etPodippQty))
            .perform(typeText("2"), closeSoftKeyboard())
        onView(withId(R.id.etPodippPrice))
            .perform(typeText("50"), closeSoftKeyboard())

        // Click clear and confirm
        onView(withId(R.id.btnClearAll)).perform(click())
        onView(withText("Yes")).perform(click())

        // Verify fields are cleared
        onView(withId(R.id.etPodippQty))
            .check(matches(withText("")))
        onView(withId(R.id.etPodippPrice))
            .check(matches(withText("")))
        onView(withId(R.id.tvTotalAmount))
            .check(matches(withText("₹0")))
    }

    @Test
    fun completeSale_withNoItems_showsError() {
        // Click complete sale without entering any items
        onView(withId(R.id.btnCompleteSale)).perform(click())

        // Should show error message
        onView(withText("Please add at least one item"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun completeSale_withItems_showsSuccessMessage() {
        // Enter sale items
        onView(withId(R.id.etPodippPrice))
            .perform(typeText("50"), closeSoftKeyboard())

        // Complete sale
        onView(withId(R.id.btnCompleteSale)).perform(click())

        // Should show success message
        onView(withText(containsString("Sale completed")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun decimalQuantities_areHandledCorrectly() {
        // Enter decimal quantity
        onView(withId(R.id.etMalliQty))
            .perform(typeText("0.5"), closeSoftKeyboard())
        onView(withId(R.id.etMalliPrice))
            .perform(typeText("120"), closeSoftKeyboard())

        // Check that total shows ₹60 (0.5 x 120)
        onView(withId(R.id.tvMalliTotal))
            .check(matches(withText(containsString("60"))))
    }

    @Test
    fun keyboardNavigation_movesToNextField() {
        // Type in first field and press next
        onView(withId(R.id.etPodippQty))
            .perform(typeText("1"), pressImeActionButton())

        // Should move to price field
        onView(withId(R.id.etPodippPrice))
            .check(matches(hasFocus()))
    }
}