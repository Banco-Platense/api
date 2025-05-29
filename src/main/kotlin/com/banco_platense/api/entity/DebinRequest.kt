package com.banco_platense.api.entity

import jakarta.persistence.*
import org.hibernate.annotations.GenericGenerator
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "debin_requests")
data class DebinRequest(
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    val id: UUID? = null,

    @Column(nullable = false)
    val walletId: UUID,

    @Column(nullable = false)
    val amount: Double,

    @Column(nullable = false)
    val description: String,

    @Column(nullable = false)
    val externalWalletInfo: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DebinStatus,

    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = timestamp
) 