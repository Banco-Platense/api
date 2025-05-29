package com.banco_platense.api.service

import com.banco_platense.api.dto.ConfirmDebinRequestDto
import com.banco_platense.api.dto.DebinRequestResponseDto
import com.banco_platense.api.dto.CreateTransactionDto
import com.banco_platense.api.entity.DebinRequest
import com.banco_platense.api.entity.DebinStatus
import com.banco_platense.api.entity.TransactionType
import com.banco_platense.api.event.DebinRequestedEvent
import com.banco_platense.api.repository.DebinRequestRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.context.event.EventListener
import java.time.LocalDateTime
import java.util.UUID
import com.banco_platense.api.service.WalletService
import org.springframework.context.annotation.Profile
import com.banco_platense.api.dto.ExternalDebinRequestDto

@Service
@Profile("!test")
class DebinService(
    private val debinRequestRepository: DebinRequestRepository,
    private val walletService: WalletService,
    private val eventPublisher: ApplicationEventPublisher
) {

    @Transactional
    fun createDebinRequest(walletId: UUID, requestDto: ExternalDebinRequestDto): DebinRequestResponseDto {
        val debinRequest = DebinRequest(
            walletId = walletId,
            amount = requestDto.amount,
            description = requestDto.description,
            externalWalletInfo = requestDto.externalWalletInfo,
            status = DebinStatus.PENDING
        )
        val saved = debinRequestRepository.save(debinRequest)
        eventPublisher.publishEvent(DebinRequestedEvent(saved.id!!))
        return mapToDto(saved)
    }

    @Transactional
    fun confirmDebinRequest(requestId: UUID, statusDto: ConfirmDebinRequestDto): DebinRequestResponseDto {
        val request = debinRequestRepository.findById(requestId)
            .orElseThrow { NoSuchElementException("Debin request not found: $requestId") }
        request.status = statusDto.status
        request.updatedAt = LocalDateTime.now()
        val updated = debinRequestRepository.save(request)
        if (updated.status == DebinStatus.ACCEPTED) {
            val createDto = CreateTransactionDto(
                type = TransactionType.EXTERNAL_DEBIT,
                amount = updated.amount,
                description = updated.description,
                externalWalletInfo = updated.externalWalletInfo
            )
            walletService.createTransaction(updated.walletId, createDto)
        }
        return mapToDto(updated)
    }

    @Async
    @EventListener
    fun handleDebinRequestedEvent(event: DebinRequestedEvent) {
        // Simulate delay for external service response
        Thread.sleep(5000)
        // Always accept Debin requests
        val status = DebinStatus.ACCEPTED
        // Confirm the request
        confirmDebinRequest(event.requestId, ConfirmDebinRequestDto(status))
    }

    private fun mapToDto(request: DebinRequest): DebinRequestResponseDto {
        return DebinRequestResponseDto(
            id = request.id,
            walletId = request.walletId,
            amount = request.amount,
            description = request.description,
            externalWalletInfo = request.externalWalletInfo,
            status = request.status,
            timestamp = request.timestamp,
            updatedAt = request.updatedAt
        )
    }
} 