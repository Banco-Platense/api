package com.banco_platense.api.dto

import java.time.LocalDateTime

data class CreateWalletDto(
    val userId: Long
)

data class WalletResponseDto(
    val id: Long,
    val userId: Long,
    val balance: Double,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class UpdateWalletBalanceDto(
    val balance: Double
) 