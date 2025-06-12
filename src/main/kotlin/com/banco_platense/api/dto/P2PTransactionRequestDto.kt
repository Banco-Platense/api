package com.banco_platense.api.dto

import java.util.UUID

/**
 * Request DTO for P2P transactions.
 */
data class P2PTransactionRequestDto(
    val amount: Double,
    val description: String,
    val receiverWalletId: UUID? = null,
    val receiverUsername: String? = null
) 