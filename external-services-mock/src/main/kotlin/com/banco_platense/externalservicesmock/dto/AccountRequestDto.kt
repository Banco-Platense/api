package com.banco_platense.externalservicesmock.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class AccountRequestDto(
    @JsonProperty("walletId") val walletId: String,
    @JsonProperty("amount") val amount: Double
) 