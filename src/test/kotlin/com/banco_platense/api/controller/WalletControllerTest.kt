package com.banco_platense.api.controller

import com.banco_platense.api.config.TestJacksonConfig
import com.banco_platense.api.config.TestSecurityConfig
import com.banco_platense.api.dto.CreateTransactionDto
import com.banco_platense.api.dto.TransactionResponseDto
import com.banco_platense.api.dto.WalletResponseDto
import com.banco_platense.api.entity.TransactionType
import com.banco_platense.api.entity.User
import com.banco_platense.api.repository.UserRepository
import com.banco_platense.api.service.WalletService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID
import com.banco_platense.api.dto.P2PTransactionRequestDto
import com.banco_platense.api.dto.ExternalTopUpRequestDto
import com.banco_platense.api.dto.ExternalDebinRequestDto

@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [WalletController::class], excludeFilters = [org.springframework.context.annotation.ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE, classes = [com.banco_platense.api.config.JwtAuthenticationFilter::class])])
@Import(TestSecurityConfig::class, TestJacksonConfig::class, ExceptionHandler::class)
class WalletControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var walletService: WalletService
    
    @MockitoBean
    private lateinit var userRepository: UserRepository
    
    private var mockJwtToken: String = "mock-jwt-token"
    private val userId = UUID.randomUUID()
    private val walletId = UUID.randomUUID()
    private val testUser = User(id = userId, username = "testuser", email = "test@example.com", passwordHash = "hashedpw")
    private val testWallet = WalletResponseDto(
        id = walletId,
        userId = userId,
        balance = 100.0,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @BeforeEach
    fun setup() {
        // Mock user repository response
        whenever(userRepository.findByUsername("testuser")).thenReturn(testUser)
        
        // Setup mock wallet service response
        whenever(walletService.getWalletByUserId(testUser.id!!)).thenReturn(testWallet)
    }

    @WithMockUser(username = "testuser")
    @Test
    fun `should get wallet by user id`() {
        // Given
        val wallet = WalletResponseDto(
            id = walletId,
            userId = userId,
            balance = 100.0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(walletService.getWalletByUserId(userId)).thenReturn(wallet)

        // When and then
        mockMvc.perform(get("/wallets/user")
            .header("Authorization", "Bearer $mockJwtToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testWallet.id.toString()))
            .andExpect(jsonPath("$.userId").value(testWallet.userId.toString()))
            .andExpect(jsonPath("$.balance").value(testWallet.balance))
    }

    @Test
    @WithMockUser(username = "testuser")
    fun `should create P2P transaction successfully`() {
        // Given
        whenever(userRepository.findByUsername("testuser")).thenReturn(testUser)
        whenever(walletService.getWalletByUserId(testUser.id!!)).thenReturn(testWallet)
        
        val requestDto = P2PTransactionRequestDto(
            amount = 30.0,
            description = "Money transfer to friend",
            receiverWalletId = UUID.randomUUID()
        )
        val expectedDto = CreateTransactionDto(
            type = TransactionType.P2P,
            amount = requestDto.amount,
            description = requestDto.description,
            receiverWalletId = requestDto.receiverWalletId
        )
        val transactionId = UUID.randomUUID()
        val transactionResponse = TransactionResponseDto(
            id = transactionId,
            type = TransactionType.P2P,
            amount = requestDto.amount,
            timestamp = LocalDateTime.now(),
            description = requestDto.description,
            senderWalletId = testWallet.id,
            receiverWalletId = requestDto.receiverWalletId,
            externalWalletInfo = null
        )

        whenever(walletService.createTransaction(testWallet.id!!, expectedDto))
            .thenReturn(transactionResponse)

        // When & then
        mockMvc.perform(
            post("/wallets/transactions/p2p")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .header("Authorization", "Bearer $mockJwtToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(transactionResponse.id.toString()))
            .andExpect(jsonPath("$.type").value(transactionResponse.type.toString()))
            .andExpect(jsonPath("$.amount").value(transactionResponse.amount))
            .andExpect(jsonPath("$.senderWalletId").value(transactionResponse.senderWalletId.toString()))
            .andExpect(jsonPath("$.receiverWalletId").value(transactionResponse.receiverWalletId.toString()))
    }

    @Test
    @WithMockUser(username = "testuser")
    fun `should create external topup transaction successfully`() {
        // Given
        whenever(userRepository.findByUsername("testuser")).thenReturn(testUser)
        whenever(walletService.getWalletByUserId(testUser.id!!)).thenReturn(testWallet)
        
        val requestDto = ExternalTopUpRequestDto(
            amount = 50.0,
            description = "Top up from bank account",
            externalWalletInfo = "Bank Account 123456"
        )
        val expectedDto = CreateTransactionDto(
            type = TransactionType.EXTERNAL_TOPUP,
            amount = requestDto.amount,
            description = requestDto.description,
            externalWalletInfo = requestDto.externalWalletInfo
        )
        val transactionId = UUID.randomUUID()
        val transactionResponse = TransactionResponseDto(
            id = transactionId,
            type = TransactionType.EXTERNAL_TOPUP,
            amount = requestDto.amount,
            timestamp = LocalDateTime.now(),
            description = requestDto.description,
            senderWalletId = null,
            receiverWalletId = testWallet.id,
            externalWalletInfo = requestDto.externalWalletInfo
        )

        whenever(walletService.createTransaction(testWallet.id!!, expectedDto))
            .thenReturn(transactionResponse)

        // When & then
        mockMvc.perform(
            post("/wallets/transactions/topup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .header("Authorization", "Bearer $mockJwtToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(transactionResponse.id.toString()))
            .andExpect(jsonPath("$.type").value(transactionResponse.type.toString()))
            .andExpect(jsonPath("$.amount").value(transactionResponse.amount))
            .andExpect(jsonPath("$.receiverWalletId").value(transactionResponse.receiverWalletId.toString()))
    }

    @Test
    @WithMockUser(username = "testuser")
    fun `should create external debit transaction successfully`() {
        // Given
        whenever(userRepository.findByUsername("testuser")).thenReturn(testUser)
        whenever(walletService.getWalletByUserId(testUser.id!!)).thenReturn(testWallet)
        
        val requestDto = ExternalDebinRequestDto(
            amount = 40.0,
            description = "Payment for services",
            externalWalletInfo = "Merchant XYZ"
        )
        val expectedDto = CreateTransactionDto(
            type = TransactionType.EXTERNAL_DEBIN,
            amount = requestDto.amount,
            description = requestDto.description,
            externalWalletInfo = requestDto.externalWalletInfo
        )
        val transactionId = UUID.randomUUID()
        val transactionResponse = TransactionResponseDto(
            id = transactionId,
            type = TransactionType.EXTERNAL_DEBIN,
            amount = requestDto.amount,
            timestamp = LocalDateTime.now(),
            description = requestDto.description,
            senderWalletId = testWallet.id,
            receiverWalletId = null,
            externalWalletInfo = requestDto.externalWalletInfo
        )

        whenever(walletService.createTransaction(testWallet.id!!, expectedDto))
            .thenReturn(transactionResponse)

        // When & then
        mockMvc.perform(
            post("/wallets/transactions/debin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .header("Authorization", "Bearer $mockJwtToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(transactionResponse.id.toString()))
            .andExpect(jsonPath("$.type").value(transactionResponse.type.toString()))
            .andExpect(jsonPath("$.amount").value(transactionResponse.amount))
            .andExpect(jsonPath("$.senderWalletId").value(transactionResponse.senderWalletId.toString()))
    }

    @Test
    @WithMockUser(username = "testuser")
    fun `should return all transactions for my wallet`() {
        // Given
        val transaction1Id = UUID.randomUUID()
        val transaction2Id = UUID.randomUUID()
        
        val transactions = listOf(
            TransactionResponseDto(
                id = transaction1Id,
                type = TransactionType.EXTERNAL_TOPUP,
                amount = 100.0,
                timestamp = LocalDateTime.now(),
                description = "Initial deposit",
                senderWalletId = null,
                receiverWalletId = testWallet.id,
                externalWalletInfo = "Bank Account"
            ),
            TransactionResponseDto(
                id = transaction2Id,
                type = TransactionType.EXTERNAL_DEBIN,
                amount = 25.0,
                timestamp = LocalDateTime.now(),
                description = "Payment for services",
                senderWalletId = testWallet.id,
                receiverWalletId = null,
                externalWalletInfo = "Merchant XYZ"
            )
        )

        whenever(walletService.getTransactionsByWalletId(testWallet.id!!)).thenReturn(transactions)

        // When and then
        mockMvc.perform(get("/wallets/transactions")
            .header("Authorization", "Bearer $mockJwtToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(transactions[0].id.toString()))
            .andExpect(jsonPath("$[0].type").value(transactions[0].type.toString()))
            .andExpect(jsonPath("$[0].amount").value(transactions[0].amount))
            .andExpect(jsonPath("$[1].id").value(transactions[1].id.toString()))
            .andExpect(jsonPath("$[1].type").value(transactions[1].type.toString()))
            .andExpect(jsonPath("$[1].amount").value(transactions[1].amount))
    }
}