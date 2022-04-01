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
package io.supabase.gotrue.util

import com.squareup.moshi.Moshi
import io.supabase.gotrue.UOSAdapter
import io.supabase.gotrue.json.CaseInsensitiveEnumsAdapterFactory
import io.supabase.gotrue.json.OffsetDateAdapter
import io.supabase.gotrue.json.UUIDAdapter
import java.time.OffsetDateTime
import java.util.*

val MOSHI: Moshi = Moshi.Builder()
    .add(CaseInsensitiveEnumsAdapterFactory)
    .add(UUID::class.java, UUIDAdapter)
    .add(OffsetDateTime::class.java, OffsetDateAdapter)
    .add(UOSAdapter)
    .build()

fun <T> linkedListOf(vararg values: T): LinkedList<T> = LinkedList<T>().also {
    it.addAll(values)
}

fun Long.expiresAt(): Long {
    return currentTimeSeconds() + this
}

fun currentTimeSeconds() = System.currentTimeMillis() / 1000

fun String.withRedirect(redirectTo: String?): String {
    return if (redirectTo != null)
        this + "&redirect_to=${redirectTo.encodeURI()}"
    else
        this
}

fun String.toUUID(): UUID = UUID.fromString(this)
