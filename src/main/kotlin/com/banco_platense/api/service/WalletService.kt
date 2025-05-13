package com.banco_platense.api.service

import com.banco_platense.api.dto.CreateWalletDto
import com.banco_platense.api.dto.UpdateWalletBalanceDto
import com.banco_platense.api.dto.WalletResponseDto
import com.banco_platense.api.entity.Wallet
import com.banco_platense.api.repository.WalletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class WalletService(private val walletRepository: WalletRepository) {

    @Transactional
    fun createWallet(createWalletDto: CreateWalletDto): WalletResponseDto {
        val wallet = Wallet(
            userId = createWalletDto.userId,
            balance = 0.0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val savedWallet = walletRepository.save(wallet)
        return mapToWalletResponseDto(savedWallet)
    }

    @Transactional(readOnly = true)
    fun getWalletByUserId(userId: Long): WalletResponseDto {
        val wallet = walletRepository.findByUserId(userId)
            ?: throw NoSuchElementException("Wallet not found for user ID: $userId")
        return mapToWalletResponseDto(wallet)
    }

    @Transactional
    fun updateWalletBalance(userId: Long, updateDto: UpdateWalletBalanceDto): WalletResponseDto {
        val wallet = walletRepository.findByUserId(userId)
            ?: throw NoSuchElementException("Wallet not found for user ID: $userId")
        
        wallet.balance = updateDto.balance
        wallet.updatedAt = LocalDateTime.now()
        
        val updatedWallet = walletRepository.save(wallet)
        return mapToWalletResponseDto(updatedWallet)
    }

    private fun mapToWalletResponseDto(wallet: Wallet): WalletResponseDto {
        return WalletResponseDto(
            id = wallet.id,
            userId = wallet.userId,
            balance = wallet.balance,
            createdAt = wallet.createdAt,
            updatedAt = wallet.updatedAt
        )
    }
} 