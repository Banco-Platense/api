package com.banco_platense.externalservicesmock

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ExternalServicesMockApplication

fun main(args: Array<String>) {
    runApplication<ExternalServicesMockApplication>(*args)
} 