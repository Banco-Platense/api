package com.banco_platense.api.service

import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.beans.factory.annotation.Value

@Service
class ExternalPaymentService(
    @Value("\${external.service.url}") private val externalServiceUrl: String,
) {
    private val restTemplate = RestTemplate()

    fun topUp(amount: Double, externalWalletInfo: String): String {
        val url = "$externalServiceUrl/top-up"
        val request = DebinRequest(walletId = externalWalletInfo, amount = amount)
        val headers = HttpHeaders().also { it.contentType = MediaType.APPLICATION_JSON }
        val entity = HttpEntity(request, headers)
        val response: ResponseEntity<DebinResponse> = restTemplate.postForEntity(url, entity, DebinResponse::class.java)
        if (!response.statusCode.is2xxSuccessful) {
            val errorMsg = response.body?.message ?: "External top-up failed with status ${'$'}{response.statusCode}"
            throw RuntimeException(errorMsg)
        }
        return externalWalletInfo
    }

    fun debin(amount: Double, externalWalletInfo: String): String {
        val url = "$externalServiceUrl/debin/request"
        val request = DebinRequest(walletId = externalWalletInfo, amount = amount)
        val headers = HttpHeaders().also { it.contentType = MediaType.APPLICATION_JSON }
        val entity = HttpEntity(request, headers)
        val response: ResponseEntity<DebinResponse> = restTemplate.postForEntity(url, entity, DebinResponse::class.java)
        if (!response.statusCode.is2xxSuccessful) {
            val errorMsg = response.body?.message ?: "External DEBIN failed with status ${'$'}{response.statusCode}"
            throw RuntimeException(errorMsg)
        }
        return externalWalletInfo
    }

    private data class DebinRequest(val walletId: String, val amount: Double)
    private data class DebinResponse(val status: String, val message: String)
} 