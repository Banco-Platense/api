package com.banco_platense.externalservicesmock.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class AccountRequestDto(
    val walletId: String,
    val amount: Double
) 