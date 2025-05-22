package com.banco_platense.api.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "transactions")
data class Transaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

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
    val senderWalletId: Long? = null,

    @Column(name = "receiver_wallet_id")
    val receiverWalletId: Long? = null,

    @Column(name = "external_wallet_info")
    val externalWalletInfo: String? = null
) 