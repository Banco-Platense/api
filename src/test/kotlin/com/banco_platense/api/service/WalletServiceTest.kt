package com.banco_platense.api.service

import com.banco_platense.api.ApiApplication
import com.banco_platense.api.config.TestApplicationConfig
import com.banco_platense.api.config.TestSecurityConfig
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

@SpringBootTest(
    classes = [
        ApiApplication::class,
        TestSecurityConfig::class,
        TestApplicationConfig::class
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
    }
    
    @Test
    fun `should create wallet for user`() {
        // Given
        val userId = 2L
        
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
        val nonExistentUserId = 999L
        
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
            senderWalletId = null,
            receiverWalletId = testWallet.id,
            externalWalletInfo = "Bank Account 123456"
        )
        
        // When
        val result = walletService.createTransaction(testWallet.id, createDto)
        
        // Then
        assertNotNull(result.id)
        assertEquals(TransactionType.EXTERNAL_TOPUP, result.type)
        assertEquals(topupAmount, result.amount)
        assertEquals("Top up from bank account", result.description)
        assertEquals(testWallet.id, result.receiverWalletId)
        
        // Verify balance was updated
        val updatedWallet = walletRepository.findById(testWallet.id).orElseThrow()
        assertEquals(initialBalance + topupAmount, updatedWallet.balance)
        
        // Verify transaction was saved
        val transactions = transactionRepository.findByReceiverWalletId(testWallet.id)
        assertEquals(1, transactions.size)
        assertEquals(TransactionType.EXTERNAL_TOPUP, transactions[0].type)
        assertEquals(topupAmount, transactions[0].amount)
    }
    
    @Test
    fun `should create external debit transaction`() {
        // Given
        val initialBalance = testWallet.balance
        val debitAmount = 50.0
        
        val createDto = CreateTransactionDto(
            type = TransactionType.EXTERNAL_DEBIT,
            amount = debitAmount,
            description = "Payment for services",
            senderWalletId = testWallet.id,
            receiverWalletId = null,
            externalWalletInfo = "Merchant XYZ"
        )
        
        // When
        val result = walletService.createTransaction(testWallet.id, createDto)
        
        // Then
        assertNotNull(result.id)
        assertEquals(TransactionType.EXTERNAL_DEBIT, result.type)
        assertEquals(debitAmount, result.amount)
        assertEquals("Payment for services", result.description)
        assertEquals(testWallet.id, result.senderWalletId)
        
        // Verify balance was updated
        val updatedWallet = walletRepository.findById(testWallet.id).orElseThrow()
        assertEquals(initialBalance - debitAmount, updatedWallet.balance)
        
        // Verify transaction was saved
        val transactions = transactionRepository.findBySenderWalletId(testWallet.id)
        assertEquals(1, transactions.size)
        assertEquals(TransactionType.EXTERNAL_DEBIT, transactions[0].type)
        assertEquals(debitAmount, transactions[0].amount)
    }
    
    @Test
    fun `should throw exception when insufficient funds for debit transaction`() {
        // Given
        val excessiveAmount = 200.0
        
        val createDto = CreateTransactionDto(
            type = TransactionType.EXTERNAL_DEBIT,
            amount = excessiveAmount,
            description = "Payment for services",
            senderWalletId = testWallet.id,
            receiverWalletId = null,
            externalWalletInfo = "Merchant XYZ"
        )
        
        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            walletService.createTransaction(testWallet.id, createDto)
        }
        
        assertTrue(exception.message?.contains("Insufficient funds") == true)
    }
    
    @Test
    fun `should get all transactions for a wallet`() {
        // Given
        transactionRepository.save(
            Transaction(
                type = TransactionType.EXTERNAL_DEBIT,
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
        val result = walletService.getTransactionsByWalletId(testWallet.id)
        
        // Then
        assertEquals(2, result.size)
        
        assertTrue(result.any { it.type == TransactionType.EXTERNAL_DEBIT })
        assertTrue(result.any { it.type == TransactionType.EXTERNAL_TOPUP })
    }
} 