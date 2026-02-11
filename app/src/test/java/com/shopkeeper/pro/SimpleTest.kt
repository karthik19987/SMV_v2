package com.shopkeeper.pro

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SimpleTest {

    @Test
    fun `simple test should pass`() {
        // Given
        val value = 1 + 1

        // Then
        assertThat(value).isEqualTo(2)
    }

    @Test
    fun `string test should pass`() {
        // Given
        val text = "Hello World"

        // Then
        assertThat(text).contains("Hello")
        assertThat(text).hasLength(11)
    }
}