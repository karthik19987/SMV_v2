package com.shopkeeper.pro.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.shopkeeper.pro.data.database.ShopKeeperDatabase
import com.shopkeeper.pro.data.entity.Sale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class SaleDaoTest {

    private lateinit var database: ShopKeeperDatabase
    private lateinit var saleDao: SaleDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ShopKeeperDatabase::class.java
        ).allowMainThreadQueries().build()

        saleDao = database.saleDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertSale_and_getAllSales_returnsInDescendingOrder() = runTest {
        // Given - Create sales with different timestamps
        val sale1 = createTestSale(id = "sale_1", createdAt = Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
        val sale2 = createTestSale(id = "sale_2", createdAt = Date(System.currentTimeMillis() - 1800000)) // 30 min ago
        val sale3 = createTestSale(id = "sale_3", createdAt = Date()) // now

        // When
        saleDao.insertSale(sale1)
        saleDao.insertSale(sale2)
        saleDao.insertSale(sale3)

        val sales = saleDao.getAllSales().first()

        // Then - Should be in descending order by createdAt
        assertThat(sales).hasSize(3)
        assertThat(sales[0].id).isEqualTo("sale_3") // Most recent
        assertThat(sales[1].id).isEqualTo("sale_2")
        assertThat(sales[2].id).isEqualTo("sale_1") // Oldest
    }

    @Test
    fun getSalesByDate_returnsOnlySalesFromSpecificDate() = runTest {
        // Given
        val today = Date()
        val yesterday = Date(today.time - 24 * 60 * 60 * 1000)

        val todaySale1 = createTestSale(id = "today_1", createdAt = today, totalAmount = 100.0)
        val todaySale2 = createTestSale(id = "today_2", createdAt = today, totalAmount = 200.0)
        val yesterdaySale = createTestSale(id = "yesterday_1", createdAt = yesterday, totalAmount = 150.0)

        saleDao.insertSale(todaySale1)
        saleDao.insertSale(todaySale2)
        saleDao.insertSale(yesterdaySale)

        // When
        val todaySales = saleDao.getSalesByDate(today).first()

        // Then
        assertThat(todaySales).hasSize(2)
        assertThat(todaySales.map { it.id }).containsExactly("today_1", "today_2")
    }

    @Test
    fun getTotalSalesForDate_calculatesSumCorrectly() = runTest {
        // Given
        val today = Date()
        val sale1 = createTestSale(totalAmount = 500.0, createdAt = today)
        val sale2 = createTestSale(totalAmount = 300.0, createdAt = today)
        val sale3 = createTestSale(totalAmount = 200.0, createdAt = today)

        saleDao.insertSale(sale1)
        saleDao.insertSale(sale2)
        saleDao.insertSale(sale3)

        // When
        val totalSales = saleDao.getTotalSalesForDate(today)

        // Then
        assertThat(totalSales).isEqualTo(1000.0)
    }

    @Test
    fun getTotalSalesForDate_returnsNull_whenNoSales() = runTest {
        // When
        val totalSales = saleDao.getTotalSalesForDate(Date())

        // Then
        assertThat(totalSales).isNull()
    }

    @Test
    fun getSalesByUser_returnsOnlyUserSales() = runTest {
        // Given
        val user1Sale1 = createTestSale(id = "s1", userId = "user1")
        val user1Sale2 = createTestSale(id = "s2", userId = "user1")
        val user2Sale = createTestSale(id = "s3", userId = "user2")

        saleDao.insertSale(user1Sale1)
        saleDao.insertSale(user1Sale2)
        saleDao.insertSale(user2Sale)

        // When
        val user1Sales = saleDao.getSalesByUser("user1").first()

        // Then
        assertThat(user1Sales).hasSize(2)
        assertThat(user1Sales.map { it.userId }).containsExactly("user1", "user1")
    }

    @Test
    fun deleteSale_removesSaleFromDatabase() = runTest {
        // Given
        val sale = createTestSale(id = "sale_to_delete")
        saleDao.insertSale(sale)

        // Verify it exists
        val beforeDelete = saleDao.getAllSales().first()
        assertThat(beforeDelete).hasSize(1)

        // When
        saleDao.deleteSale("sale_to_delete")

        // Then
        val afterDelete = saleDao.getAllSales().first()
        assertThat(afterDelete).isEmpty()
    }

    @Test
    fun paymentMethod_isStoredCorrectly() = runTest {
        // Given
        val cashSale = createTestSale(id = "cash_sale", paymentMethod = "cash")
        val cardSale = createTestSale(id = "card_sale", paymentMethod = "card")

        // When
        saleDao.insertSale(cashSale)
        saleDao.insertSale(cardSale)

        val sales = saleDao.getAllSales().first()

        // Then
        val cash = sales.find { it.id == "cash_sale" }
        val card = sales.find { it.id == "card_sale" }

        assertThat(cash?.paymentMethod).isEqualTo("cash")
        assertThat(card?.paymentMethod).isEqualTo("card")
    }

    @Test
    fun items_jsonString_isStoredCorrectly() = runTest {
        // Given
        val itemsJson = """[{"itemId":"item_1","itemName":"Podipp","quantity":2.5,"pricePerUnit":50.0,"totalPrice":125.0}]"""
        val sale = createTestSale(items = itemsJson)

        // When
        saleDao.insertSale(sale)

        val retrieved = saleDao.getAllSales().first().first()

        // Then
        assertThat(retrieved.items).isEqualTo(itemsJson)
    }

    @Test
    fun getAllSales_emitsUpdates_whenDataChanges() = runTest {
        // Given
        val sale = createTestSale()

        // Initial state
        val initialSales = saleDao.getAllSales().first()
        assertThat(initialSales).isEmpty()

        // When - Insert sale
        saleDao.insertSale(sale)
        val afterInsert = saleDao.getAllSales().first()

        // Then
        assertThat(afterInsert).hasSize(1)

        // When - Delete sale
        saleDao.deleteSale(sale.id)
        val afterDelete = saleDao.getAllSales().first()

        // Then
        assertThat(afterDelete).isEmpty()
    }

    // Helper function to create test sales
    private fun createTestSale(
        id: String = "test_sale_${System.currentTimeMillis()}",
        userId: String = "test_user",
        totalAmount: Double = 500.0,
        items: String = """[{"itemId":"item_1","itemName":"Test Item","quantity":1.0,"pricePerUnit":500.0,"totalPrice":500.0}]""",
        createdAt: Date = Date(),
        paymentMethod: String = "cash"
    ): Sale {
        return Sale(
            id = id,
            userId = userId,
            totalAmount = totalAmount,
            items = items,
            createdAt = createdAt,
            paymentMethod = paymentMethod
        )
    }
}