package com.banco_platense.externalservicesmock.controller

import com.banco_platense.externalservicesmock.dto.AccountRequestDto
import com.banco_platense.externalservicesmock.dto.ExternalResponseDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.random.Random

@RestController
@RequestMapping("/external")
class ExternalMockController {

    companion object {
        private const val ACCEPT_ACCOUNT = "0001"
        private const val REJECT_ACCOUNT = "0002"
    }

    @PostMapping("/top-up")
    fun topUp(@RequestBody request: AccountRequestDto): ResponseEntity<ExternalResponseDto> {
        return if (Random.nextDouble() <= 0.8) {
            ResponseEntity.ok(ExternalResponseDto("success", "Money loaded successfully"))
        } else {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ExternalResponseDto("error", "External service failed"))
        }
    }

    @PostMapping("/debin/request")
    fun debinRequest(@RequestBody request: AccountRequestDto): ResponseEntity<ExternalResponseDto> {
        return when (request.accountNumber) {
            ACCEPT_ACCOUNT -> ResponseEntity.ok(ExternalResponseDto("success", "Money loaded successfully"))
            REJECT_ACCOUNT -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ExternalResponseDto("error", "Error, no soy hincha fanatico de platense asi que no acepto el DEBIN"))
            else -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ExternalResponseDto("error", "Account not found"))
        }
    }
} 