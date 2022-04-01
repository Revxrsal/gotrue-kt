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
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import io.supabase.gotrue.type.Session
import io.supabase.gotrue.type.SupabaseUser
import java.lang.reflect.Type

/**
 * A class for wrapping responses in which either a session or a user
 * might be returned.
 *
 * This will be a:
 * - Logged-in session if the server has "autoconfirm" ON
 * - User if the server has "autoconfirm" OFF
 */
data class UserOrSession(val user: SupabaseUser?, val session: Session?) {
    override fun equals(other: Any?): Boolean {
        return when (other) {
            is UserOrSession -> if (user != null) user == other.user else session == other.session
            is SupabaseUser -> user == other
            is Session -> session == other
            else -> false
        }
    }

    override fun hashCode(): Int {
        return user?.hashCode() ?: session!!.hashCode()
    }

    override fun toString(): String {
        return user?.toString() ?: session!!.toString()
    }

    val isUser = user != null
    val isSession = session != null
}

object UOSAdapter : JsonAdapter.Factory {

    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (type != UserOrSession::class.java) return null
        val sessionAdapter: JsonAdapter<Session> = moshi.nextAdapter(this, Session::class.java, annotations)
        val userAdapter: JsonAdapter<SupabaseUser> = moshi.nextAdapter(this, SupabaseUser::class.java, annotations)
        return object : JsonAdapter<UserOrSession>() {
            override fun fromJson(reader: JsonReader): UserOrSession {
                val isSession = isSession(reader.peekJson())
                return if (isSession) {
                    val session = sessionAdapter.fromJson(reader)
                    UserOrSession(session = session, user = null)
                } else {
                    val user = userAdapter.fromJson(reader)
                    UserOrSession(user = user, session = null)
                }
            }

            override fun toJson(writer: JsonWriter, value: UserOrSession?) {
                if (value == null) return
                if (value.user != null) {
                    userAdapter.toJson(writer, value.user)
                } else if (value.session != null) {
                    sessionAdapter.toJson(writer, value.session)
                }
            }
        }.nullSafe()
    }

    private fun isSession(peek: JsonReader): Boolean {
        peek.beginObject()
        while (peek.hasNext()) {
            val name = peek.nextName()
            peek.skipValue()
            if (name == "access_token") {
                return true
            }
        }
        return false
    }
}
