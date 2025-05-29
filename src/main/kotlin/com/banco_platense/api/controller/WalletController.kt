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
import com.banco_platense.api.dto.P2PTransactionRequestDto
import com.banco_platense.api.dto.ExternalTopUpRequestDto
import com.banco_platense.api.dto.ExternalDebinRequestDto
import com.banco_platense.api.entity.TransactionType

@RestController
@RequestMapping("/wallets")
class WalletController(
    private val walletService: WalletService,
    private val userRepository: UserRepository,
) {

    @GetMapping("/user")
    fun getWalletByUserId(): ResponseEntity<Any> {
        val currentUsername = getCurrentUsername()
        val currentUser = userRepository.findByUsername(currentUsername)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))

        val wallet = walletService.getWalletByUserId(currentUser.id!!)
        return ResponseEntity.ok(wallet)
    }

    @GetMapping("/transactions")
    fun getTransactionsByWalletId(): ResponseEntity<Any> {
        val currentUsername = getCurrentUsername()
        val currentUser = userRepository.findByUsername(currentUsername)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))
        
        val userWallet = walletService.getWalletByUserId(currentUser.id!!)

        val transactions = walletService.getTransactionsByWalletId(userWallet.id!!)
        return ResponseEntity.ok(transactions)
    }
    
    @PostMapping("/{walletId}/transactions/p2p")
    fun createP2PTransaction(
        @PathVariable walletId: UUID,
        @RequestBody request: P2PTransactionRequestDto
    ): ResponseEntity<Any> {
        val username = getCurrentUsername()
        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))

        val userWallet = walletService.getWalletByUserId(user.id!!)
        if (userWallet.id != walletId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You can only create transactions for your own wallet"))
        }

        val createDto = CreateTransactionDto(
            type = TransactionType.P2P,
            amount = request.amount,
            description = request.description,
            receiverWalletId = request.receiverWalletId
        )
        val transaction = walletService.createTransaction(walletId, createDto)
        return ResponseEntity.ok(transaction)
    }

    @PostMapping("/{walletId}/transactions/topup")
    fun createTopUpTransaction(
        @PathVariable walletId: UUID,
        @RequestBody request: ExternalTopUpRequestDto
    ): ResponseEntity<Any> {
        val username = getCurrentUsername()
        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))

        val userWallet = walletService.getWalletByUserId(user.id!!)
        if (userWallet.id != walletId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You can only create transactions for your own wallet"))
        }

        val createDto = CreateTransactionDto(
            type = TransactionType.EXTERNAL_TOPUP,
            amount = request.amount,
            description = request.description,
            externalWalletInfo = request.externalWalletInfo
        )
        val transaction = walletService.createTransaction(walletId, createDto)
        return ResponseEntity.ok(transaction)
    }

    @PostMapping("/{walletId}/transactions/debin")
    fun createDebinTransaction(
        @PathVariable walletId: UUID,
        @RequestBody request: ExternalDebinRequestDto
    ): ResponseEntity<Any> {
        val username = getCurrentUsername()
        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))

        val userWallet = walletService.getWalletByUserId(user.id!!)
        if (userWallet.id != walletId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You can only create transactions for your own wallet"))
        }

        val createDto = CreateTransactionDto(
            type = TransactionType.EXTERNAL_DEBIT,
            amount = request.amount,
            description = request.description,
            externalWalletInfo = request.externalWalletInfo
        )
        val transaction = walletService.createTransaction(walletId, createDto)
        return ResponseEntity.ok(transaction)
    }
    
    private fun getCurrentUsername(): String {
        val authentication: Authentication = SecurityContextHolder.getContext().authentication
        return authentication.name
    }
} 