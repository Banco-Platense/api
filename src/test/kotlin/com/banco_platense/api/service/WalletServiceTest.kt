package com.banco_platense.api.service

import com.banco_platense.api.ApiApplication
import com.banco_platense.api.config.TestApplicationConfig
import com.banco_platense.api.config.TestSecurityConfig
import com.banco_platense.api.config.TestExternalPaymentServiceConfig
import com.banco_platense.api.dto.CreateTransactionDto
import com.banco_platense.api.entity.Transaction
import com.banco_platense.api.entity.TransactionType
import com.banco_platense.api.entity.Wallet
import com.banco_platense.api.repository.TransactionRepository
import com.banco_platense.api.repository.WalletRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest(
    classes = [
        ApiApplication::class,
        TestSecurityConfig::class,
        TestApplicationConfig::class,
        TestExternalPaymentServiceConfig::class
    ],
    properties = ["spring.main.allow-bean-definition-overriding=true"]
)
@ActiveProfiles("test")
@Transactional
class WalletServiceTest {

    @Autowired
    private lateinit var walletRepository: WalletRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var walletService: WalletService

    private lateinit var testWallet: Wallet
    private lateinit var recipientWallet: Wallet
    
    @BeforeEach
    fun setup() {
        transactionRepository.deleteAll()
        walletRepository.deleteAll()
        
        testWallet = walletRepository.save(
            Wallet(
                userId = UUID.randomUUID(),
                balance = 100.0,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )
        
        recipientWallet = walletRepository.save(
            Wallet(
                userId = UUID.randomUUID(),
                balance = 50.0,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )
    }
    
    @Test
    fun `should create wallet for user`() {
        // Given
        val userId = UUID.randomUUID()
        
        // When
        val result = walletService.createWallet(userId)
        
        // Then
        assertNotNull(result.id)
        assertEquals(userId, result.userId)
        assertEquals(0.0, result.balance)
        
        // Verify in database
        val walletInDb = walletRepository.findByUserId(userId)
        assertNotNull(walletInDb)
        assertEquals(userId, walletInDb?.userId)
    }
    
    @Test
    fun `should get wallet by user id`() {
        // When
        val result = walletService.getWalletByUserId(testWallet.userId)
        
        // Then
        assertEquals(testWallet.id, result.id)
        assertEquals(testWallet.userId, result.userId)
        assertEquals(testWallet.balance, result.balance)
    }
    
    @Test
    fun `should throw exception when wallet not found by user id`() {
        // Given
        val nonExistentUserId = UUID.randomUUID()
        
        // When & Then
        val exception = assertThrows(NoSuchElementException::class.java) {
            walletService.getWalletByUserId(nonExistentUserId)
        }
        
        assertEquals("Wallet not found for user ID: $nonExistentUserId", exception.message)
    }
    
    @Test
    fun `should create external topup transaction`() {
        // Given
        val initialBalance = testWallet.balance
        val topupAmount = 50.0
        
        val createDto = CreateTransactionDto(
            type = TransactionType.EXTERNAL_TOPUP,
            amount = topupAmount,
            description = "Top up from bank account",
            externalWalletInfo = "Bank Account 123456"
        )
        
        // When
        val result = walletService.createTransaction(testWallet.id!!, createDto)
        
        // Then
        assertNotNull(result.id)
        assertEquals(TransactionType.EXTERNAL_TOPUP, result.type)
        assertEquals(topupAmount, result.amount)
        assertEquals("Top up from bank account", result.description)
        assertEquals(testWallet.id, result.receiverWalletId)
        assertNotNull(result.externalWalletInfo)
        assertDoesNotThrow { UUID.fromString(result.externalWalletInfo) }
        
        // Verify balance was updated
        val updatedWallet = walletRepository.findById(testWallet.id!!).orElseThrow()
        assertEquals(initialBalance + topupAmount, updatedWallet.balance)
        
        // Verify transaction was saved
        val transactions = transactionRepository.findByReceiverWalletId(testWallet.id!!)
        assertEquals(1, transactions.size)
        assertEquals(TransactionType.EXTERNAL_TOPUP, transactions[0].type)
        assertNotNull(transactions[0].externalWalletInfo)
        assertDoesNotThrow { UUID.fromString(transactions[0].externalWalletInfo) }
        assertEquals(topupAmount, transactions[0].amount)
    }
    
    @Test
    fun `should create external debin transaction`() {
        // Given
        val initialBalance = testWallet.balance
        val debinAmount = 50.0

        val createDto = CreateTransactionDto(
            type = TransactionType.EXTERNAL_DEBIN,
            amount = debinAmount,
            description = "Payment for services",
            externalWalletInfo = "Merchant XYZ"
        )

        // When
        val result = walletService.createTransaction(testWallet.id!!, createDto)

        // Then
        assertNotNull(result.id)
        assertEquals(TransactionType.EXTERNAL_DEBIN, result.type)
        assertEquals(debinAmount, result.amount)
        assertEquals("Payment for services", result.description)
        assertEquals(testWallet.id, result.receiverWalletId)
        assertNotNull(result.externalWalletInfo)
        assertDoesNotThrow { UUID.fromString(result.externalWalletInfo) }

        // Verify balance was updated
        val updatedWallet = walletRepository.findById(testWallet.id!!).orElseThrow()
        assertEquals(initialBalance + debinAmount, updatedWallet.balance)

        // Verify transaction was saved
        val transactions = transactionRepository.findByReceiverWalletId(testWallet.id!!)
        assertEquals(1, transactions.size)
        assertEquals(TransactionType.EXTERNAL_DEBIN, transactions[0].type)
        assertNotNull(transactions[0].externalWalletInfo)
        assertDoesNotThrow { UUID.fromString(transactions[0].externalWalletInfo) }
        assertEquals(debinAmount, transactions[0].amount)
    }
    
    @Test
    fun `should create P2P transaction`() {
        // Given
        val initialSenderBalance = testWallet.balance
        val initialReceiverBalance = recipientWallet.balance
        val transferAmount = 50.0
        
        val createDto = CreateTransactionDto(
            type = TransactionType.P2P,
            amount = transferAmount,
            description = "Money transfer to friend",
            receiverWalletId = recipientWallet.id
        )
        
        // When
        val result = walletService.createTransaction(testWallet.id!!, createDto)
        
        // Then
        assertNotNull(result.id)
        assertEquals(TransactionType.P2P, result.type)
        assertEquals(transferAmount, result.amount)
        assertEquals("Money transfer to friend", result.description)
        assertEquals(testWallet.id, result.senderWalletId)
        assertEquals(recipientWallet.id, result.receiverWalletId)
        
        // Verify sender balance was updated
        val updatedSenderWallet = walletRepository.findById(testWallet.id!!).orElseThrow()
        assertEquals(initialSenderBalance - transferAmount, updatedSenderWallet.balance)
        
        // Verify receiver balance was updated
        val updatedReceiverWallet = walletRepository.findById(recipientWallet.id!!).orElseThrow()
        assertEquals(initialReceiverBalance + transferAmount, updatedReceiverWallet.balance)
        
        // Verify transaction was saved
        val transactions = transactionRepository.findBySenderWalletId(testWallet.id!!)
        assertEquals(1, transactions.size)
        assertEquals(TransactionType.P2P, transactions[0].type)
        assertEquals(transferAmount, transactions[0].amount)
    }
    
    @Test
    fun `should throw exception when trying to send to self`() {
        // Given
        val createDto = CreateTransactionDto(
            type = TransactionType.P2P,
            amount = 50.0,
            description = "Transfer to self",
            receiverWalletId = testWallet.id
        )
        
        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            walletService.createTransaction(testWallet.id!!, createDto)
        }
        
        assertTrue(exception.message?.contains("Cannot send money to yourself") == true)
    }
    
    @Test
    fun `should get all transactions for a wallet`() {
        // Given
        transactionRepository.save(
            Transaction(
                type = TransactionType.EXTERNAL_DEBIN,
                amount = 30.0,
                description = "Payment for services",
                senderWalletId = testWallet.id,
                receiverWalletId = null,
                externalWalletInfo = "Merchant ABC"
            )
        )
        
        transactionRepository.save(
            Transaction(
                type = TransactionType.EXTERNAL_TOPUP,
                amount = 50.0,
                description = "Top up",
                senderWalletId = null,
                receiverWalletId = testWallet.id,
                externalWalletInfo = "Bank Account"
            )
        )
        
        // When
        val result = walletService.getTransactionsByWalletId(testWallet.id!!)
        
        // Then
        assertEquals(2, result.size)
        
        assertTrue(result.any { it.type == TransactionType.EXTERNAL_DEBIN })
        assertTrue(result.any { it.type == TransactionType.EXTERNAL_TOPUP })
    }
} 