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
package io.supabase.gotrue

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.supabase.gotrue.json.typeOf
import io.supabase.gotrue.type.Provider
import io.supabase.gotrue.type.Session
import io.supabase.gotrue.type.SupabaseUser
import io.supabase.gotrue.type.UserAttributes
import io.supabase.gotrue.util.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.*

/**
 * Low-level implementation of the GoTrue API.
 *
 * You should use [GoTrueClient].
 */
class GoTrueApi(
    private val url: String,
    private val authorization: String,
    private val serviceRole: String? = null,
    headers: Map<String, String> = emptyMap(),
    private val client: OkHttpClient = OkHttpClient(),
    private val moshi: Moshi = MOSHI
) {

    /**
     * The default headers for this API
     */
    private val headers = buildMap {
        putAll(headers)
        put("Authorization", "Bearer $authorization")
        put("apikey", authorization)
        if (serviceRole != null)
            put("service_role", serviceRole)
    }.toHeaders()

    /**
     * Create a temporary object with all configured headers and
     * adds the Authorization token to be used on request methods
     *
     * @param jwt A valid, logged-in JWT.
     */
    private fun createRequestHeaders(jwt: String) = headers.newBuilder()
        .add("Authorization", "Bearer $jwt")
        .build()

    /**
     * Generates the relevant login URL for a third-party provider.
     *
     * @param provider One of the providers supported by GoTrue.
     * @param redirectTo A URL or mobile address to send the user to after they are confirmed.
     * @param scopes A space-separated list of scopes granted to the OAuth application.
     */
    fun getUrlForProvider(provider: Provider, redirectTo: String? = null, scopes: String? = null): String {
        val urlParams = linkedListOf("provider=${provider.name.lowercase().encodeURI()}")
        if (redirectTo != null) {
            urlParams.push("redirect_to=${redirectTo.encodeURI()}")
        }
        if (scopes != null) {
            urlParams.push("scopes=${scopes.encodeURI()}")
        }
        return "$url/authorize?${urlParams.joinToString("&")}"
    }

    /**
     * Creates a new user using their email address.
     *
     * @param email The email address of the user.
     * @param password The password of the user.
     * @param redirectTo A URL or mobile address to send the user to after they are confirmed.
     * @param data Optional user metadata.
     */
    fun signUpWithEmail(
        email: String,
        password: String,
        data: Map<String, Any>? = null,
        redirectTo: String? = null,
    ): UserOrSession {
        request("$url/signup".withRedirect(redirectTo)) {
            post(
                "email" to email,
                "password" to password,
                "data" to data
            )
        } whenResponds {
            return adapter<UserOrSession>().fromJson(it).apply { setExpiry() }
        }
    }

    /**
     * Logs in an existing user using their email address.
     *
     * @param email The email address of the user.
     * @param password The password of the user.
     * @param redirectTo A URL or mobile address to send the user to after they are confirmed.
     */
    fun signInWithEmail(
        email: String,
        password: String,
        redirectTo: String? = null
    ): Session {
        request("$url/token?grant_type=password".withRedirect(redirectTo)) {
            post(
                "email" to email,
                "password" to password,
            )
        } whenResponds {
            return adapter<Session>().fromJson(it).apply { setExpiry() }
        }
    }

    /**
     * Signs up a new user using their phone number and a password.
     *
     * @param phone The phone number of the user.
     * @param password The password of the user.
     * @param data Optional user metadata.
     */
    fun signUpWithPhone(
        phone: String,
        password: String,
        data: Map<String, Any>? = null
    ): UserOrSession {
        request("${this.url}/signup") {
            post(
                "phone" to phone,
                "password" to password,
                "data" to data
            )
        } whenResponds {
            return adapter<UserOrSession>().fromJson(it).apply { setExpiry() }
        }
    }

    /**
     * Logs in an existing user using their phone number and password.
     *
     * @param phone The phone number of the user.
     * @param password The password of the user.
     */
    fun signInWithPhone(
        phone: String,
        password: String
    ): Session {
        request("${this.url}/token?grant_type=password") {
            post(
                "phone" to phone,
                "password" to password
            )
        } whenResponds {
            return adapter<Session>().fromJson(it).apply { setExpiry() }
        }
    }

    /**
     * Sends a magic login link to an email address.
     *
     * @param email The email address of the user.
     * @param createUser If this should register the account if it doesn't already exist
     * @param redirectTo A URL or mobile address to send the user to after they are confirmed.
     */
    fun sendMagicLinkEmail(
        email: String,
        createUser: Boolean = true,
        redirectTo: String? = null,
    ): Map<String, Any> {
        request("$url/otp".withRedirect(redirectTo)) {
            post(
                "email" to email,
                "create_user" to createUser
            )
        } whenResponds {
            return adapter<Map<String, Any>>().fromJson(it)
        }
    }

    /**
     * Sends a mobile OTP via SMS. If [createUser] is true, this will
     * register the account if it doesn't already exist
     *
     * @param phone The user's phone number WITH international prefix
     * @param createUser If this should register the account if it doesn't already exist
     */
    fun sendMobileOTP(phone: String, createUser: Boolean = true): Map<String, Any> {
        request("$url/otp") {
            post(
                "phone" to phone,
                "create_user" to createUser
            )
        } whenResponds {
            return adapter<Map<String, Any>>().fromJson(it)
        }
    }

    /**
     * Removes a logged-in session.
     *
     * @param jwt A valid, logged-in JWT.
     */
    fun signOut(jwt: String) {
        request("$url/logout") {
            post("{}".toRequestBody(APPLICATION_JSON))
            headers(createRequestHeaders(jwt = jwt))
        }.send()
    }

    /**
     * Send a user supplied mobile OTP to be verified.
     *
     * @param phone The user's phone number WITH international prefix
     * @param token Token that user was sent to their mobile phone
     * @param redirectTo A URL or mobile address to send the user to after they are confirmed.
     */
    fun verifyOTP(
        phone: String? = null,
        token: String,
        redirectTo: String? = null,
        type: String = "sms"
    ): UserOrSession {
        request("$url/verify") {
            post(
                "phone" to phone,
                "token" to token,
                "type" to type,
                "redirect_to" to redirectTo
            )
        } whenResponds {
            println(it.code)
            println(it.body!!.string())
            return adapter<UserOrSession>().fromJson(it).apply { setExpiry() }
        }
    }

    /**
     * Sends an invite-link to an email address.
     *
     * @param email The email address of the user.
     * @param redirectTo A URL or mobile address to send the user to after they are confirmed.
     * @param data Optional user metadata.
     */
    fun inviteUserByEmail(
        email: String,
        redirectTo: String? = null,
        data: Map<String, Any>? = null
    ): SupabaseUser {
        request("$url/invite".withRedirect(redirectTo)) {
            post(
                "email" to email,
                "data" to data
            )
        } whenResponds {
            return adapter<SupabaseUser>().fromJson(it)
        }
    }

    /**
     * Sends a reset request to an email address.
     *
     * @param email The email address of the user.
     * @param redirectTo A URL or mobile address to send the user to after they are confirmed.
     */
    fun resetPasswordForEmail(email: String, redirectTo: String? = null): Map<String, Any> {
        request("$url/recover".withRedirect(redirectTo)) {
            post("email" to email)
        } whenResponds {
            return adapter<Map<String, Any>>().fromJson(it)
        }
    }

    /**
     * Generates a new JWT.
     *
     * @param refreshToken A valid refresh token that was returned on login.
     */
    fun refreshAccessToken(refreshToken: String): Session {
        request("$url/token?grant_type=refresh_token") {
            post("refresh_token" to refreshToken)
        } whenResponds {
            return adapter<Session>().fromJson(it).apply { setExpiry() }
        }
    }

    /**
     * Generates links to be sent via email or other.
     *
     * @param type The link type
     * @param email The user's email.
     * @param password User password. For signup only.
     * @param redirectTo A URL or mobile address to send the user to after they are confirmed.
     * @param data Optional user metadata. For signup only.
     */
    fun generateLink(
        type: AuthenticationType,
        email: String,
        password: String? = null,
        data: Map<String, Any>? = null,
        redirectTo: String? = null
    ): UserOrSession {
        request("$url/admin/generate_link") {
            post(
                "type" to type.jsonName,
                "email" to email,
                "password" to password,
                "data" to data,
                "redirect_to" to redirectTo
            )
        } whenResponds {
            return adapter<UserOrSession>().fromJson(it).apply { setExpiry() }
        }
    }

    /**
     * Creates a new user.
     *
     * This function should only be called on a server. Never expose
     * your `service_role` key in the browser.
     *
     * @param attributes The data you want to create the user with.
     */
    fun createUser(attributes: UserAttributes): SupabaseUser {
        request("$url/admin/users") {
            post(attributes.toRequestBody())
        } whenResponds {
            return adapter<SupabaseUser>().fromJson(it)
        }
    }

    /**
     * Get a list of users.
     *
     * This function should only be called on a server. Never expose
     * your `service_role` key in the browser.
     */
    fun listUsers(): List<SupabaseUser> {
        request("${this.url}/admin/users") {
            get()
        } whenResponds {
            return adapter<List<SupabaseUser>>().fromJson(it)
        }
    }

    /**
     * Gets the user details by their UUID. This will throw a [SupabaseException]
     * if no such user exists.
     *
     * @param uid The user's UUID.
     */
    fun getUserById(uid: UUID): SupabaseUser {
        request("$url/admin/users/$uid") {
            get()
        } whenResponds {
            return adapter<SupabaseUser>().fromJson(it)
        }
    }

    /**
     * Updates the user data by their UUID. This will throw a [SupabaseException]
     * if no such user exists.
     *
     * @param uid The user's UUID.
     * @param attributes The data you want to update.
     */
    fun updateUserById(uid: UUID, attributes: UserAttributes): SupabaseUser {
        request("$url/admin/users/$uid") {
            put(attributes.toRequestBody())
        } whenResponds {
            return adapter<SupabaseUser>().fromJson(it)
        }
    }

    /**
     * Delete a user. Requires a `service_role` key.
     *
     * This function should only be called on a server. Never expose
     * your `service_role` key in the browser.
     */
    fun deleteUser(uid: UUID): SupabaseUser {
        request("$url/admin/users/$uid") {
            delete("{}".toRequestBody(APPLICATION_JSON))
        } whenResponds {
            return adapter<SupabaseUser>().fromJson(it)
        }
    }

    /**
     * Gets the user details by the JWT token.
     *
     * @param jwt A valid, logged-in JWT.
     */
    fun getUser(jwt: String): SupabaseUser {
        request("$url/user") {
            headers(createRequestHeaders(jwt = jwt))
        } whenResponds {
            return adapter<SupabaseUser>().fromJson(it)
        }
    }

    /**
     * Updates the user data.
     *
     * @param jwt A valid, logged-in JWT.
     * @param attributes The data you want to update.
     */
    fun updateUser(jwt: String, attributes: UserAttributes): SupabaseUser {
        request("$url/user") {
            put(attributes.toRequestBody())
            headers(createRequestHeaders(jwt = jwt))
        } whenResponds {
            return adapter<SupabaseUser>().fromJson(it)
        }
    }

    /**
     * Generates a request and provides a friendly DSL for configuring it
     */
    private fun request(url: String, init: Request.Builder.() -> Unit): Request {
        val builder = Request.Builder()
            .headers(headers)
            .url(url)
        builder.init()
        return builder.build()
    }

    /**
     * Sends the request to the specified endpoint and provides a callback for
     * processing the response
     */
    private inline infix fun <R> Request.whenResponds(onResponse: (Response) -> R): R {
        client.newCall(this).execute().use {
            if (it.code.isAccepted) {
                return onResponse(it)
            }
            val map = adapter<Map<String, Any>>().fromJson(it)
            val message = (map["msg"] ?: map["message"] ?: map["error"] ?: map).toString()
            throw SupabaseException(message)
        }
    }

    /**
     * Sends the request to the specified endpoint
     */
    private fun Request.send() {
        client.newCall(this).execute().use {
            if (!it.code.isAccepted) {
                val map = adapter<Map<String, Any>>().fromJson(it)
                val message = (map["msg"] ?: map["message"] ?: map["error"] ?: map).toString()
                throw SupabaseException(message)
            }
        }
    }

    private inline fun <reified T> adapter(): JsonAdapter<T> = moshi.adapter(typeOf<T>())

    private inline fun <reified T> T.toRequestBody(): RequestBody {
        return adapter<T>().toJson(this).toRequestBody(APPLICATION_JSON)
    }

    private inline fun <reified K, reified V> Request.Builder.post(vararg body: Pair<K, V>) {
        val adapter: JsonAdapter<Map<K, V>> =
            moshi.adapter(Types.newParameterizedType(Map::class.java, typeOf<K>(), typeOf<V>()))
        post(adapter.toJson(body.toMap()).toRequestBody(APPLICATION_JSON))
    }

    private fun <T> JsonAdapter<T>.fromJson(it: Response): T {
        @Suppress("UNCHECKED_CAST")
        return fromJson(it.body!!.source()) as T
    }

    private fun Session.setExpiry() {
        if (expiresIn != null)
            expiresAt = expiresIn.expiresAt()
    }

    private fun UserOrSession.setExpiry() {
        session?.apply { setExpiry() }
    }
}