package com.banco_platense.api.repository

import com.banco_platense.api.entity.Wallet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WalletRepository : JpaRepository<Wallet, Long> {
    fun findByUserId(userId: Long): Wallet?
} 