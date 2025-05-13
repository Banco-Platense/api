package com.banco_platense.api.entity

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

class WalletTest {

    @Test
    fun `should create wallet with correct properties`() {
        // Given
        val userId = 1L
        val balance = 0.0
        val createdAt = LocalDateTime.now()
        val updatedAt = LocalDateTime.now()

        // When
        val wallet = Wallet(
            id = 1L,
            userId = userId,
            balance = balance,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        // Then
        assertEquals(userId, wallet.userId)
        assertEquals(balance, wallet.balance)
        assertEquals(createdAt, wallet.createdAt)
        assertEquals(updatedAt, wallet.updatedAt)
    }

    @Test
    fun `should update wallet balance`() {
        // Given
        val wallet = Wallet(
            id = 1L,
            userId = 1L,
            balance = 0.0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val newBalance = 100.0

        // When
        wallet.balance = newBalance
        wallet.updatedAt = LocalDateTime.now()

        // Then
        assertEquals(newBalance, wallet.balance)
        assertTrue(wallet.updatedAt.isAfter(wallet.createdAt))
    }
} 