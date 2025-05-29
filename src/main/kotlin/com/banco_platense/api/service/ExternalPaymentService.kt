package com.banco_platense.api.service

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ExternalPaymentService {
    /**
     * Simulates an external top-up (e.g., home banking transfer) and returns a fake external transaction ID.
     */
    fun simulateTopUp(amount: Double, externalWalletInfo: String): String {
        Thread.sleep(500)
        val externalTransactionId = UUID.randomUUID().toString()
        println("Simulated external top-up: amount=$amount, source=$externalWalletInfo, externalTransactionId=$externalTransactionId")
        return externalTransactionId
    }

    /**
     * Simulates an external DEBIN (e.g., bank account debit) and returns a fake external transaction ID.
     */
    fun simulateDebin(amount: Double, externalWalletInfo: String): String {
        Thread.sleep(500)
        val externalTransactionId = UUID.randomUUID().toString()
        println("Simulated external DEBIN: amount=$amount, source=$externalWalletInfo, externalTransactionId=$externalTransactionId")
        return externalTransactionId
    }
} 