package com.banco_platense.api.integration

import com.banco_platense.api.ApiApplication
import com.banco_platense.api.config.TestApplicationConfig
import com.banco_platense.api.config.TestSecurityConfig
import com.banco_platense.api.dto.CreateTransactionDto
import com.banco_platense.api.entity.Transaction
import com.banco_platense.api.entity.TransactionType
import com.banco_platense.api.entity.Wallet
import com.banco_platense.api.repository.TransactionRepository
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
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
    private lateinit var walletRepository: WalletRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var testWallet: Wallet

    @BeforeEach
    fun setup() {
        transactionRepository.deleteAll()
        walletRepository.deleteAll()
        
        testWallet = walletRepository.save(
            Wallet(
                userId = 1L,
                balance = 100.0,
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
    fun `should get wallet by user id`() {
        // When & then
        mockMvc.perform(get("/api/v1/wallets/user/${testWallet.userId}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testWallet.id))
            .andExpect(jsonPath("$.userId").value(testWallet.userId))
            .andExpect(jsonPath("$.balance").value(testWallet.balance))
    }

    @Test
    fun `should create external topup transaction and update wallet balance`() {
        // given
        val initialBalance = testWallet.balance
        val topupAmount = 50.0
        
        val createTransactionDto = CreateTransactionDto(
            type = TransactionType.EXTERNAL_TOPUP,
            amount = topupAmount,
            description = "Top up from bank account",
            senderWalletId = null,
            receiverWalletId = testWallet.id,
            externalWalletInfo = "Bank Account 123456"
        )

        // when
        mockMvc.perform(
            post("/api/v1/wallets/${testWallet.id}/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createTransactionDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value(TransactionType.EXTERNAL_TOPUP.toString()))
            .andExpect(jsonPath("$.amount").value(topupAmount))
            .andExpect(jsonPath("$.receiverWalletId").value(testWallet.id))

        // Then
        val updatedWallet = walletRepository.findById(testWallet.id).orElseThrow()
        assertEquals(initialBalance + topupAmount, updatedWallet.balance)
        
        // Make sure transaction was saved rAwr xd uwu
        val transactions = transactionRepository.findByReceiverWalletId(testWallet.id)
        assertEquals(1, transactions.size)
        assertEquals(TransactionType.EXTERNAL_TOPUP, transactions[0].type)
        assertEquals(topupAmount, transactions[0].amount)
    }

    @Test
    fun `should create external debit transaction and update wallet balance`() {
        // Given
        val initialBalance = testWallet.balance
        val debitAmount = 30.0
        
        val createTransactionDto = CreateTransactionDto(
            type = TransactionType.EXTERNAL_DEBIT,
            amount = debitAmount,
            description = "Payment for services",
            senderWalletId = testWallet.id,
            receiverWalletId = null,
            externalWalletInfo = "Merchant XYZ"
        )

        // when
        mockMvc.perform(
            post("/api/v1/wallets/${testWallet.id}/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createTransactionDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value(TransactionType.EXTERNAL_DEBIT.toString()))
            .andExpect(jsonPath("$.amount").value(debitAmount))
            .andExpect(jsonPath("$.senderWalletId").value(testWallet.id))

        // Then
        val updatedWallet = walletRepository.findById(testWallet.id).orElseThrow()
        assertEquals(initialBalance - debitAmount, updatedWallet.balance)
        
        // Verify
        val transactions = transactionRepository.findBySenderWalletId(testWallet.id)
        assertEquals(1, transactions.size)
        assertEquals(TransactionType.EXTERNAL_DEBIT, transactions[0].type)
        assertEquals(debitAmount, transactions[0].amount)
    }

    @Test
    fun `should return all transactions for a wallet`() {
        // Given
        val sentTransaction = transactionRepository.save(
            Transaction(
                type = TransactionType.EXTERNAL_DEBIT,
                amount = 20.0,
                description = "Sent payment",
                senderWalletId = testWallet.id,
                receiverWalletId = null,
                externalWalletInfo = "Merchant ABC"
            )
        )
        
        val receivedTransaction = transactionRepository.save(
            Transaction(
                type = TransactionType.EXTERNAL_TOPUP,
                amount = 50.0,
                description = "Received top up",
                senderWalletId = null,
                receiverWalletId = testWallet.id,
                externalWalletInfo = "Bank Account"
            )
        )

        // When and then
        mockMvc.perform(get("/api/v1/wallets/${testWallet.id}/transactions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").isNumber())
            .andExpect(jsonPath("$[1].id").isNumber())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isNotEmpty())
    }
    
    @Test
    fun `should reject transaction with insufficient funds`() {
        // Given
        val excessiveAmount = 500.0
        
        val createTransactionDto = CreateTransactionDto(
            type = TransactionType.EXTERNAL_DEBIT,
            amount = excessiveAmount,
            description = "Payment that should fail",
            senderWalletId = testWallet.id,
            receiverWalletId = null,
            externalWalletInfo = "Merchant XYZ"
        )

        // When & then
        mockMvc.perform(
            post("/api/v1/wallets/${testWallet.id}/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createTransactionDto))
        )
            .andExpect(status().isBadRequest)
    }
} 