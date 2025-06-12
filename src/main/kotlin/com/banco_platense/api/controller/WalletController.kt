package com.banco_platense.api.controller

import com.banco_platense.api.dto.CreateTransactionDto
import com.banco_platense.api.service.WalletService
import com.banco_platense.api.repository.UserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import com.banco_platense.api.dto.P2PTransactionRequestDto
import com.banco_platense.api.dto.ExternalTopUpRequestDto
import com.banco_platense.api.dto.ExternalDebinRequestDto
import com.banco_platense.api.dto.WalletResponseDto
import com.banco_platense.api.dto.TransactionResponseDto
import com.banco_platense.api.dto.UserData
import com.banco_platense.api.entity.TransactionType
import com.banco_platense.api.entity.User
import java.util.*

@RestController
@RequestMapping("/wallets")
@Tag(name = "Wallets", description = "Wallet management and transaction endpoints")
@SecurityRequirement(name = "JWT")
class WalletController(
    private val walletService: WalletService,
    private val userRepository: UserRepository,
) {

    @GetMapping("/user")
    @Operation(
        summary = "Get user's wallet",
        description = "Retrieves the wallet information for the authenticated user"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Wallet information retrieved successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = WalletResponseDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Invalid or missing JWT token",
                content = [Content(mediaType = "application/json")]
            )
        ]
    )
    fun getWalletByUserId(): ResponseEntity<Any> {
        val currentUsername = getCurrentUsername()
        val currentUser = userRepository.findByUsername(currentUsername)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))

        val wallet = walletService.getWalletByUserId(currentUser.id!!)
        return ResponseEntity.ok(wallet)
    }

    @GetMapping("/transactions")
    @Operation(
        summary = "Get user's transactions",
        description = "Retrieves all transactions for the authenticated user's wallet"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Transactions retrieved successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Array<TransactionResponseDto>::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Invalid or missing JWT token",
                content = [Content(mediaType = "application/json")]
            )
        ]
    )
    fun getTransactionsByWalletId(): ResponseEntity<Any> {
        val currentUsername = getCurrentUsername()
        val currentUser = userRepository.findByUsername(currentUsername)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))
        
        val userWallet = walletService.getWalletByUserId(currentUser.id!!)

        val transactions = walletService.getTransactionsByWalletId(userWallet.id!!)
        return ResponseEntity.ok(transactions)
    }
    
    @PostMapping("/transactions/p2p")
    @Operation(
        summary = "Create P2P transaction",
        description = "Creates a peer-to-peer transaction between wallets"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "P2P transaction created successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = TransactionResponseDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request - insufficient funds or invalid data",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User or receiver wallet not found",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Invalid or missing JWT token",
                content = [Content(mediaType = "application/json")]
            )
        ]
    )
    fun createP2PTransaction(
        @RequestBody request: P2PTransactionRequestDto
    ): ResponseEntity<Any> {
        val username = getCurrentUsername()
        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))

        val userWallet = walletService.getWalletByUserId(user.id!!)

        // Determine receiver wallet ID based on provided wallet ID or username
        val receiverWalletId = request.receiverWalletId ?: run {
            if (request.receiverUsername.isNullOrBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("error" to "Receiver wallet ID or username must be provided"))
            }
            val receiverUser = userRepository.findByUsername(request.receiverUsername)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to "Receiver user not found"))
            walletService.getWalletByUserId(receiverUser.id!!).id!!
        }

        val createDto = CreateTransactionDto(
            type = TransactionType.P2P,
            amount = request.amount,
            description = request.description,
            receiverWalletId = receiverWalletId
        )

        val transaction = walletService.createTransaction(userWallet.id!!, createDto)
        return ResponseEntity.ok(transaction)
    }

    @PostMapping("/transactions/topup")
    @Operation(
        summary = "Create top-up transaction",
        description = "Creates an external top-up transaction to add funds to the wallet"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Top-up transaction created successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = TransactionResponseDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request - invalid external wallet info or amount",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Invalid or missing JWT token",
                content = [Content(mediaType = "application/json")]
            )
        ]
    )
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

    @PostMapping("/transactions/debin")
    @Operation(
        summary = "Create debin transaction",
        description = "Creates an external debin transaction to withdraw funds from the wallet"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Debin transaction created successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = TransactionResponseDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request - insufficient funds, invalid external wallet info, or invalid amount",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Invalid or missing JWT token",
                content = [Content(mediaType = "application/json")]
            )
        ]
    )
    fun createDebinTransaction(
        @RequestBody request: ExternalDebinRequestDto
    ): ResponseEntity<Any> {
        val username = getCurrentUsername()
        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))

        val userWallet = walletService.getWalletByUserId(user.id!!)

        val createDto = CreateTransactionDto(
            type = TransactionType.EXTERNAL_DEBIN,
            amount = request.amount,
            description = request.description,
            externalWalletInfo = request.externalWalletInfo
        )
        val transaction = walletService.createTransaction(userWallet.id!!, createDto)
        return ResponseEntity.ok(transaction)
    }
    
    private fun getCurrentUsername(): String {
        val authentication: Authentication = SecurityContextHolder.getContext().authentication
        return authentication.name
    }

    @GetMapping("/{walletId}/user")
    @Operation(
        summary = "Get user data by wallet ID",
        description = "Retrieves the user information associated with the given wallet ID"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "User information retrieved successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserData::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Wallet or user not found",
                content = [Content(mediaType = "application/json")]
            )
        ]
    )
    fun getUserByWalletId(@PathVariable walletId: UUID): ResponseEntity<Any> {
        val wallet = walletService.getWalletById(walletId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Wallet not found"))
        val user = userRepository.findById(wallet.userId)
        return if (user.isPresent) {
            val userData = UserData(
                username = user.get().username,
                id = user.get().id!!,
                email = user.get().email
            )
            ResponseEntity.ok(userData)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))
        }
    }
} 