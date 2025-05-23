package com.banco_platense.api.controller

import com.banco_platense.api.dto.CreateTransactionDto
import com.banco_platense.api.dto.TransactionResponseDto
import com.banco_platense.api.dto.WalletResponseDto
import com.banco_platense.api.entity.TransactionType
import com.banco_platense.api.service.WalletService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class WalletControllerTest {

    @Mock
    private lateinit var walletService: WalletService

    @InjectMocks
    private lateinit var walletController: WalletController

    private lateinit var mockMvc: MockMvc
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(walletController)
            .setControllerAdvice(ExceptionHandler::class.java)
            .build()
            
        objectMapper = ObjectMapper().apply {
            registerModule(JavaTimeModule())
            disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    @Test
    fun `should get wallet by user id`() {
        // Given
        val userId = 1L
        val wallet = WalletResponseDto(
            id = 1L,
            userId = userId,
            balance = 100.0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        `when`(walletService.getWalletByUserId(userId)).thenReturn(wallet)

        // When and then
        mockMvc.perform(get("/api/v1/wallets/user/$userId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(wallet.id))
            .andExpect(jsonPath("$.userId").value(wallet.userId))
            .andExpect(jsonPath("$.balance").value(wallet.balance))
    }

    @Test
    fun `should return not found when wallet does not exist`() {
        // Given
        val userId = 999L
        
        `when`(walletService.getWalletByUserId(userId))
            .thenThrow(NoSuchElementException("Wallet not found for user ID: $userId"))

        // When and Then
        mockMvc.perform(get("/api/v1/wallets/user/$userId"))
            .andExpect(status().isNotFound)
    }
    
    @Test
    fun `should create transaction successfully`() {
        // Given
        val walletId = 1L
        val createTransactionDto = CreateTransactionDto(
            type = TransactionType.EXTERNAL_TOPUP,
            amount = 50.0,
            description = "Top up from bank account",
            senderWalletId = null,
            receiverWalletId = walletId,
            externalWalletInfo = "Bank Account 123456"
        )
        
        val transactionResponse = TransactionResponseDto(
            id = 1L,
            type = TransactionType.EXTERNAL_TOPUP,
            amount = 50.0,
            timestamp = LocalDateTime.now(),
            description = "Top up from bank account",
            senderWalletId = null,
            receiverWalletId = walletId,
            externalWalletInfo = "Bank Account 123456"
        )
        
        `when`(walletService.createTransaction(walletId, createTransactionDto))
            .thenReturn(transactionResponse)

        // When and then
        mockMvc.perform(
            post("/api/v1/wallets/$walletId/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createTransactionDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(transactionResponse.id))
            .andExpect(jsonPath("$.type").value(transactionResponse.type.toString()))
            .andExpect(jsonPath("$.amount").value(transactionResponse.amount))
            .andExpect(jsonPath("$.description").value(transactionResponse.description))
    }
    
    @Test
    fun `should return all transactions for a wallet`() {
        // Given
        val walletId = 1L
        val transactions = listOf(
            TransactionResponseDto(
                id = 1L,
                type = TransactionType.EXTERNAL_TOPUP,
                amount = 100.0,
                timestamp = LocalDateTime.now(),
                description = "Initial deposit",
                senderWalletId = null,
                receiverWalletId = walletId,
                externalWalletInfo = "Bank Account"
            ),
            TransactionResponseDto(
                id = 2L,
                type = TransactionType.EXTERNAL_DEBIT,
                amount = 25.0,
                timestamp = LocalDateTime.now(),
                description = "Payment for services",
                senderWalletId = walletId,
                receiverWalletId = null,
                externalWalletInfo = "Merchant XYZ"
            )
        )
        
        `when`(walletService.getTransactionsByWalletId(walletId)).thenReturn(transactions)

        // When and then
        mockMvc.perform(get("/api/v1/wallets/$walletId/transactions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(transactions[0].id))
            .andExpect(jsonPath("$[0].type").value(transactions[0].type.toString()))
            .andExpect(jsonPath("$[0].amount").value(transactions[0].amount))
            .andExpect(jsonPath("$[1].id").value(transactions[1].id))
            .andExpect(jsonPath("$[1].type").value(transactions[1].type.toString()))
            .andExpect(jsonPath("$[1].amount").value(transactions[1].amount))
    }
} 