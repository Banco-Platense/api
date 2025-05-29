package com.banco_platense.api.dto

import com.banco_platense.api.entity.TransactionType
import java.time.LocalDateTime
import java.util.UUID

data class CreateTransactionDto(
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val senderWalletId: UUID? = null,
    val receiverWalletId: UUID? = null,
    val externalWalletInfo: String? = null
)

data class TransactionResponseDto(
    val id: UUID?,
    val type: TransactionType,
    val amount: Double,
    val timestamp: LocalDateTime,
    val description: String,
    val senderWalletId: UUID?,
    val receiverWalletId: UUID?,
    val externalWalletInfo: String?
) 