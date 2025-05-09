package com.banco_platense.api.controller

import com.banco_platense.api.dto.RegistrationRequest
import com.banco_platense.api.dto.RegistrationResult
import com.banco_platense.api.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class UserController(private val userService: UserService) {

    @PostMapping("/register")
    fun registerUser(@RequestBody registrationRequest: RegistrationRequest): ResponseEntity<String> {
        return when (val result = userService.registerUser(registrationRequest)) {
            is RegistrationResult.Success -> ResponseEntity.ok("User created successfully")
            is RegistrationResult.Failure -> ResponseEntity.badRequest().body(result.message)
        }
    }
}
