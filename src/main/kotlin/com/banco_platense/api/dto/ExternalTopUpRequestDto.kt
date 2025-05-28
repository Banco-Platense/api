package com.banco_platense.api.dto

/**
 * Request DTO for external top-up transactions.
 */
data class ExternalTopUpRequestDto(
    val amount: Double,
    val description: String,
    val externalWalletInfo: String
) 