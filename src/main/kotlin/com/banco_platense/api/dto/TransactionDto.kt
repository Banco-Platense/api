package com.banco_platense.api.dto

import com.banco_platense.api.entity.TransactionType
import java.time.LocalDateTime

data class CreateTransactionDto(
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val senderWalletId: Long?,
    val receiverWalletId: Long?,
    val externalWalletInfo: String?
)

data class TransactionResponseDto(
    val id: Long,
    val type: TransactionType,
    val amount: Double,
    val timestamp: LocalDateTime,
    val description: String,
    val senderWalletId: Long?,
    val receiverWalletId: Long?,
    val externalWalletInfo: String?
) 