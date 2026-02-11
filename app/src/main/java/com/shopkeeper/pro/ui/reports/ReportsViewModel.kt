package com.shopkeeper.pro.ui.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.shopkeeper.pro.data.database.ShopKeeperDatabase
import com.shopkeeper.pro.data.entity.ExpenseCategory
import com.shopkeeper.pro.data.entity.Sale
import com.shopkeeper.pro.data.entity.Expense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ShopKeeperDatabase.getDatabase(application)
    private val saleDao = database.saleDao()
    private val expenseDao = database.expenseDao()

    private val _reportData = MutableLiveData<ReportData>()
    val reportData: LiveData<ReportData> = _reportData

    private val _dateRange = MutableLiveData<Pair<Date, Date>>()
    val dateRange: LiveData<Pair<Date, Date>> = _dateRange

    private val _periodLabel = MutableLiveData<String>()
    val periodLabel: LiveData<String> = _periodLabel

    init {
        // Default to today's report
        selectToday()
    }

    fun selectToday() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.time

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        _periodLabel.value = "Today: ${dateFormat.format(startDate)}"
        _dateRange.value = Pair(startDate, endDate)
        loadReport(startDate, endDate)
    }

    fun selectThisWeek() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        calendar.add(Calendar.DAY_OF_WEEK, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.time

        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        _periodLabel.value = "This Week: ${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
        _dateRange.value = Pair(startDate, endDate)
        loadReport(startDate, endDate)
    }

    fun selectThisMonth() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.time

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        _periodLabel.value = "This Month: ${monthFormat.format(startDate)}"
        _dateRange.value = Pair(startDate, endDate)
        loadReport(startDate, endDate)
    }

    fun selectThisYear() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        calendar.set(Calendar.MONTH, Calendar.DECEMBER)
        calendar.set(Calendar.DAY_OF_MONTH, 31)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.time

        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        _periodLabel.value = "This Year: ${yearFormat.format(startDate)}"
        _dateRange.value = Pair(startDate, endDate)
        loadReport(startDate, endDate)
    }

    fun selectCustomRange(startDate: Date, endDate: Date) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        _periodLabel.value = "Custom: ${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
        _dateRange.value = Pair(startDate, endDate)
        loadReport(startDate, endDate)
    }

    private fun loadReport(startDate: Date, endDate: Date) {
        viewModelScope.launch {
            try {
                // Get all sales in date range
                val allSales = saleDao.getAllSales().first()
                val salesInRange = allSales.filter { sale ->
                    sale.createdAt.time >= startDate.time && sale.createdAt.time <= endDate.time
                }

                // Get all expenses in date range
                val allExpenses = expenseDao.getAllExpenses().first()
                val expensesInRange = allExpenses.filter { expense ->
                    expense.createdAt.time >= startDate.time && expense.createdAt.time <= endDate.time
                }

                // Calculate totals
                val totalSales = salesInRange.sumOf { it.totalAmount }
                val totalExpenses = expensesInRange.sumOf { it.amount }
                val netProfit = totalSales - totalExpenses

                // Calculate sales breakdown by item
                val salesByItem = mutableMapOf<String, ItemSummary>()
                salesInRange.forEach { sale ->
                    try {
                        val itemsArray = JSONArray(sale.items)
                        for (i in 0 until itemsArray.length()) {
                            val item = itemsArray.getJSONObject(i)
                            val itemName = item.getString("itemName")
                            val quantity = item.getDouble("quantity")
                            val totalPrice = item.getDouble("totalPrice")

                            val existing = salesByItem[itemName] ?: ItemSummary(itemName, 0.0, 0.0)
                            salesByItem[itemName] = existing.copy(
                                quantity = existing.quantity + quantity,
                                amount = existing.amount + totalPrice
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Calculate expenses breakdown by category
                val expensesByCategory = expensesInRange
                    .groupBy { it.category }
                    .map { (category, expenses) ->
                        CategorySummary(
                            category = category,
                            count = expenses.size,
                            amount = expenses.sumOf { it.amount }
                        )
                    }
                    .sortedByDescending { it.amount }

                _reportData.value = ReportData(
                    totalSales = totalSales,
                    salesCount = salesInRange.size,
                    totalExpenses = totalExpenses,
                    expensesCount = expensesInRange.size,
                    netProfit = netProfit,
                    salesByItem = salesByItem.values.toList().sortedByDescending { it.amount },
                    expensesByCategory = expensesByCategory,
                    sales = salesInRange,
                    expenses = expensesInRange
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _reportData.value = ReportData()
            }
        }
    }

    fun generateCSVContent(): String {
        val report = _reportData.value ?: return ""
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

        val csv = StringBuilder()

        // Header
        csv.appendLine("ShopKeeper Pro Report")
        csv.appendLine("Period: ${_periodLabel.value}")
        csv.appendLine("Generated on: ${dateFormat.format(Date())}")
        csv.appendLine()

        // Summary
        csv.appendLine("Summary")
        csv.appendLine("Total Sales,₹${String.format("%.2f", report.totalSales)}")
        csv.appendLine("Total Expenses,₹${String.format("%.2f", report.totalExpenses)}")
        csv.appendLine("Net Profit,₹${String.format("%.2f", report.netProfit)}")
        csv.appendLine("Number of Sales,${report.salesCount}")
        csv.appendLine("Number of Expenses,${report.expensesCount}")
        csv.appendLine()

        // Sales by Item
        csv.appendLine("Sales by Item")
        csv.appendLine("Item,Quantity,Amount")
        report.salesByItem.forEach { item ->
            csv.appendLine("${item.name},${item.quantity},₹${String.format("%.2f", item.amount)}")
        }
        csv.appendLine()

        // Expenses by Category
        csv.appendLine("Expenses by Category")
        csv.appendLine("Category,Count,Amount")
        report.expensesByCategory.forEach { category ->
            csv.appendLine("${category.category.name.replace("_", " ")},${category.count},₹${String.format("%.2f", category.amount)}")
        }
        csv.appendLine()

        // Detailed Sales
        csv.appendLine("Detailed Sales")
        csv.appendLine("Date,Items,Total Amount")
        report.sales.forEach { sale ->
            val itemCount = try {
                JSONArray(sale.items).length()
            } catch (e: Exception) { 0 }
            csv.appendLine("${dateFormat.format(sale.createdAt)},$itemCount items,₹${String.format("%.2f", sale.totalAmount)}")
        }
        csv.appendLine()

        // Detailed Expenses
        csv.appendLine("Detailed Expenses")
        csv.appendLine("Date,Category,Description,Amount")
        report.expenses.forEach { expense ->
            csv.appendLine("${dateFormat.format(expense.createdAt)},${expense.category.name.replace("_", " ")},${expense.description},₹${String.format("%.2f", expense.amount)}")
        }

        return csv.toString()
    }
}

data class ReportData(
    val totalSales: Double = 0.0,
    val salesCount: Int = 0,
    val totalExpenses: Double = 0.0,
    val expensesCount: Int = 0,
    val netProfit: Double = 0.0,
    val salesByItem: List<ItemSummary> = emptyList(),
    val expensesByCategory: List<CategorySummary> = emptyList(),
    val sales: List<Sale> = emptyList(),
    val expenses: List<Expense> = emptyList()
)

data class ItemSummary(
    val name: String,
    val quantity: Double,
    val amount: Double
)

data class CategorySummary(
    val category: ExpenseCategory,
    val count: Int,
    val amount: Double
)