package com.banco_platense.api.dto

import java.time.LocalDateTime

data class WalletResponseDto(
    val id: Long,
    val userId: Long,
    val balance: Double,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)