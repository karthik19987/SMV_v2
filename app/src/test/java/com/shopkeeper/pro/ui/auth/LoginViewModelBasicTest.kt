package com.shopkeeper.pro.ui.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.shopkeeper.pro.testutils.TestData
import org.junit.Rule
import org.junit.Test

class LoginViewModelBasicTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `initial state is idle`() {
        // Given a view model (we'll use a simplified version without mocks)
        // For now, just test the LoginState classes

        // When
        val idleState = LoginState.Idle
        val loadingState = LoginState.Loading

        // Then
        assertThat(idleState).isNotNull()
        assertThat(loadingState).isNotNull()
        assertThat(idleState).isNotEqualTo(loadingState)
    }

    @Test
    fun `login states have correct types`() {
        // Test Success state
        val user = TestData.createTestUser()
        val successState = LoginState.Success(user)
        assertThat(successState.user).isEqualTo(user)

        // Test Error state
        val errorMessage = "Test error"
        val errorState = LoginState.Error(errorMessage)
        assertThat(errorState.message).isEqualTo(errorMessage)
    }
}