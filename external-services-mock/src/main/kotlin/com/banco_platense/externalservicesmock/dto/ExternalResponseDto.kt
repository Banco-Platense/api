package com.banco_platense.externalservicesmock.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ExternalResponseDto(
    @JsonProperty("status") val status: String,
    @JsonProperty("message") val message: String
) 