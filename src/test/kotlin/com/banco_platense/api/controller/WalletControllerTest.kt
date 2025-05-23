package com.banco_platense.api.controller

import com.banco_platense.api.config.JwtUtil
import com.banco_platense.api.config.TestJacksonConfig
import com.banco_platense.api.config.TestSecurityConfig
import com.banco_platense.api.dto.CreateTransactionDto
import com.banco_platense.api.dto.TransactionResponseDto
import com.banco_platense.api.dto.WalletResponseDto
import com.banco_platense.api.entity.TransactionType
import com.banco_platense.api.service.WalletService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@WebMvcTest(WalletController::class)
@Import(TestSecurityConfig::class, TestJacksonConfig::class)
class WalletControllerTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @MockBean
    private lateinit var walletService: WalletService
    
    @MockBean
    private lateinit var jwtUtil: JwtUtil

    private lateinit var mockMvc: MockMvc
    private lateinit var objectMapper: ObjectMapper
    private lateinit var mockJwtToken: String

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .build()
        objectMapper = ObjectMapper().apply {
            registerModule(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
        
        // Setup mock JWT token
        val userDetails = User(
            "testuser", 
            "password", 
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        mockJwtToken = "mock-jwt-token"
        whenever(jwtUtil.extractUsername(mockJwtToken)).thenReturn("testuser")
        whenever(jwtUtil.validateToken(mockJwtToken, userDetails)).thenReturn(true)
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

        whenever(walletService.getWalletByUserId(userId)).thenReturn(wallet)

        // When and then
        mockMvc.perform(get("/api/v1/wallets/user/$userId")
            .header("Authorization", "Bearer $mockJwtToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(wallet.id))
            .andExpect(jsonPath("$.userId").value(wallet.userId))
            .andExpect(jsonPath("$.balance").value(wallet.balance))
    }

    @Test
    fun `should return not found when wallet does not exist`() {
        // Given
        val userId = 999L

        whenever(walletService.getWalletByUserId(userId))
            .thenThrow(NoSuchElementException("Wallet not found for user ID: $userId"))

        // When and Then
        mockMvc.perform(get("/api/v1/wallets/user/$userId")
            .header("Authorization", "Bearer $mockJwtToken"))
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

        whenever(walletService.createTransaction(walletId, createTransactionDto))
            .thenReturn(transactionResponse)

        // When and then
        mockMvc.perform(
            post("/api/v1/wallets/$walletId/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createTransactionDto))
                .header("Authorization", "Bearer $mockJwtToken")
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

        whenever(walletService.getTransactionsByWalletId(walletId)).thenReturn(transactions)

        // When and then
        mockMvc.perform(get("/api/v1/wallets/$walletId/transactions")
            .header("Authorization", "Bearer $mockJwtToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(transactions[0].id))
            .andExpect(jsonPath("$[0].type").value(transactions[0].type.toString()))
            .andExpect(jsonPath("$[0].amount").value(transactions[0].amount))
            .andExpect(jsonPath("$[1].id").value(transactions[1].id))
            .andExpect(jsonPath("$[1].type").value(transactions[1].type.toString()))
            .andExpect(jsonPath("$[1].amount").value(transactions[1].amount))
    }
} 