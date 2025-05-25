package com.banco_platense.api.dto

import java.util.UUID

sealed class RegistrationResult {
    data class Success(val userId: UUID) : RegistrationResult()
    data class Failure(val message: String) : RegistrationResult()
}