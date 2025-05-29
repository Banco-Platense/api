package com.banco_platense.api.repository

import com.banco_platense.api.entity.DebinRequest
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DebinRequestRepository : JpaRepository<DebinRequest, UUID> 