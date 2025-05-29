package com.banco_platense.api.entity

import jakarta.persistence.*
import org.hibernate.annotations.GenericGenerator
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "transactions")
data class Transaction(
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    val id: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: TransactionType,

    @Column(nullable = false)
    val amount: Double,

    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val description: String,

    @Column(name = "sender_wallet_id")
    val senderWalletId: UUID? = null,

    @Column(name = "receiver_wallet_id")
    val receiverWalletId: UUID? = null,

    @Column(name = "external_wallet_info")
    val externalWalletInfo: String? = null
) 