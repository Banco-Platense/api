package com.banco_platense.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Health", description = "Health check endpoint")
class HealthController {

    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Returns the health status of the API"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API is healthy",
        content = [Content(
            mediaType = "application/json",
            examples = [ExampleObject(value = """{"status": "UP", "message": "API is running"}""")]
        )]
    )
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "message" to "API is running"
        ))
    }
} 