package com.banco_platense.api.integration

import com.banco_platense.api.ApiApplication
import com.banco_platense.api.config.TestApplicationConfig
import com.banco_platense.api.config.TestSecurityConfig
import com.banco_platense.api.dto.P2PTransactionRequestDto
import com.banco_platense.api.dto.ExternalTopUpRequestDto
import com.banco_platense.api.dto.ExternalDebinRequestDto
import com.banco_platense.api.entity.Drink
import com.banco_platense.api.entity.Transaction
import com.banco_platense.api.entity.TransactionType
import com.banco_platense.api.entity.User
import com.banco_platense.api.entity.Wallet
import com.banco_platense.api.repository.TransactionRepository
import com.banco_platense.api.repository.UserRepository
import com.banco_platense.api.repository.WalletRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDateTime

@SpringBootTest(
    classes = [
        ApiApplication::class,
        TestSecurityConfig::class,
        TestApplicationConfig::class
    ],
    properties = ["spring.main.allow-bean-definition-overriding=true"]
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WalletIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var walletRepository: WalletRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var testUser: User
    private lateinit var testWallet: Wallet
    private lateinit var otherUser: User
    private lateinit var otherWallet: Wallet

    @BeforeEach
    fun setup() {
        // Clean up repositories
        transactionRepository.deleteAll()
        walletRepository.deleteAll()
        userRepository.deleteAll()
        
        // Configure MockMvc with security
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()
        
        // Set up test users
        testUser = userRepository.save(
            User(
                email = "testuser@example.com",
                username = "testuser",
                passwordHash = "hashedpassword",
                drinks = Drink.COFFEE
            )
        )
        
        otherUser = userRepository.save(
            User(
                email = "otheruser@example.com",
                username = "otheruser",
                passwordHash = "hashedpassword",
                drinks = Drink.COFFEE
            )
        )
        
        // Set up test wallets
        testWallet = walletRepository.save(
            Wallet(
                userId = testUser.id!!,
                balance = 100.0,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )
        
        otherWallet = walletRepository.save(
            Wallet(
                userId = otherUser.id!!,
                balance = 50.0,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )
        
        if (objectMapper.registeredModuleIds.none { it.toString().contains("JavaTimeModule") }) {
            objectMapper.registerModule(JavaTimeModule())
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    @Test
    @WithMockUser(username = "testuser")
    fun `should get my wallet`() {
        // When & then
        mockMvc.perform(get("/wallets/user"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testWallet.id.toString()))
            .andExpect(jsonPath("$.userId").value(testWallet.userId.toString()))
            .andExpect(jsonPath("$.balance").value(testWallet.balance))
    }

    @Test
    @WithMockUser(username = "testuser")
    fun `should create external topup transaction and update wallet balance`() {
        // given
        val initialBalance = testWallet.balance
        val topupAmount = 50.0
        
        val requestDto = ExternalTopUpRequestDto(
            amount = topupAmount,
            description = "Top up from bank account",
            externalWalletInfo = "Bank Account 123456"
        )

        // when
        mockMvc.perform(
            post("/wallets/transactions/topup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value(TransactionType.EXTERNAL_TOPUP.toString()))
            .andExpect(jsonPath("$.amount").value(topupAmount))
            .andExpect(jsonPath("$.receiverWalletId").value(testWallet.id.toString()))

        // Then
        val updatedWallet = walletRepository.findById(testWallet.id!!).orElseThrow()
        assertEquals(initialBalance + topupAmount, updatedWallet.balance)
        
        // Verify transaction was saved
        val transactions = transactionRepository.findByReceiverWalletId(testWallet.id!!)
        assertEquals(1, transactions.size)
        assertEquals(TransactionType.EXTERNAL_TOPUP, transactions[0].type)
        assertEquals(topupAmount, transactions[0].amount)
    }

    @Test
    @WithMockUser(username = "testuser")
    fun `should create external debit transaction and update wallet balance`() {
        // Given
        val initialBalance = testWallet.balance
        val debitAmount = 30.0

        val requestDto = ExternalDebinRequestDto(
            amount = debitAmount,
            description = "Payment for services",
            externalWalletInfo = "Merchant XYZ"
        )

        // when
        mockMvc.perform(
            post("/wallets/transactions/debin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value(TransactionType.EXTERNAL_DEBIN.toString()))
            .andExpect(jsonPath("$.amount").value(debitAmount))
            .andExpect(jsonPath("$.senderWalletId").value(testWallet.id.toString()))

        // Then
        val updatedWallet = walletRepository.findById(testWallet.id!!).orElseThrow()
        assertEquals(initialBalance - debitAmount, updatedWallet.balance)

        // Verify
        val transactions = transactionRepository.findBySenderWalletId(testWallet.id!!)
        assertEquals(1, transactions.size)
        assertEquals(TransactionType.EXTERNAL_DEBIN, transactions[0].type)
        assertEquals(debitAmount, transactions[0].amount)
    }

    @Test
    @WithMockUser(username = "testuser")
    fun `should create P2P transaction and update both wallets`() {
        // Given
        val initialSenderBalance = testWallet.balance
        val initialReceiverBalance = otherWallet.balance
        val transferAmount = 30.0
        
        val requestDto = P2PTransactionRequestDto(
            amount = transferAmount,
            description = "Money transfer to friend",
            receiverWalletId = otherWallet.id!!
        )

        // when
        mockMvc.perform(
            post("/wallets/transactions/p2p")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value(TransactionType.P2P.toString()))
            .andExpect(jsonPath("$.amount").value(transferAmount))
            .andExpect(jsonPath("$.senderWalletId").value(testWallet.id.toString()))
            .andExpect(jsonPath("$.receiverWalletId").value(otherWallet.id.toString()))

        // Then
        val updatedSenderWallet = walletRepository.findById(testWallet.id!!).orElseThrow()
        val updatedReceiverWallet = walletRepository.findById(otherWallet.id!!).orElseThrow()
        assertEquals(initialSenderBalance - transferAmount, updatedSenderWallet.balance)
        assertEquals(initialReceiverBalance + transferAmount, updatedReceiverWallet.balance)
        
        // Verify transaction was saved
        val transactions = transactionRepository.findBySenderWalletId(testWallet.id!!)
        assertEquals(1, transactions.size)
        assertEquals(TransactionType.P2P, transactions[0].type)
        assertEquals(transferAmount, transactions[0].amount)
    }

    @Test
    @WithMockUser(username = "testuser")
    fun `should return all transactions for my wallet regardless of order`() {
        // Given
        val sentTransaction = transactionRepository.save(
            Transaction(
                type = TransactionType.EXTERNAL_DEBIN,
                amount = 20.0,
                description = "Sent payment",
                senderWalletId = testWallet.id,
                receiverWalletId = null,
                externalWalletInfo = "Merchant ABC",
            )
        )

        val receivedTransaction = transactionRepository.save(
            Transaction(
                type = TransactionType.EXTERNAL_TOPUP,
                amount = 50.0,
                description = "Received top up",
                senderWalletId = null,
                receiverWalletId = testWallet.id,
                externalWalletInfo = "Bank Account",
            )
        )

        // When and then
        mockMvc.perform(get("/wallets/transactions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isNotEmpty())
            .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.containsInAnyOrder(
                sentTransaction.id.toString(), 
                receivedTransaction.id.toString()
            )))
            .andExpect(jsonPath("$[*].type").value(org.hamcrest.Matchers.containsInAnyOrder(
                sentTransaction.type.toString(), 
                receivedTransaction.type.toString()
            )))
            .andExpect(jsonPath("$[*].amount").value(org.hamcrest.Matchers.containsInAnyOrder(
                sentTransaction.amount, 
                receivedTransaction.amount
            )))
            .andExpect(jsonPath("$[*].description").value(org.hamcrest.Matchers.containsInAnyOrder(
                sentTransaction.description, 
                receivedTransaction.description
            )))
    }
    
    @Test
    @WithMockUser(username = "testuser")
    fun `should reject transaction with insufficient funds`() {
        // Given
        val excessiveAmount = 500.0

        val requestDto = ExternalDebinRequestDto(
            amount = excessiveAmount,
            description = "Payment that should fail",
            externalWalletInfo = "Merchant XYZ"
        )

        // When & then
        mockMvc.perform(
            post("/wallets/transactions/debin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isBadRequest)
    }
} 