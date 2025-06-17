package com.banco_platense.externalservicesmock.controller

import com.banco_platense.externalservicesmock.dto.AccountRequestDto
import com.banco_platense.externalservicesmock.dto.ExternalResponseDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import kotlin.random.Random

@CrossOrigin(
    origins = ["*"],
    allowedHeaders = ["*"],
    methods = [RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS]
)
@RestController
@RequestMapping("/external")
class ExternalMockController {

    companion object {
        private const val ACCEPT_WALLET_ID = "11111111-1111-1111-1111-111111111111"
        private const val REJECT_WALLET_ID = "22222222-2222-2222-2222-222222222222"
    }

    @PostMapping("/debin/request")
    fun debinRequest(@RequestBody request: AccountRequestDto): ResponseEntity<ExternalResponseDto> {
        return when (request.walletId) {
            ACCEPT_WALLET_ID -> ResponseEntity.ok(ExternalResponseDto("success", "Funds transferred successfully"))
            REJECT_WALLET_ID -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ExternalResponseDto("error", "No soy hincha fanatico de platense asi que no acepto el DEBIN"))
            else -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ExternalResponseDto("error", "Wallet not found"))
        }
    }
} 