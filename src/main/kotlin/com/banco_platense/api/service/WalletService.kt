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
import java.util.UUID

@Service
class WalletService(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    private val externalPaymentService: ExternalPaymentService
) {

    companion object {
        private const val ACCEPT_WALLET_ID = "11111111-1111-1111-1111-111111111111"
    }

    @Transactional
    fun createWallet(userId: UUID): WalletResponseDto {
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
    fun getWalletByUserId(userId: UUID): WalletResponseDto {
        val wallet = walletRepository.findByUserId(userId)
            ?: throw NoSuchElementException("Wallet not found for user ID: $userId")
        return mapToWalletResponseDto(wallet)
    }

    @Transactional
    fun createTransaction(walletId: UUID, createDto: CreateTransactionDto): TransactionResponseDto {
        val wallet = walletRepository.findById(walletId)
            .orElseThrow { NoSuchElementException("Wallet not found with ID: $walletId") }

        if (createDto.senderWalletId != null && createDto.type != TransactionType.EXTERNAL_TOPUP) {
            require(createDto.senderWalletId == walletId) {
                "For security reasons, senderWalletId must match the authenticated user's wallet ID" 
            }
        }

        validateTransaction(wallet, createDto)
        
        // Simulate external interaction for top-up or DEBIN and obtain an external transaction ID
        val externalInfo = when (createDto.type) {
            TransactionType.EXTERNAL_TOPUP -> externalPaymentService.topUp(createDto.amount, createDto.externalWalletInfo!!)
            TransactionType.EXTERNAL_DEBIN -> externalPaymentService.debin(createDto.amount, createDto.externalWalletInfo!!)
            else -> createDto.externalWalletInfo
        }
        
        val transaction = Transaction(
            type = createDto.type,
            amount = createDto.amount,
            timestamp = LocalDateTime.now(),
            description = createDto.description,
            senderWalletId = when (createDto.type) {
                TransactionType.P2P,
                TransactionType.EXTERNAL_DEBIN -> null
                TransactionType.EXTERNAL_TOPUP -> null
            },
            receiverWalletId = when (createDto.type) {
                TransactionType.P2P -> createDto.receiverWalletId
                TransactionType.EXTERNAL_TOPUP -> walletId
                TransactionType.EXTERNAL_DEBIN -> walletId
            },
            externalWalletInfo = externalInfo
        )
        
        val savedTransaction = transactionRepository.save(transaction)
        updateWalletBalance(wallet, createDto)
        
        if (createDto.type == TransactionType.P2P && createDto.receiverWalletId != null) {
            val receiverWallet = walletRepository.findById(createDto.receiverWalletId)
                .orElseThrow { NoSuchElementException("Receiver wallet not found with ID: ${createDto.receiverWalletId}") }
            receiverWallet.balance += createDto.amount
            receiverWallet.updatedAt = LocalDateTime.now()
            walletRepository.save(receiverWallet)
        }
        
        return mapToTransactionResponseDto(savedTransaction)
    }

    @Transactional(readOnly = true)
    fun getTransactionsByWalletId(walletId: UUID): List<TransactionResponseDto> {
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
                requireNotNull(createDto.receiverWalletId) { "Receiver wallet ID is required for P2P transactions" }
                require(createDto.receiverWalletId != wallet.id) { "Cannot send money to yourself" }
            }
            TransactionType.EXTERNAL_TOPUP -> {
                require(createDto.amount > 0) { "Amount must be positive for external topup" }
                requireNotNull(createDto.externalWalletInfo) { "External wallet info is required for external topup" }
            }
            TransactionType.EXTERNAL_DEBIN -> {
                require(createDto.amount > 0) { "Amount must be positive for external debit" }
                requireNotNull(createDto.externalWalletInfo) { "External wallet info is required for external debit" }
            }
        }
    }

    private fun updateWalletBalance(wallet: Wallet, createDto: CreateTransactionDto) {
        when (createDto.type) {
            TransactionType.P2P -> wallet.balance -= createDto.amount
            TransactionType.EXTERNAL_TOPUP -> wallet.balance += createDto.amount
            TransactionType.EXTERNAL_DEBIN -> wallet.balance += createDto.amount
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