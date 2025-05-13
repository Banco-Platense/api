package com.banco_platense.api.controller

import com.banco_platense.api.dto.CreateWalletDto
import com.banco_platense.api.dto.UpdateWalletBalanceDto
import com.banco_platense.api.dto.WalletResponseDto
import com.banco_platense.api.service.WalletService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/wallets")
class WalletController(private val walletService: WalletService) {

    @PostMapping
    fun createWallet(@RequestBody createWalletDto: CreateWalletDto): ResponseEntity<WalletResponseDto> {
        val wallet = walletService.createWallet(createWalletDto)
        return ResponseEntity.status(HttpStatus.CREATED).body(wallet)
    }

    @GetMapping("/user/{userId}")
    fun getWalletByUserId(@PathVariable userId: Long): ResponseEntity<WalletResponseDto> {
        val wallet = walletService.getWalletByUserId(userId)
        return ResponseEntity.ok(wallet)
    }

    @PutMapping("/user/{userId}/balance")
    fun updateWalletBalance(
        @PathVariable userId: Long,
        @RequestBody updateDto: UpdateWalletBalanceDto
    ): ResponseEntity<WalletResponseDto> {
        val wallet = walletService.updateWalletBalance(userId, updateDto)
        return ResponseEntity.ok(wallet)
    }
} 