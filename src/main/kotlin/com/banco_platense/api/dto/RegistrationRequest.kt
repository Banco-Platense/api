package com.banco_platense.api.dto

data class RegistrationRequest(
    val email: String,
    val username: String,
    val password: String
)
