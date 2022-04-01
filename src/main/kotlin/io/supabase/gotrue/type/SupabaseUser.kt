/*
 * This file is part of gotrue-kt, licensed under the MIT License.
 *
 *  Copyright (c) Revxrsal <reflxction.github@gmail.com>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package io.supabase.gotrue.type

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.OffsetDateTime
import java.util.*

@JsonClass(generateAdapter = true)
data class SupabaseUser(
    val id: UUID,
    @Json(name = "app_metadata") val appMetadata: AppMetadata,
    @Json(name = "user_metadata") val userMetadata: UserMetadata,
    val aud: String,
    @Json(name = "recovery_sent_at") val recoverySentAt: OffsetDateTime? = null,
    @Json(name = "invited_at") val invitedAt: OffsetDateTime? = null,
    @Json(name = "action_link") val actionLink: String? = null,
    val email: String? = null,
    val phone: String? = null,
    @Json(name = "created_at") val createdAt: OffsetDateTime,
    @Json(name = "confirmed_at") val confirmedAt: OffsetDateTime? = null,
    @Json(name = "email_confirmed_at") val emailConfirmedAt: OffsetDateTime? = null,
    @Json(name = "phone_confirmed_at") val phoneConfirmedAt: OffsetDateTime? = null,
    @Json(name = "last_sign_in_at") val lastSignInAt: OffsetDateTime? = null,
    val role: String? = null,
    @Json(name = "updated_at") val updatedAt: OffsetDateTime? = null,
    val identities: List<UserIdentity> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AppMetadata(
    val provider: String? = null,
    val key: Map<String, Any> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class UserMetadata(
    val key: Map<String, Any> = emptyMap()
)