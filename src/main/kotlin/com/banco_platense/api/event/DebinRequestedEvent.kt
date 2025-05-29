package com.banco_platense.api.event

import java.util.UUID

/**
 * Event published when a Debin request is created.
 */
data class DebinRequestedEvent(
    val requestId: UUID
) 