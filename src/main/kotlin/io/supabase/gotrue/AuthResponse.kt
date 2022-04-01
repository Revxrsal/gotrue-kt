package io.supabase.gotrue

import io.supabase.gotrue.type.Session

data class AuthResponse(
    val session: Session? = null,
    val providerURL: String? = null,
)
