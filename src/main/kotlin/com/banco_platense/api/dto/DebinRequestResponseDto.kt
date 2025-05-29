package com.banco_platense.api.dto

import com.banco_platense.api.entity.DebinStatus
import java.time.LocalDateTime
import java.util.UUID

/**
 * DTO representing a Debin request.
 */
 data class DebinRequestResponseDto(
    val id: UUID?,
    val walletId: UUID,
    val amount: Double,
    val description: String,
    val externalWalletInfo: String,
    val status: DebinStatus,
    val timestamp: LocalDateTime,
    val updatedAt: LocalDateTime
 ) 