package com.banco_platense.api.controller

import com.banco_platense.api.dto.CreateTransactionDto
import com.banco_platense.api.service.WalletService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/wallets")
class WalletController(private val walletService: WalletService) {

    @GetMapping("/user/{userId}")
    fun getWalletByUserId(@PathVariable userId: Long): ResponseEntity<Any> {
        return try {
            val wallet = walletService.getWalletByUserId(userId)
            ResponseEntity.ok(wallet)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
        }
    }

    @PostMapping("/{walletId}/transactions")
    fun createTransaction(
        @PathVariable walletId: Long,
        @RequestBody createTransactionDto: CreateTransactionDto
    ): ResponseEntity<Any> {
        return try {
            val transaction = walletService.createTransaction(walletId, createTransactionDto)
            ResponseEntity.ok(transaction)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }

    @GetMapping("/{walletId}/transactions")
    fun getTransactionsByWalletId(@PathVariable walletId: Long): ResponseEntity<Any> {
        return try {
            val transactions = walletService.getTransactionsByWalletId(walletId)
            ResponseEntity.ok(transactions)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
        }
    }
} 