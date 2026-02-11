package com.shopkeeper.pro.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.shopkeeper.pro.data.database.ShopKeeperDatabase
import com.shopkeeper.pro.data.entity.Expense
import com.shopkeeper.pro.data.entity.ExpenseCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class ExpenseDaoTest {

    private lateinit var database: ShopKeeperDatabase
    private lateinit var expenseDao: ExpenseDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ShopKeeperDatabase::class.java
        ).allowMainThreadQueries().build()

        expenseDao = database.expenseDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertExpense_and_getAllExpenses_returnsInDescendingOrder() = runTest {
        // Given - Create expenses with different timestamps
        val expense1 = createTestExpense(id = "exp_1", createdAt = Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
        val expense2 = createTestExpense(id = "exp_2", createdAt = Date(System.currentTimeMillis() - 1800000)) // 30 min ago
        val expense3 = createTestExpense(id = "exp_3", createdAt = Date()) // now

        // When
        expenseDao.insertExpense(expense1)
        expenseDao.insertExpense(expense2)
        expenseDao.insertExpense(expense3)

        val expenses = expenseDao.getAllExpenses().first()

        // Then - Should be in descending order by createdAt
        assertThat(expenses).hasSize(3)
        assertThat(expenses[0].id).isEqualTo("exp_3") // Most recent
        assertThat(expenses[1].id).isEqualTo("exp_2")
        assertThat(expenses[2].id).isEqualTo("exp_1") // Oldest
    }

    @Test
    fun getExpensesByDate_returnsOnlyExpensesFromSpecificDate() = runTest {
        // Given
        val today = Date()
        val yesterday = Date(today.time - 24 * 60 * 60 * 1000)

        val todayExpense1 = createTestExpense(id = "today_1", createdAt = today, amount = 100.0)
        val todayExpense2 = createTestExpense(id = "today_2", createdAt = today, amount = 200.0)
        val yesterdayExpense = createTestExpense(id = "yesterday_1", createdAt = yesterday, amount = 150.0)

        expenseDao.insertExpense(todayExpense1)
        expenseDao.insertExpense(todayExpense2)
        expenseDao.insertExpense(yesterdayExpense)

        // When
        val todayExpenses = expenseDao.getExpensesByDate(today).first()

        // Then
        assertThat(todayExpenses).hasSize(2)
        assertThat(todayExpenses.map { it.id }).containsExactly("today_1", "today_2")
    }

    @Test
    fun getTotalExpensesForDate_calculatesSumCorrectly() = runTest {
        // Given
        val today = Date()
        val expense1 = createTestExpense(amount = 500.0, createdAt = today)
        val expense2 = createTestExpense(amount = 300.0, createdAt = today)
        val expense3 = createTestExpense(amount = 200.0, createdAt = today)

        expenseDao.insertExpense(expense1)
        expenseDao.insertExpense(expense2)
        expenseDao.insertExpense(expense3)

        // When
        val totalExpenses = expenseDao.getTotalExpensesForDate(today)

        // Then
        assertThat(totalExpenses).isEqualTo(1000.0)
    }

    @Test
    fun getTotalExpensesForDate_returnsNull_whenNoExpenses() = runTest {
        // When
        val totalExpenses = expenseDao.getTotalExpensesForDate(Date())

        // Then
        assertThat(totalExpenses).isNull()
    }

    @Test
    fun getExpensesByCategory_returnsOnlySpecificCategory() = runTest {
        // Given
        val rentExpense1 = createTestExpense(id = "rent_1", category = ExpenseCategory.RENT)
        val rentExpense2 = createTestExpense(id = "rent_2", category = ExpenseCategory.RENT)
        val purchaseExpense = createTestExpense(id = "purchase_1", category = ExpenseCategory.PURCHASE)
        val billsExpense = createTestExpense(id = "bills_1", category = ExpenseCategory.BILLS)

        expenseDao.insertExpense(rentExpense1)
        expenseDao.insertExpense(rentExpense2)
        expenseDao.insertExpense(purchaseExpense)
        expenseDao.insertExpense(billsExpense)

        // When
        val rentExpenses = expenseDao.getExpensesByCategory(ExpenseCategory.RENT).first()

        // Then
        assertThat(rentExpenses).hasSize(2)
        assertThat(rentExpenses.map { it.id }).containsExactly("rent_1", "rent_2")
    }

    @Test
    fun updateExpense_modifiesExistingExpense() = runTest {
        // Given
        val expense = createTestExpense(id = "exp_1", amount = 100.0, description = "Original")
        expenseDao.insertExpense(expense)

        // When
        val updatedExpense = expense.copy(amount = 150.0, description = "Updated")
        expenseDao.updateExpense(updatedExpense)

        val expenses = expenseDao.getAllExpenses().first()

        // Then
        assertThat(expenses).hasSize(1)
        val retrieved = expenses[0]
        assertThat(retrieved.amount).isEqualTo(150.0)
        assertThat(retrieved.description).isEqualTo("Updated")
    }

    @Test
    fun deleteExpense_removesExpenseFromDatabase() = runTest {
        // Given
        val expense = createTestExpense(id = "exp_to_delete")
        expenseDao.insertExpense(expense)

        // Verify it exists
        val beforeDelete = expenseDao.getAllExpenses().first()
        assertThat(beforeDelete).hasSize(1)

        // When
        expenseDao.deleteExpense(expense)

        // Then
        val afterDelete = expenseDao.getAllExpenses().first()
        assertThat(afterDelete).isEmpty()
    }

    @Test
    fun allExpenseCategories_areHandledCorrectly() = runTest {
        // Given - Test all expense categories
        val expenses = listOf(
            createTestExpense(id = "1", category = ExpenseCategory.PURCHASE),
            createTestExpense(id = "2", category = ExpenseCategory.DAILY_WAGES),
            createTestExpense(id = "3", category = ExpenseCategory.BILLS),
            createTestExpense(id = "4", category = ExpenseCategory.RENT),
            createTestExpense(id = "5", category = ExpenseCategory.ELECTRICITY),
            createTestExpense(id = "6", category = ExpenseCategory.TRANSPORT),
            createTestExpense(id = "7", category = ExpenseCategory.OTHER)
        )

        // When
        expenses.forEach { expenseDao.insertExpense(it) }

        // Then - Verify each category
        val purchaseExpenses = expenseDao.getExpensesByCategory(ExpenseCategory.PURCHASE).first()
        assertThat(purchaseExpenses).hasSize(1)
        assertThat(purchaseExpenses[0].category).isEqualTo(ExpenseCategory.PURCHASE)

        val wagesExpenses = expenseDao.getExpensesByCategory(ExpenseCategory.DAILY_WAGES).first()
        assertThat(wagesExpenses).hasSize(1)
        assertThat(wagesExpenses[0].category).isEqualTo(ExpenseCategory.DAILY_WAGES)

        val billsExpenses = expenseDao.getExpensesByCategory(ExpenseCategory.BILLS).first()
        assertThat(billsExpenses).hasSize(1)
        assertThat(billsExpenses[0].category).isEqualTo(ExpenseCategory.BILLS)

        val rentExpenses = expenseDao.getExpensesByCategory(ExpenseCategory.RENT).first()
        assertThat(rentExpenses).hasSize(1)
        assertThat(rentExpenses[0].category).isEqualTo(ExpenseCategory.RENT)

        val electricityExpenses = expenseDao.getExpensesByCategory(ExpenseCategory.ELECTRICITY).first()
        assertThat(electricityExpenses).hasSize(1)
        assertThat(electricityExpenses[0].category).isEqualTo(ExpenseCategory.ELECTRICITY)

        val transportExpenses = expenseDao.getExpensesByCategory(ExpenseCategory.TRANSPORT).first()
        assertThat(transportExpenses).hasSize(1)
        assertThat(transportExpenses[0].category).isEqualTo(ExpenseCategory.TRANSPORT)

        val otherExpenses = expenseDao.getExpensesByCategory(ExpenseCategory.OTHER).first()
        assertThat(otherExpenses).hasSize(1)
        assertThat(otherExpenses[0].category).isEqualTo(ExpenseCategory.OTHER)
    }

    @Test
    fun getAllExpenses_emitsUpdates_whenDataChanges() = runTest {
        // Given
        val expense = createTestExpense()

        // Initial state
        val initialExpenses = expenseDao.getAllExpenses().first()
        assertThat(initialExpenses).isEmpty()

        // When - Insert expense
        expenseDao.insertExpense(expense)
        val afterInsert = expenseDao.getAllExpenses().first()

        // Then
        assertThat(afterInsert).hasSize(1)

        // When - Delete expense
        expenseDao.deleteExpense(expense)
        val afterDelete = expenseDao.getAllExpenses().first()

        // Then
        assertThat(afterDelete).isEmpty()
    }

    @Test
    fun expenseWithLongDescription_isStoredCorrectly() = runTest {
        // Given
        val longDescription = "This is a very long expense description that contains " +
                "multiple lines and lots of details about what this expense was for. " +
                "It might include vendor information, item details, and other notes."

        val expense = createTestExpense(description = longDescription)

        // When
        expenseDao.insertExpense(expense)
        val retrieved = expenseDao.getAllExpenses().first()

        // Then
        assertThat(retrieved).hasSize(1)
        assertThat(retrieved[0].description).isEqualTo(longDescription)
    }

    // Helper function to create test expenses
    private fun createTestExpense(
        id: String = "test_expense_${System.currentTimeMillis()}",
        userId: String = "test_user",
        description: String = "Test Expense",
        amount: Double = 100.0,
        category: ExpenseCategory = ExpenseCategory.OTHER,
        createdAt: Date = Date()
    ): Expense {
        return Expense(
            id = id,
            userId = userId,
            description = description,
            amount = amount,
            category = category,
            createdAt = createdAt
        )
    }
}