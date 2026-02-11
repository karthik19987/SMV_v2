package com.shopkeeper.pro.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SalesCalculatorTest {

    @Test
    fun `when price is entered but quantity is empty then default quantity is 1`() {
        // When
        val total = SalesCalculator.calculateItemTotal(quantity = null, pricePerUnit = 100.0)

        // Then
        assertThat(total).isEqualTo(100.0)
    }

    @Test
    fun `when price is entered and quantity is 0 then default quantity is 1`() {
        // When
        val total = SalesCalculator.calculateItemTotal(quantity = 0.0, pricePerUnit = 50.0)

        // Then
        assertThat(total).isEqualTo(50.0)
    }

    @Test
    fun `when both quantity and price are provided then calculate normally`() {
        // When
        val total = SalesCalculator.calculateItemTotal(quantity = 2.5, pricePerUnit = 80.0)

        // Then
        assertThat(total).isEqualTo(200.0)
    }

    @Test
    fun `when price is 0 then total is 0 regardless of quantity`() {
        // When
        val total = SalesCalculator.calculateItemTotal(quantity = 5.0, pricePerUnit = 0.0)

        // Then
        assertThat(total).isEqualTo(0.0)
    }

    @Test
    fun `when both quantity and price are null then total is 0`() {
        // When
        val total = SalesCalculator.calculateItemTotal(quantity = null, pricePerUnit = null)

        // Then
        assertThat(total).isEqualTo(0.0)
    }

    @Test
    fun `calculateGrandTotal sums all item totals correctly`() {
        // Given
        val items = listOf(
            Pair(2.0, 50.0),    // 100
            Pair(null, 80.0),   // 80 (default qty 1)
            Pair(1.5, 120.0),   // 180
            Pair(3.0, 0.0)      // 0
        )

        // When
        val grandTotal = SalesCalculator.calculateGrandTotal(items)

        // Then
        assertThat(grandTotal).isEqualTo(360.0)
    }

    @Test
    fun `formatCurrency formats amount as Indian rupees`() {
        // When
        val formatted = SalesCalculator.formatCurrency(1234.50)

        // Then
        assertThat(formatted).isEqualTo("₹1234.50")
    }

    @Test
    fun `formatCurrency handles zero amount`() {
        // When
        val formatted = SalesCalculator.formatCurrency(0.0)

        // Then
        assertThat(formatted).isEqualTo("₹0.00")
    }

    @Test
    fun `calculateProfit returns correct profit`() {
        // When
        val profit = SalesCalculator.calculateProfit(totalSales = 10000.0, totalExpenses = 3000.0)

        // Then
        assertThat(profit).isEqualTo(7000.0)
    }

    @Test
    fun `calculateProfit handles negative profit (loss)`() {
        // When
        val profit = SalesCalculator.calculateProfit(totalSales = 5000.0, totalExpenses = 8000.0)

        // Then
        assertThat(profit).isEqualTo(-3000.0)
    }

    @Test
    fun `calculateProfitPercentage returns correct percentage`() {
        // When
        val percentage = SalesCalculator.calculateProfitPercentage(totalSales = 10000.0, totalExpenses = 3000.0)

        // Then
        assertThat(percentage).isWithin(0.01).of(70.0)
    }

    @Test
    fun `calculateProfitPercentage handles zero sales`() {
        // When
        val percentage = SalesCalculator.calculateProfitPercentage(totalSales = 0.0, totalExpenses = 1000.0)

        // Then
        assertThat(percentage).isEqualTo(0.0)
    }

    @Test
    fun `calculateProfitPercentage handles loss scenario`() {
        // When
        val percentage = SalesCalculator.calculateProfitPercentage(totalSales = 5000.0, totalExpenses = 8000.0)

        // Then
        assertThat(percentage).isWithin(0.01).of(-60.0)
    }

    @Test
    fun `decimal quantity calculations work correctly`() {
        // When
        val total = SalesCalculator.calculateItemTotal(quantity = 0.5, pricePerUnit = 100.0)

        // Then
        assertThat(total).isEqualTo(50.0)
    }

    @Test
    fun `large number calculations work correctly`() {
        // When
        val total = SalesCalculator.calculateItemTotal(quantity = 1000.0, pricePerUnit = 100.0)

        // Then
        assertThat(total).isEqualTo(100000.0)
    }
}