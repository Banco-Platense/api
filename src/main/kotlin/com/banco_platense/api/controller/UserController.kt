package com.banco_platense.api.controller

import com.banco_platense.api.config.JwtUtil
import com.banco_platense.api.dto.*
import com.banco_platense.api.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RestController
@RequestMapping("/auth")
class UserController(
    private val userService: UserService,
    private val authenticationManager: AuthenticationManager,
    private val userDetailsService: UserDetailsService,
    private val jwtUtil: JwtUtil
) {

    private val logger: Logger = LoggerFactory.getLogger(UserController::class.java)

    @PostMapping("/register")
    fun registerUser(@RequestBody registrationRequest: RegistrationRequest): ResponseEntity<String> {
        return when (val result = userService.registerUser(registrationRequest)) {
            is RegistrationResult.Success -> ResponseEntity.ok("User created successfully")
            is RegistrationResult.Failure -> ResponseEntity.badRequest().body(result.message)
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody loginRequest: LoginRequest): ResponseEntity<Any> {
        logger.info("Login attempt for user: {}", loginRequest.username)
        
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                loginRequest.username,
                loginRequest.password
            )
        )
        
        val userDetails = userDetailsService.loadUserByUsername(loginRequest.username)
        val token = jwtUtil.generateToken(userDetails)
        logger.info("Login successful for user: {}", loginRequest.username)
        val user = userService.getUserByUsername(loginRequest.username)
        
        return ResponseEntity.ok(LoginResponse(token, UserData(username = user.username, email = user.email, id = user.id!!)))
    }
}
