package com.banco_platense.api.repository

import com.banco_platense.api.entity.Wallet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface WalletRepository : JpaRepository<Wallet, UUID> {
    fun findByUserId(userId: UUID): Wallet?
} 