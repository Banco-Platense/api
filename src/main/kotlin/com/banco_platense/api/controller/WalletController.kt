package com.banco_platense.api.controller

import com.banco_platense.api.dto.CreateTransactionDto
import com.banco_platense.api.service.WalletService
import com.banco_platense.api.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/wallets")
class WalletController(
    private val walletService: WalletService,
    private val userRepository: UserRepository,
) {

    @GetMapping("/user/{userId}")
    fun getWalletByUserId(@PathVariable userId: UUID): ResponseEntity<Any> {
        val currentUsername = getCurrentUsername()
        val currentUser = userRepository.findByUsername(currentUsername)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))

        // Ensure the user is only accessing their own wallet
        if (currentUser.id != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You can only access your own wallet"))
        }

        val wallet = walletService.getWalletByUserId(userId)
        return ResponseEntity.ok(wallet)
    }

    @PostMapping("/{walletId}/transactions")
    fun createTransaction(
        @PathVariable walletId: UUID,
        @RequestBody createTransactionDto: CreateTransactionDto
    ): ResponseEntity<Any> {
        val username = getCurrentUsername()
        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))
        
        // Verify that the wallet belongs to the authenticated user
        val userWallet = walletService.getWalletByUserId(user.id!!)
        if (userWallet.id != walletId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You can only create transactions for your own wallet"))
        }
        
        val transaction = walletService.createTransaction(walletId, createTransactionDto)
        return ResponseEntity.ok(transaction)
    }

    @GetMapping("/{walletId}/transactions")
    fun getTransactionsByWalletId(@PathVariable walletId: UUID): ResponseEntity<Any> {
        val currentUsername = getCurrentUsername()
        val currentUser = userRepository.findByUsername(currentUsername)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))
        
        // Ensure the user is only accessing their own wallet's transactions
        val userWallet = walletService.getWalletByUserId(currentUser.id!!)
        if (userWallet.id != walletId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You can only access transactions from your own wallet"))
        }
        
        val transactions = walletService.getTransactionsByWalletId(walletId)
        return ResponseEntity.ok(transactions)
    }
    
    private fun getCurrentUsername(): String {
        val authentication: Authentication = SecurityContextHolder.getContext().authentication
        return authentication.name
    }
} 