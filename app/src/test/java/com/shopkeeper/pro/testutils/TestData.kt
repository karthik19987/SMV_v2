package com.shopkeeper.pro.testutils

import com.shopkeeper.pro.data.entity.*
import java.util.*

/**
 * Test data builders for unit tests
 */
object TestData {

    fun createTestUser(
        id: String = "test_user_${System.currentTimeMillis()}",
        username: String = "testuser",
        displayName: String = "Test User",
        role: String = "owner",
        isActive: Boolean = true
    ): User {
        return User(
            id = id,
            username = username,
            displayName = displayName,
            role = role,
            createdAt = Date(),
            isActive = isActive
        )
    }

    fun createTestItem(
        id: String = "test_item_${System.currentTimeMillis()}",
        name: String = "Test Item",
        category: ItemCategory = ItemCategory.PRODUCT,
        pricePerKg: Double? = 100.0,
        unit: String = "kg",
        isActive: Boolean = true,
        createdBy: String = "test_user"
    ): Item {
        return Item(
            id = id,
            name = name,
            category = category,
            pricePerKg = pricePerKg,
            unit = unit,
            isActive = isActive,
            createdAt = Date(),
            createdBy = createdBy
        )
    }

    fun createTestSale(
        id: String = "test_sale_${System.currentTimeMillis()}",
        userId: String = "test_user",
        totalAmount: Double = 500.0,
        items: String = """[{"itemId":"item_1","itemName":"Test Item","quantity":1.0,"pricePerUnit":500.0,"totalPrice":500.0}]""",
        paymentMethod: String = "cash"
    ): Sale {
        return Sale(
            id = id,
            userId = userId,
            totalAmount = totalAmount,
            items = items,
            createdAt = Date(),
            paymentMethod = paymentMethod
        )
    }

    fun createTestSaleItem(
        itemId: String = "test_item_${System.currentTimeMillis()}",
        itemName: String = "Test Item",
        quantity: Double = 1.0,
        pricePerUnit: Double = 100.0,
        totalPrice: Double = 100.0
    ): SaleItem {
        return SaleItem(
            itemId = itemId,
            itemName = itemName,
            quantity = quantity,
            pricePerUnit = pricePerUnit,
            totalPrice = totalPrice
        )
    }

    fun createTestExpense(
        id: String = "test_expense_${System.currentTimeMillis()}",
        userId: String = "test_user",
        description: String = "Test Expense",
        amount: Double = 200.0,
        category: ExpenseCategory = ExpenseCategory.OTHER
    ): Expense {
        return Expense(
            id = id,
            userId = userId,
            description = description,
            amount = amount,
            category = category,
            createdAt = Date()
        )
    }

    fun createDefaultItems(userId: String): List<Item> {
        return listOf(
            createTestItem(
                id = "item_1",
                name = "Podipp",
                category = ItemCategory.PRODUCT,
                pricePerKg = 50.0,
                unit = "kg",
                createdBy = userId
            ),
            createTestItem(
                id = "item_2",
                name = "Chilly",
                category = ItemCategory.PRODUCT,
                pricePerKg = 80.0,
                unit = "kg",
                createdBy = userId
            ),
            createTestItem(
                id = "item_3",
                name = "Malli",
                category = ItemCategory.PRODUCT,
                pricePerKg = 120.0,
                unit = "kg",
                createdBy = userId
            ),
            createTestItem(
                id = "item_4",
                name = "Unda",
                category = ItemCategory.PRODUCT,
                pricePerKg = null,
                unit = "pc",
                createdBy = userId
            ),
            createTestItem(
                id = "item_5",
                name = "Repair Service",
                category = ItemCategory.SERVICE,
                pricePerKg = null,
                unit = "service",
                createdBy = userId
            )
        )
    }
}