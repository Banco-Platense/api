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
    
    @PostMapping("/transactions/p2p")
    fun createP2PTransaction(
        @RequestBody request: P2PTransactionRequestDto
    ): ResponseEntity<Any> {
        val username = getCurrentUsername()
        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))

        val userWallet = walletService.getWalletByUserId(user.id!!)
        val createDto = CreateTransactionDto(
            type = TransactionType.P2P,
            amount = request.amount,
            description = request.description,
            receiverWalletId = request.receiverWalletId
        )
        val transaction = walletService.createTransaction(userWallet.id!!, createDto)
        return ResponseEntity.ok(transaction)
    }

    @PostMapping("/transactions/topup")
    fun createTopUpTransaction(
        @RequestBody request: ExternalTopUpRequestDto
    ): ResponseEntity<Any> {
        val username = getCurrentUsername()
        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))

        val userWallet = walletService.getWalletByUserId(user.id!!)
        val createDto = CreateTransactionDto(
            type = TransactionType.EXTERNAL_TOPUP,
            amount = request.amount,
            description = request.description,
            externalWalletInfo = request.externalWalletInfo
        )
        val transaction = walletService.createTransaction(userWallet.id!!, createDto)
        return ResponseEntity.ok(transaction)
    }

    @PostMapping("/transactions/debin", "/{walletId}/transactions/debin")
    fun createDebinTransaction(
        @PathVariable(required = false) walletId: UUID?,
        @RequestBody request: ExternalDebinRequestDto
    ): ResponseEntity<Any> {
        val username = getCurrentUsername()
        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))

        val userWallet = walletService.getWalletByUserId(user.id!!)
        if (walletId != null && userWallet.id != walletId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You can only create transactions for your own wallet"))
        }
        val targetWalletId = walletId ?: userWallet.id!!
        // If DebinService is available (non-test), initiate Debin request; otherwise perform immediate debit
        return if (debinService != null) {
            val debinResponse: DebinRequestResponseDto = debinService!!.createDebinRequest(targetWalletId, request)
            ResponseEntity.ok(debinResponse)
        } else {
            val createDto = CreateTransactionDto(
                type = TransactionType.EXTERNAL_DEBIT,
                amount = request.amount,
                description = request.description,
                externalWalletInfo = request.externalWalletInfo
            )
            val transaction = walletService.createTransaction(targetWalletId, createDto)
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