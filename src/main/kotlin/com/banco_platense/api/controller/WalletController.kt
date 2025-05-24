package com.banco_platense.api.controller

import com.banco_platense.api.dto.CreateTransactionDto
import com.banco_platense.api.service.WalletService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/wallets")
class WalletController(private val walletService: WalletService) {

    @GetMapping("/user/{userId}")
    fun getWalletByUserId(@PathVariable userId: Long): ResponseEntity<Any> {
        val wallet = walletService.getWalletByUserId(userId)
        return ResponseEntity.ok(wallet)
    }

    @PostMapping("/{walletId}/transactions")
    fun createTransaction(
        @PathVariable walletId: Long,
        @RequestBody createTransactionDto: CreateTransactionDto
    ): ResponseEntity<Any> {
        val transaction = walletService.createTransaction(walletId, createTransactionDto)
        return ResponseEntity.ok(transaction)
    }

    @GetMapping("/{walletId}/transactions")
    fun getTransactionsByWalletId(@PathVariable walletId: Long): ResponseEntity<Any> {
        val transactions = walletService.getTransactionsByWalletId(walletId)
        return ResponseEntity.ok(transactions)
    }
} 