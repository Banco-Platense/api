package com.banco_platense.api.repository

import com.banco_platense.api.entity.Transaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TransactionRepository : JpaRepository<Transaction, Long> {
    fun findBySenderWalletId(walletId: Long): List<Transaction>
    fun findByReceiverWalletId(walletId: Long): List<Transaction>
} 