package com.banco_platense.externalservicesmock.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class AccountRequestDto(
    @JsonProperty("accountNumber") val accountNumber: String,
    @JsonProperty("amount") val amount: Double
) 