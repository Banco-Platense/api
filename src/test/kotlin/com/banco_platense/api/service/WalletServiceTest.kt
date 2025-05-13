package com.banco_platense.api.service

import com.banco_platense.api.dto.CreateWalletDto
import com.banco_platense.api.dto.UpdateWalletBalanceDto
import com.banco_platense.api.entity.Wallet
import com.banco_platense.api.repository.WalletRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import java.time.LocalDateTime

class WalletServiceTest {

    private val walletRepository: WalletRepository = mock()
    private val walletService = WalletService(walletRepository)

    @Test
    fun `should create wallet successfully`() {
        // Given
        val createWalletDto = CreateWalletDto(userId = 1L)
        val wallet = Wallet(
            id = 1L,
            userId = createWalletDto.userId,
            balance = 0.0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        `when`(walletRepository.save(any())).thenReturn(wallet)

        // When
        val result = walletService.createWallet(createWalletDto)

        // Then
        assertNotNull(result)
        assertEquals(createWalletDto.userId, result.userId)
        assertEquals(0.0, result.balance)
        verify(walletRepository).save(any())
    }

    @Test
    fun `should get wallet by user id`() {
        // Given
        val userId = 1L
        val wallet = Wallet(
            id = 1L,
            userId = userId,
            balance = 100.0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        `when`(walletRepository.findByUserId(userId)).thenReturn(wallet)

        // When
        val result = walletService.getWalletByUserId(userId)

        // Then
        assertNotNull(result)
        assertEquals(userId, result.userId)
        assertEquals(100.0, result.balance)
        verify(walletRepository).findByUserId(userId)
    }

} 