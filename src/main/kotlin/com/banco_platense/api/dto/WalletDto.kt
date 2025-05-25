package com.banco_platense.api.dto

import java.time.LocalDateTime
import java.util.UUID

data class WalletResponseDto(
    val id: UUID?,
    val userId: UUID,
    val balance: Double,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)