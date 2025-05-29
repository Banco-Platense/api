package com.banco_platense.api.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestJacksonConfig {
    
    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(JavaTimeModule())
            registerKotlinModule()
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
} 