package com.banco_platense.api.repository

import com.banco_platense.api.entity.Transaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TransactionRepository : JpaRepository<Transaction, UUID> {
    fun findBySenderWalletId(walletId: UUID): List<Transaction>
    fun findByReceiverWalletId(walletId: UUID): List<Transaction>
} 