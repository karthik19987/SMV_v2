package com.shopkeeper.pro.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.shopkeeper.pro.data.database.ShopKeeperDatabase
import com.shopkeeper.pro.data.entity.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var database: ShopKeeperDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ShopKeeperDatabase::class.java
        ).allowMainThreadQueries().build()

        userDao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertUser_and_getUserById_returnsCorrectUser() = runTest {
        // Given
        val user = createTestUser(id = "user_1")

        // When
        userDao.insertUser(user)
        val retrieved = userDao.getUserById("user_1")

        // Then
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.id).isEqualTo(user.id)
        assertThat(retrieved?.username).isEqualTo(user.username)
        assertThat(retrieved?.displayName).isEqualTo(user.displayName)
        assertThat(retrieved?.role).isEqualTo(user.role)
        assertThat(retrieved?.isActive).isTrue()
    }

    @Test
    fun getUserByUsername_returnsCorrectUser() = runTest {
        // Given
        val user = createTestUser(username = "admin")
        userDao.insertUser(user)

        // When
        val retrieved = userDao.getUserByUsername("admin")

        // Then
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.username).isEqualTo("admin")
    }

    @Test
    fun getUserByUsername_returnsNull_whenUserNotFound() = runTest {
        // When
        val retrieved = userDao.getUserByUsername("nonexistent")

        // Then
        assertThat(retrieved).isNull()
    }

    @Test
    fun getAllActiveUsers_returnsOnlyActiveUsers() = runTest {
        // Given
        val activeUser1 = createTestUser(id = "user_1", isActive = true)
        val activeUser2 = createTestUser(id = "user_2", isActive = true)
        val inactiveUser = createTestUser(id = "user_3", isActive = false)

        userDao.insertUser(activeUser1)
        userDao.insertUser(activeUser2)
        userDao.insertUser(inactiveUser)

        // When
        val activeUsers = userDao.getAllActiveUsers().first()

        // Then
        assertThat(activeUsers).hasSize(2)
        assertThat(activeUsers.map { it.id }).containsExactly("user_1", "user_2")
    }

    @Test
    fun updateUser_modifiesExistingUser() = runTest {
        // Given
        val user = createTestUser(id = "user_1", displayName = "Original Name")
        userDao.insertUser(user)

        // When
        val updatedUser = user.copy(displayName = "Updated Name")
        userDao.updateUser(updatedUser)

        val retrieved = userDao.getUserById("user_1")

        // Then
        assertThat(retrieved?.displayName).isEqualTo("Updated Name")
    }

    @Test
    fun deactivateUser_setsIsActiveToFalse() = runTest {
        // Given
        val user = createTestUser(id = "user_1", isActive = true)
        userDao.insertUser(user)

        // When
        userDao.deactivateUser("user_1")
        val retrieved = userDao.getUserById("user_1")

        // Then
        assertThat(retrieved?.isActive).isFalse()
    }

    @Test
    fun insertUser_withConflict_replacesExistingUser() = runTest {
        // Given
        val user1 = createTestUser(id = "user_1", displayName = "First Name")
        val user2 = createTestUser(id = "user_1", displayName = "Second Name")

        // When
        userDao.insertUser(user1)
        userDao.insertUser(user2)

        val retrieved = userDao.getUserById("user_1")

        // Then
        assertThat(retrieved?.displayName).isEqualTo("Second Name")
    }

    @Test
    fun getAllActiveUsers_emitsUpdates_whenDataChanges() = runTest {
        // Given
        val user = createTestUser(id = "user_1", isActive = true)

        // Get initial state
        val initialUsers = userDao.getAllActiveUsers().first()
        assertThat(initialUsers).isEmpty()

        // When - Insert user
        userDao.insertUser(user)
        val afterInsert = userDao.getAllActiveUsers().first()

        // Then
        assertThat(afterInsert).hasSize(1)

        // When - Deactivate user
        userDao.deactivateUser("user_1")
        val afterDeactivate = userDao.getAllActiveUsers().first()

        // Then
        assertThat(afterDeactivate).isEmpty()
    }

    @Test
    fun multipleUsersWithSameUsername_returnsFirstOne() = runTest {
        // Given - Insert multiple users with same username (should not happen in real app)
        val user1 = createTestUser(id = "user_1", username = "admin")
        val user2 = createTestUser(id = "user_2", username = "admin")

        userDao.insertUser(user1)
        userDao.insertUser(user2)

        // When
        val retrieved = userDao.getUserByUsername("admin")

        // Then - Should return one of them (LIMIT 1 in query)
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.username).isEqualTo("admin")
    }

    // Helper function to create test users
    private fun createTestUser(
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
}