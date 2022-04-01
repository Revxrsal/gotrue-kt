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

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import io.supabase.gotrue.type.*
import io.supabase.gotrue.util.MOSHI
import io.supabase.gotrue.util.currentTimeSeconds
import io.supabase.gotrue.util.queryParams
import okhttp3.OkHttpClient
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private val EXECUTOR = Executors.newSingleThreadScheduledExecutor()

/**
 * Represents the implementation of GoTrue.
 *
 * @param url The URL of the GoTrue server.
 * @param authorization The authorization key
 * @param headers Any additional headers to send to the GoTrue server.
 * @param autoRefreshToken Set to true if you want to automatically refresh the token before expiring
 * @param persistSession Set to "true" if you want to automatically save the user session into local storage.
 * @param storage The storage engine to use for persisting the session.
 */
class GoTrueClient(
    url: String = GOTRUE_URL,
    authorization: String,
    private val persistSession: Boolean = true,
    private val moshi: Moshi = MOSHI,
    client: OkHttpClient = OkHttpClient(),
    private val storage: AuthStorage,
    headers: Map<String, String> = emptyMap(),
    private val autoRefreshToken: Boolean = false
) {

    private val changeListeners = mutableMapOf<AuthChangeEvent, MutableList<Listener>>()
    var currentSession: Session? = null
        private set
    val currentUser get() = currentSession?.user

    private val api = GoTrueApi(
        url = url,
        authorization = authorization,
        client = client,
        moshi = moshi,
        headers = headers
    )

    @Volatile
    private var refreshTokenTask: ScheduledFuture<*>? = null

    /**
     * Loads the session from the local storage if it was present and
     * had not expired yet. This will also refresh the token.
     */
    fun recover() {
        recoverSession()
        recoverAndRefresh()
    }

    /**
     * Creates a new user using their email address or mobile phone. You
     * must either specify the email or the phone. (Email takes precedence when
     * both are supplied)
     *
     * @param email The email address of the user.
     * @param phone The phone number of the user.
     * @param password The password of the user.
     * @param redirectTo A URL or mobile address to send the user to after they are confirmed.
     * @param data Optional user metadata.
     */
    fun signUp(
        email: String? = null,
        phone: String? = null,
        password: String,
        data: Map<String, Any>? = null,
        redirectTo: String? = null,
    ): UserOrSession {
        removeSession()
        val response: UserOrSession
        if (email != null) {
            response = api.signUpWithEmail(
                email = email,
                password = password,
                data = data,
                redirectTo = redirectTo,
            )
        } else if (phone != null) {
            response = api.signUpWithPhone(
                phone = phone,
                password = password,
                data = data,
            )
        } else {
            error("You must either specify an email or phone!")
        }
        if (response.session != null) {
            saveSession(response.session)
            notifyAllSubscribers(AuthChangeEvent.SIGNED_IN)
        }
        return response
    }

    /**
     * Log in an existing user, or login via a third-party provider. If email
     * and phone are provided, email will be used and phone will be ignored.
     *
     * @param email The user's email address.
     * @param phone The user's phone number.
     * @param password The user's password.
     * @param refreshToken A valid refresh token that was returned on login.
     * @param provider One of the providers supported by GoTrue.
     * @param redirectTo A URL or mobile address to send the user to after they are confirmed.
     * @param scopes A space-separated list of scopes granted to the OAuth application.
     */
    fun signIn(
        email: String? = null,
        password: String? = null,
        phone: String? = null,
        refreshToken: String? = null,
        provider: Provider? = null,
        redirectTo: String? = null,
        scopes: String? = null,
        createUser: Boolean = false
    ): AuthResponse {
        removeSession()
        val response: Session
        if (email != null) {
            if (password != null) {
                response = api.signInWithEmail(
                    email = email,
                    password = password,
                    redirectTo = redirectTo,
                )
                saveSession(response)
                notifyAllSubscribers(AuthChangeEvent.SIGNED_IN)
            } else {
                api.sendMagicLinkEmail(
                    email = email,
                    createUser = createUser,
                    redirectTo = redirectTo,
                )
                return AuthResponse(currentSession!!, null)
            }
        } else if (phone != null) {
            if (password != null) {
                response = api.signInWithPhone(phone = phone, password = password)
                saveSession(response)
                notifyAllSubscribers(AuthChangeEvent.SIGNED_IN)
            } else {
                api.sendMobileOTP(phone = phone, createUser = createUser)
                return AuthResponse(currentSession!!, null)
            }
        } else if (refreshToken != null) {
            callRefreshToken(refreshToken = refreshToken)
            response = currentSession!!
        } else if (provider != null) {
            return AuthResponse(
                session = null,
                providerURL = api.getUrlForProvider(
                    provider = provider,
                    redirectTo = redirectTo,
                    scopes = scopes
                )
            )
        } else {
            error("You must either define an email, a phone number, a refresh token, or a provider!")
        }
        return AuthResponse(response, null)
    }

    /**
     * Log in a user given a User supplied OTP received via mobile.
     *
     * @param phone The user's phone number.
     * @param token The user's OTP.
     * @param redirectTo A URL or mobile address to send the user to after they are confirmed.
     */
    fun verifyOTP(
        phone: String,
        token: String,
        redirectTo: String? = null
    ): UserOrSession {
        removeSession()
        val response = api.verifyOTP(
            phone = phone,
            token = token,
            redirectTo = redirectTo
        )
        if (response.session != null) {
            saveSession(response.session)
            notifyAllSubscribers(AuthChangeEvent.SIGNED_IN)
        }
        return response
    }

    /**
     * Updates user data, if there is a logged-in user.
     *
     * @param attributes The attributes to update.
     */
    fun updateUser(attributes: UserAttributes): SupabaseUser {
        checkLoggedIn()
        val session = currentSession!!
        val response = api.updateUser(
            jwt = session.accessToken!!,
            attributes = attributes
        )
        currentSession = session.copy(user = response)
        saveSession(session)
        notifyAllSubscribers(AuthChangeEvent.USER_UPDATED)
        return response
    }

    /**
     * Sets the session data from refresh_token and returns current Session
     *
     * @param refreshToken A JWT token
     */
    fun setSession(refreshToken: String): Session {
        val session = api.refreshAccessToken(refreshToken)
        saveSession(session)
        notifyAllSubscribers(AuthChangeEvent.SIGNED_IN)
        return session
    }

    /**
     * Overrides the JWT on the current client. The JWT will then be sent in
     * all subsequent network requests.
     *
     * @param accessToken A JWT token
     */
    fun setAuth(accessToken: String): Session {
        val session = Session(
            accessToken = accessToken,
            tokenType = "bearer",
            user = null,
            expiresIn = currentSession?.expiresIn,
            expiresAt = currentSession?.expiresAt,
            refreshToken = currentSession?.refreshToken,
            providerToken = currentSession?.providerToken,
        )
        saveSession(session)
        return session
    }

    /**
     * Gets the session data from a URL string.
     *
     * @param url The URL string.
     * @param persistSession Optionally store the session in the storage
     */
    fun getSessionFromURL(url: String, persistSession: Boolean = false): Session {
        val query = url.queryParams
        val errorDescription = query.getOrDefault("error_description", emptyList())
        val accessToken = query.getOrDefault("access_token", emptyList())
        val expiresIn = query.getOrDefault("expires_in", emptyList())
        val refreshToken = query.getOrDefault("refresh_token", emptyList())
        val tokenType = query.getOrDefault("token_type", emptyList())
        when {
            errorDescription.isNotEmpty() -> throw SupabaseException(errorDescription[0])
            accessToken.getOrElse(0) { "" }.isEmpty() -> throw SupabaseException("No access_token detected.")
            refreshToken.getOrElse(0) { "" }.isEmpty() -> throw SupabaseException("No refresh_token detected")
            tokenType.getOrElse(0) { "" }.isEmpty() -> throw SupabaseException("No token_type detected.")
            expiresIn.getOrElse(0) { "" }.isEmpty() -> throw SupabaseException("No expires_in detected.")
        }
        val providerToken = query.getOrDefault("provider_token", emptyList())
        val expiresAt = (System.currentTimeMillis() / 1000L) +
                (expiresIn[0].toIntOrNull() ?: error("Invalid expires_in: ${expiresIn[0]}"))
        val user = api.getUser(jwt = accessToken[0])
        val session = Session(
            providerToken = providerToken.getOrNull(0),
            accessToken = accessToken[0],
            tokenType = tokenType[0],
            user = user,
            expiresIn = expiresIn[0].toLong(),
            expiresAt = expiresAt,
            refreshToken = refreshToken[0]
        )
        if (persistSession) {
            saveSession(session)
            val recoveryMode = query.getOrDefault("type", emptyList())
            notifyAllSubscribers(AuthChangeEvent.SIGNED_IN)
            if (recoveryMode.isNotEmpty() && recoveryMode[0] == "recovery") {
                notifyAllSubscribers(AuthChangeEvent.PASSWORD_RECOVERY)
            }
        }
        return session
    }

    /**
     * Log the user out.
     */
    fun signOut() {
        val accessToken = currentSession?.accessToken
        removeSession()
        notifyAllSubscribers(AuthChangeEvent.SIGNED_OUT)
        if (accessToken != null)
            api.signOut(accessToken)
    }

    /**
     * Force refreshes the session including the user data incase it was
     * updated in a different session.
     */
    fun refreshSession() {
        checkLoggedIn()
        callRefreshToken()
    }

    /**
     * Throws an exception if the user is not logged in
     */
    private fun checkLoggedIn() {
        if (currentUser == null)
            error("Not loggind in.")
    }

    private fun callRefreshToken(refreshToken: String? = null): Session {
        var token = refreshToken
        if (refreshToken == null) {
            if (currentSession != null)
                token = currentSession!!.refreshToken
            else
                error("No current session and refreshToken not supplied.")
        }
        val response = api.refreshAccessToken(token!!)
        saveSession(response)
        notifyAllSubscribers(AuthChangeEvent.TOKEN_REFRESHED)
        notifyAllSubscribers(AuthChangeEvent.SIGNED_IN)
        return response
    }

    private fun readStoredSession(): StorageEntry? {
        val json = storage[STORAGE_KEY] ?: return null
        return moshi.adapter(StorageEntry::class.java).fromJson(json)
    }

    /**
     * Save session to client
     */
    private fun saveSession(session: Session) {
        currentSession = session
        if (session.expiresAt != null) {
            val now = currentTimeSeconds()
            val expiresIn = session.expiresAt!! - now
            val refreshDurationBeforeExpires = if (expiresIn > 60) 60 else 0.5
            startAutoRefreshToken(value = (expiresIn - refreshDurationBeforeExpires.toDouble()) * 1000)
        }
        if (persistSession && session.expiresAt != null) {
            persistSession(session)
        }
    }

    private fun persistSession(session: Session) {
        val entry = StorageEntry(session = session, expiresAt = session.expiresAt!!)
        storage[STORAGE_KEY] = moshi.adapter(StorageEntry::class.java).toJson(entry)
    }

    private fun startAutoRefreshToken(value: Double) {
        refreshTokenTask?.cancel(true)
        if (value <= 0 || !autoRefreshToken) return
        refreshTokenTask = EXECUTOR.schedule({
            callRefreshToken()
        }, value.toLong(), TimeUnit.SECONDS);
    }

    private fun notifyAllSubscribers(event: AuthChangeEvent) {
        changeListeners[event]?.forEach { it(currentSession) }
    }

    /**
     * Remove the session.
     */
    private fun removeSession() {
        currentSession = null
        refreshTokenTask?.cancel(true)
        storage.remove(STORAGE_KEY)
    }

    private fun recoverSession() {
        val (session, expiresAt) = readStoredSession() ?: return
        if (expiresAt >= currentTimeSeconds()) {
            saveSession(session)
            notifyAllSubscribers(AuthChangeEvent.SIGNED_IN)
        }
    }

    private fun recoverAndRefresh() {
        val (session, expiresAt) = readStoredSession() ?: return
        if (expiresAt < currentTimeSeconds() && autoRefreshToken && session.refreshToken != null) {
            try {
                callRefreshToken(session.refreshToken)
            } catch (e: SupabaseException) {
                removeSession()
            }
        } else if (expiresAt < currentTimeSeconds() || session.user == null) {
            removeSession()
        } else {
            saveSession(session)
            notifyAllSubscribers(AuthChangeEvent.SIGNED_IN)
        }
    }

    fun on(event: AuthChangeEvent, callback: Listener): Subscription {
        val list = changeListeners.getOrPut(event) { mutableListOf() }
        list.add(callback)
        return Subscription { list.remove(callback) }
    }

    @JsonClass(generateAdapter = true)
    data class StorageEntry(
        val session: Session,
        @Json(name = "expires_at") val expiresAt: Long,
    )
}

fun interface Subscription {
    fun unregister()
}

typealias Listener = (Session?) -> Unit

