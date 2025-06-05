package com.banco_platense.api.service

import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.beans.factory.annotation.Value
import java.util.UUID

@Service
class ExternalPaymentService(
    @Value("${external.mock.url}") private val mockBaseUrl: String
) {
    private val restTemplate = RestTemplate()

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
        // Call external mock for DEBIN request
        val url = "$mockBaseUrl/debin/request"
        val request = DebinMockRequest(walletId = externalWalletInfo, amount = amount)
        val headers = HttpHeaders().also { it.contentType = MediaType.APPLICATION_JSON }
        val entity = HttpEntity(request, headers)
        val response: ResponseEntity<DebinMockResponse> = restTemplate.postForEntity(url, entity, DebinMockResponse::class.java)
        if (!response.statusCode.is2xxSuccessful) {
            val errorMsg = response.body?.message ?: "External DEBIN mock failed with status ${'$'}{response.statusCode}"
            throw RuntimeException(errorMsg)
        }
        // On success, generate a transaction ID
        val externalTransactionId = UUID.randomUUID().toString()
        println("Simulated external DEBIN via mock: amount=${'$'}amount, source=${'$'}externalWalletInfo, externalTransactionId=${'$'}externalTransactionId")
        return externalTransactionId
    }

    // DTOs for external mock interaction
    private data class DebinMockRequest(val walletId: String, val amount: Double)
    private data class DebinMockResponse(val status: String, val message: String)
} 