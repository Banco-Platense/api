package com.banco_platense.api.dto

/**
 * Request DTO for external debit transactions.
 */
data class ExternalDebitRequestDto(
    val amount: Double,
    val description: String,
    val externalWalletInfo: String
) 