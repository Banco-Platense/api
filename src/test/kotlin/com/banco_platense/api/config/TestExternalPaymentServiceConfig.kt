package com.banco_platense.api.config

import com.banco_platense.api.service.ExternalPaymentService
import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.util.UUID

@TestConfiguration
class TestExternalPaymentServiceConfig {
    
    @Bean
    @Primary
    fun mockExternalPaymentService(): ExternalPaymentService {
        val mock = Mockito.mock(ExternalPaymentService::class.java)
        
        // Mock topUp method to return a UUID
        Mockito.`when`(mock.topUp(Mockito.anyDouble(), Mockito.anyString()))
            .thenReturn(UUID.randomUUID().toString())
        
        // Mock debin method to return a UUID
        Mockito.`when`(mock.debin(Mockito.anyDouble(), Mockito.anyString()))
            .thenReturn(UUID.randomUUID().toString())
        
        return mock
    }
}
