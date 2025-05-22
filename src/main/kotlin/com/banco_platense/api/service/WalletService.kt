package com.banco_platense.api.service

import com.banco_platense.api.dto.CreateTransactionDto
import com.banco_platense.api.dto.TransactionResponseDto
import com.banco_platense.api.dto.WalletResponseDto
import com.banco_platense.api.entity.Transaction
import com.banco_platense.api.entity.TransactionType
import com.banco_platense.api.entity.Wallet
import com.banco_platense.api.repository.TransactionRepository
import com.banco_platense.api.repository.WalletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class WalletService(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository
) {

    @Transactional
    fun createWallet(userId: Long): WalletResponseDto {
        val wallet = Wallet(
            userId = userId,
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
    fun createTransaction(walletId: Long, createDto: CreateTransactionDto): TransactionResponseDto {
        val wallet = walletRepository.findById(walletId)
            .orElseThrow { NoSuchElementException("Wallet not found with ID: $walletId") }

        validateTransaction(wallet, createDto)
        
        val transaction = Transaction(
            type = createDto.type,
            amount = createDto.amount,
            timestamp = LocalDateTime.now(),
            description = createDto.description,
            senderWalletId = when (createDto.type) {
                TransactionType.P2P,
                TransactionType.EXTERNAL_DEBIT -> walletId
                TransactionType.EXTERNAL_TOPUP -> null
            },
            receiverWalletId = when (createDto.type) {
                TransactionType.P2P,
                TransactionType.EXTERNAL_TOPUP -> walletId
                TransactionType.EXTERNAL_DEBIT -> null
            },
            externalWalletInfo = createDto.externalWalletInfo
        )
        
        val savedTransaction = transactionRepository.save(transaction)
        updateWalletBalance(wallet, createDto)
        
        return mapToTransactionResponseDto(savedTransaction)
    }

    @Transactional(readOnly = true)
    fun getTransactionsByWalletId(walletId: Long): List<TransactionResponseDto> {
        walletRepository.findById(walletId)
            .orElseThrow { NoSuchElementException("Wallet not found with ID: $walletId") }

        val sentTransactions = transactionRepository.findBySenderWalletId(walletId)
        val receivedTransactions = transactionRepository.findByReceiverWalletId(walletId)
        
        return (sentTransactions + receivedTransactions)
            .map { mapToTransactionResponseDto(it) }
            .sortedByDescending { it.timestamp }
    }

    private fun validateTransaction(wallet: Wallet, createDto: CreateTransactionDto) {
        when (createDto.type) {
            TransactionType.P2P -> {
                require(createDto.amount > 0) { "Amount must be positive for P2P transactions" }
                require(wallet.balance >= createDto.amount) { "Insufficient funds" }
            }
            TransactionType.EXTERNAL_TOPUP -> {
                require(createDto.amount > 0) { "Amount must be positive for external topup" }
                requireNotNull(createDto.externalWalletInfo) { "External wallet info is required for external topup" }
            }
            TransactionType.EXTERNAL_DEBIT -> {
                require(createDto.amount > 0) { "Amount must be positive for external debit" }
                require(wallet.balance >= createDto.amount) { "Insufficient funds" }
                requireNotNull(createDto.externalWalletInfo) { "External wallet info is required for external debit" }
            }
        }
    }

    private fun updateWalletBalance(wallet: Wallet, createDto: CreateTransactionDto) {
        when (createDto.type) {
            TransactionType.P2P -> wallet.balance -= createDto.amount
            TransactionType.EXTERNAL_TOPUP -> wallet.balance += createDto.amount
            TransactionType.EXTERNAL_DEBIT -> wallet.balance -= createDto.amount
        }
        wallet.updatedAt = LocalDateTime.now()
        walletRepository.save(wallet)
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

    private fun mapToTransactionResponseDto(transaction: Transaction): TransactionResponseDto {
        return TransactionResponseDto(
            id = transaction.id,
            type = transaction.type,
            amount = transaction.amount,
            timestamp = transaction.timestamp,
            description = transaction.description,
            senderWalletId = transaction.senderWalletId,
            receiverWalletId = transaction.receiverWalletId,
            externalWalletInfo = transaction.externalWalletInfo
        )
    }
} 