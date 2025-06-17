package com.banco_platense.api.controller

import com.banco_platense.api.dto.*
import com.banco_platense.api.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
class UserController(
    private val userService: UserService,
) {

    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account with username, email, and password"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "User created successfully",
                content = [Content(
                    mediaType = "text/plain",
                    examples = [ExampleObject(value = "User created successfully")]
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request - user already exists or invalid data",
                content = [Content(
                    mediaType = "text/plain",
                    examples = [ExampleObject(value = "User already exists")]
                )]
            )
        ]
    )
    fun registerUser(@RequestBody registrationRequest: RegistrationRequest): ResponseEntity<String> {
        return when (val result = userService.registerUser(registrationRequest)) {
            is RegistrationResult.Success -> ResponseEntity.ok("User created successfully")
            is RegistrationResult.Failure -> ResponseEntity.badRequest().body(result.message)
        }
    }

    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticates a user and returns a JWT token for accessing protected endpoints"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Login successful",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = LoginResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid credentials",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(value = """{"error": "Invalid credentials"}""")]
                )]
            )
        ]
    )
    fun login(@RequestBody loginRequest: LoginRequest): ResponseEntity<Any> {
        return ResponseEntity.ok(userService.login(loginRequest))
    }
}
