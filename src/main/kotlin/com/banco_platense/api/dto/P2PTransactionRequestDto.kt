package com.banco_platense.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonInclude
import java.util.UUID

/**
 * Request DTO for P2P transactions.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
data class P2PTransactionRequestDto(
    val amount: Double,
    val description: String,
    @JsonProperty("receiverWalletId")
    val receiverWalletId: UUID? = null,
    @JsonProperty("receiverUsername")
    val receiverUsername: String? = null
) 