package com.banco_platense.api.dto

sealed class RegistrationResult {
    data class Success(val userId: Long) : RegistrationResult()
    data class Failure(val message: String) : RegistrationResult()
}