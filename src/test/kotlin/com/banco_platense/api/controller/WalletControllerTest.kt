package com.banco_platense.api.controller

import com.banco_platense.api.dto.CreateWalletDto
import com.banco_platense.api.dto.UpdateWalletBalanceDto
import com.banco_platense.api.dto.WalletResponseDto
import com.banco_platense.api.service.WalletService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.*

class WalletControllerTest {

    private val walletService: WalletService = mock()
    private val walletController = WalletController(walletService)

    @Test
    fun `should create wallet successfully`() {
        // Given
        val createWalletDto = CreateWalletDto(userId = 1L)
        val walletResponse = WalletResponseDto(
            id = 1L,
            userId = 1L,
            balance = 0.0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        `when`(walletService.createWallet(createWalletDto)).thenReturn(walletResponse)

        // When
        val response = walletController.createWallet(createWalletDto)

        // Then
        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(walletResponse, response.body)
        verify(walletService).createWallet(createWalletDto)
    }

    @Test
    fun `should get wallet by user id successfully`() {
        // Given
        val userId = 1L
        val walletResponse = WalletResponseDto(
            id = 1L,
            userId = userId,
            balance = 100.0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        `when`(walletService.getWalletByUserId(userId)).thenReturn(walletResponse)

        // When
        val response = walletController.getWalletByUserId(userId)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(walletResponse, response.body)
        verify(walletService).getWalletByUserId(userId)
    }

    @Test
    fun `should update wallet balance successfully`() {
        // Given
        val userId = 1L
        val updateDto = UpdateWalletBalanceDto(balance = 200.0)
        val walletResponse = WalletResponseDto(
            id = 1L,
            userId = userId,
            balance = updateDto.balance,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        `when`(walletService.updateWalletBalance(userId, updateDto)).thenReturn(walletResponse)

        // When
        val response = walletController.updateWalletBalance(userId, updateDto)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(walletResponse, response.body)
        verify(walletService).updateWalletBalance(userId, updateDto)
    }
} 