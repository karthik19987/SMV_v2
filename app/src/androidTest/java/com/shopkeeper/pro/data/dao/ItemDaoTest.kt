package com.shopkeeper.pro.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.shopkeeper.pro.data.database.ShopKeeperDatabase
import com.shopkeeper.pro.data.entity.Item
import com.shopkeeper.pro.data.entity.ItemCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class ItemDaoTest {

    private lateinit var database: ShopKeeperDatabase
    private lateinit var itemDao: ItemDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ShopKeeperDatabase::class.java
        ).allowMainThreadQueries().build()

        itemDao = database.itemDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertItem_and_getItemById_returnsCorrectItem() = runTest {
        // Given
        val item = createTestItem(id = "item_1", name = "Test Product")

        // When
        itemDao.insertItem(item)
        val retrieved = itemDao.getItemById("item_1")

        // Then
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.id).isEqualTo(item.id)
        assertThat(retrieved?.name).isEqualTo("Test Product")
        assertThat(retrieved?.category).isEqualTo(item.category)
        assertThat(retrieved?.pricePerKg).isEqualTo(item.pricePerKg)
        assertThat(retrieved?.isActive).isTrue()
    }

    @Test
    fun getAllActiveItems_returnsOnlyActiveItems_sortedByName() = runTest {
        // Given
        val item1 = createTestItem(id = "1", name = "Zebra Product", isActive = true)
        val item2 = createTestItem(id = "2", name = "Apple Product", isActive = true)
        val item3 = createTestItem(id = "3", name = "Banana Product", isActive = true)
        val inactiveItem = createTestItem(id = "4", name = "Inactive Product", isActive = false)

        itemDao.insertItem(item1)
        itemDao.insertItem(item2)
        itemDao.insertItem(item3)
        itemDao.insertItem(inactiveItem)

        // When
        val activeItems = itemDao.getAllActiveItems().first()

        // Then
        assertThat(activeItems).hasSize(3)
        assertThat(activeItems[0].name).isEqualTo("Apple Product")
        assertThat(activeItems[1].name).isEqualTo("Banana Product")
        assertThat(activeItems[2].name).isEqualTo("Zebra Product")
    }

    @Test
    fun updateItem_modifiesExistingItem() = runTest {
        // Given
        val item = createTestItem(id = "item_1", pricePerKg = 100.0)
        itemDao.insertItem(item)

        // When
        val updatedItem = item.copy(pricePerKg = 150.0)
        itemDao.updateItem(updatedItem)

        val retrieved = itemDao.getItemById("item_1")

        // Then
        assertThat(retrieved?.pricePerKg).isEqualTo(150.0)
    }

    @Test
    fun deleteItem_setsIsActiveToFalse() = runTest {
        // Given
        val item = createTestItem(id = "item_1", isActive = true)
        itemDao.insertItem(item)

        // When
        itemDao.deleteItem("item_1")
        val retrieved = itemDao.getItemById("item_1")

        // Then - Item still exists but is inactive
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.isActive).isFalse()
    }

    @Test
    fun searchItems_returnsMatchingActiveItems() = runTest {
        // Given
        val item1 = createTestItem(name = "Podipp", isActive = true)
        val item2 = createTestItem(name = "Chilly Powder", isActive = true)
        val item3 = createTestItem(name = "Malli", isActive = true)
        val inactiveItem = createTestItem(name = "Chilly Flakes", isActive = false)

        itemDao.insertItem(item1)
        itemDao.insertItem(item2)
        itemDao.insertItem(item3)
        itemDao.insertItem(inactiveItem)

        // When - Search for "chilly"
        val searchResults = itemDao.searchItems("Chilly").first()

        // Then - Should find only active item with "Chilly"
        assertThat(searchResults).hasSize(1)
        assertThat(searchResults[0].name).isEqualTo("Chilly Powder")
    }

    @Test
    fun searchItems_isCaseInsensitive() = runTest {
        // Given
        val item = createTestItem(name = "PODIPP")
        itemDao.insertItem(item)

        // When
        val searchResults = itemDao.searchItems("podipp").first()

        // Then
        assertThat(searchResults).hasSize(1)
        assertThat(searchResults[0].name).isEqualTo("PODIPP")
    }

    @Test
    fun searchItems_returnsEmptyList_whenNoMatches() = runTest {
        // Given
        val item = createTestItem(name = "Test Item")
        itemDao.insertItem(item)

        // When
        val searchResults = itemDao.searchItems("xyz").first()

        // Then
        assertThat(searchResults).isEmpty()
    }

    @Test
    fun insertItem_withConflict_replacesExistingItem() = runTest {
        // Given
        val item1 = createTestItem(id = "item_1", name = "First Name", pricePerKg = 100.0)
        val item2 = createTestItem(id = "item_1", name = "Second Name", pricePerKg = 200.0)

        // When
        itemDao.insertItem(item1)
        itemDao.insertItem(item2)

        val retrieved = itemDao.getItemById("item_1")

        // Then
        assertThat(retrieved?.name).isEqualTo("Second Name")
        assertThat(retrieved?.pricePerKg).isEqualTo(200.0)
    }

    @Test
    fun itemCategories_areStoredCorrectly() = runTest {
        // Given
        val productItem = createTestItem(id = "1", category = ItemCategory.PRODUCT)
        val serviceItem = createTestItem(id = "2", category = ItemCategory.SERVICE)

        // When
        itemDao.insertItem(productItem)
        itemDao.insertItem(serviceItem)

        // Then
        val product = itemDao.getItemById("1")
        val service = itemDao.getItemById("2")

        assertThat(product?.category).isEqualTo(ItemCategory.PRODUCT)
        assertThat(service?.category).isEqualTo(ItemCategory.SERVICE)
    }

    @Test
    fun pricePerKg_canBeNull() = runTest {
        // Given - Service items might not have per kg pricing
        val item = createTestItem(pricePerKg = null, category = ItemCategory.SERVICE)

        // When
        itemDao.insertItem(item)
        val retrieved = itemDao.getItemById(item.id)

        // Then
        assertThat(retrieved?.pricePerKg).isNull()
    }

    @Test
    fun getAllActiveItems_emitsUpdates_whenDataChanges() = runTest {
        // Given
        val item = createTestItem(id = "item_1", isActive = true)

        // Initial state
        val initialItems = itemDao.getAllActiveItems().first()
        assertThat(initialItems).isEmpty()

        // When - Insert item
        itemDao.insertItem(item)
        val afterInsert = itemDao.getAllActiveItems().first()

        // Then
        assertThat(afterInsert).hasSize(1)

        // When - Deactivate item
        itemDao.deleteItem("item_1")
        val afterDelete = itemDao.getAllActiveItems().first()

        // Then
        assertThat(afterDelete).isEmpty()
    }

    // Helper function to create test items
    private fun createTestItem(
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
}