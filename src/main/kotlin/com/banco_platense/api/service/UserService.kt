package com.banco_platense.api.service

import com.banco_platense.api.dto.RegistrationRequest
import com.banco_platense.api.dto.RegistrationResult
import com.banco_platense.api.entity.User
import com.banco_platense.api.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetailsService
import com.banco_platense.api.config.JwtUtil
import com.banco_platense.api.dto.LoginRequest
import com.banco_platense.api.dto.LoginResponse
import com.banco_platense.api.dto.UserData

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val walletService: WalletService,
    private val authenticationManager: AuthenticationManager,
    private val userDetailsService: UserDetailsService,
    private val jwtUtil: JwtUtil
) {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")

    @Transactional
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
        )

        val saved = userRepository.save(user)
        
        walletService.createWallet(saved.id!!)
        
        return RegistrationResult.Success(saved.id!!)
    }

    fun getUserByUsername(username: String): User {
        return userRepository.findByUsername(username)!!
    }

    fun login(loginRequest: LoginRequest): LoginResponse {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                loginRequest.username,
                loginRequest.password
            )
        )
        val userDetails = userDetailsService.loadUserByUsername(loginRequest.username)
        val token = jwtUtil.generateToken(userDetails)
        val user = getUserByUsername(loginRequest.username)
        return LoginResponse(token, UserData(username = user.username, email = user.email, id = user.id!!))
    }
}