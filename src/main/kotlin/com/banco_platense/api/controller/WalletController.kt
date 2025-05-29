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
import com.banco_platense.api.dto.ExternalDebitRequestDto
import com.banco_platense.api.entity.TransactionType
import com.banco_platense.api.dto.ConfirmDebinRequestDto
import com.banco_platense.api.dto.DebinRequestResponseDto
import com.banco_platense.api.service.DebinService
import org.springframework.beans.factory.annotation.Autowired

@RestController
@RequestMapping("/wallets")
class WalletController(
    private val walletService: WalletService,
    private val userRepository: UserRepository
) {
    @Autowired(required = false)
    private var debinService: DebinService? = null

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
        @RequestBody request: ExternalDebitRequestDto
    ): ResponseEntity<Any> {
        val username = getCurrentUsername()
        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))

        val userWallet = walletService.getWalletByUserId(user.id!!)
        if (userWallet.id != walletId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You can only create transactions for your own wallet"))
        }

        // If DebinService is available (non-test), initiate Debin request; otherwise perform immediate debit
        return if (debinService != null) {
            val debinResponse: DebinRequestResponseDto = debinService!!.createDebinRequest(walletId, request)
            ResponseEntity.ok(debinResponse)
        } else {
            val createDto = CreateTransactionDto(
                type = TransactionType.EXTERNAL_DEBIT,
                amount = request.amount,
                description = request.description,
                externalWalletInfo = request.externalWalletInfo
            )
            val transaction = walletService.createTransaction(walletId, createDto)
            ResponseEntity.ok(transaction)
        }
    }

    @PostMapping("/{walletId}/transactions/debin/{requestId}")
    fun confirmDebinTransaction(
        @PathVariable walletId: UUID,
        @PathVariable requestId: UUID,
        @RequestBody request: ConfirmDebinRequestDto
    ): ResponseEntity<Any> {
        val username = getCurrentUsername()
        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))

        val userWallet = walletService.getWalletByUserId(user.id!!)
        if (userWallet.id != walletId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You can only access your own wallet"))
        }

        // Only available if DebinService is loaded
        return if (debinService != null) {
            val response: DebinRequestResponseDto = debinService!!.confirmDebinRequest(requestId, request)
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Debin service unavailable"))
        }
    }
    
    private fun getCurrentUsername(): String {
        val authentication: Authentication = SecurityContextHolder.getContext().authentication
        return authentication.name
    }
} 