package com.shopkeeper.pro.util

/**
 * Utility class for sales calculations
 */
object SalesCalculator {

    /**
     * Calculate the total price for an item
     * @param quantity The quantity of items. If null or 0 and price is > 0, defaults to 1
     * @param pricePerUnit The price per unit
     * @return The total price
     */
    fun calculateItemTotal(quantity: Double?, pricePerUnit: Double?): Double {
        val price = pricePerUnit ?: 0.0
        val qty = when {
            price > 0 && (quantity == null || quantity == 0.0) -> 1.0
            else -> quantity ?: 0.0
        }
        return qty * price
    }

    /**
     * Calculate grand total from multiple items
     * @param items List of pairs (quantity, pricePerUnit)
     * @return The grand total
     */
    fun calculateGrandTotal(items: List<Pair<Double?, Double?>>): Double {
        return items.sumOf { (quantity, price) ->
            calculateItemTotal(quantity, price)
        }
    }

    /**
     * Format amount as Indian currency
     */
    fun formatCurrency(amount: Double): String {
        return "₹${String.format("%.2f", amount)}"
    }

    /**
     * Calculate profit (sales - expenses)
     */
    fun calculateProfit(totalSales: Double, totalExpenses: Double): Double {
        return totalSales - totalExpenses
    }

    /**
     * Calculate profit percentage
     */
    fun calculateProfitPercentage(totalSales: Double, totalExpenses: Double): Double {
        return if (totalSales > 0) {
            ((totalSales - totalExpenses) / totalSales) * 100
        } else {
            0.0
        }
    }
}