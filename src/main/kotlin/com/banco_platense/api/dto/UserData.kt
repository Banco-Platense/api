package com.banco_platense.api.dto

import java.util.*

data class UserData(
    val username: String,
    val id: UUID,
    val email: String
)
