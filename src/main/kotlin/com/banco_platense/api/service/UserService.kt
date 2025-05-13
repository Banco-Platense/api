package com.banco_platense.api.service

import com.banco_platense.api.dto.RegistrationRequest
import com.banco_platense.api.dto.RegistrationResult
import com.banco_platense.api.entity.Drink
import com.banco_platense.api.entity.User
import com.banco_platense.api.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")

    fun registerUser(registrationRequest: RegistrationRequest): RegistrationResult {
        if (!emailRegex.matches(registrationRequest.email)) {
            return RegistrationResult.Failure("Invalid email format")
        }
        if (userRepository.findByEmail(registrationRequest.email) != null) {
            return RegistrationResult.Failure("Email already exists")
        }
        if (userRepository.findByUsername(registrationRequest.username) != null) {
            return RegistrationResult.Failure("Username already exists")
        }

        val encodedPassword = passwordEncoder.encode(registrationRequest.password)
        val user = User(
            email = registrationRequest.email,
            username = registrationRequest.username,
            passwordHash = encodedPassword,
            drinks = if (registrationRequest.username.contains("panchubi")) Drink.MATCHA else Drink.COFFEE
        )

        val saved = userRepository.save(user)
        return RegistrationResult.Success(saved.id!!)
    }
}