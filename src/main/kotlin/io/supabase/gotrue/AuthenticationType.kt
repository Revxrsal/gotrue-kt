package io.supabase.gotrue

enum class AuthenticationType(val jsonName: String) {
    SIGNUP("signup"),
    MAGIC_LINK("magiclink"),
    RECOVERY("recovery"),
    INVITE("invite")
}
