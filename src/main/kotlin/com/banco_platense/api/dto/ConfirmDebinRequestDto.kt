package com.banco_platense.api.dto

import com.banco_platense.api.entity.DebinStatus

/**
 * DTO for confirming a Debin request.
 */
data class ConfirmDebinRequestDto(
    val status: DebinStatus
) 