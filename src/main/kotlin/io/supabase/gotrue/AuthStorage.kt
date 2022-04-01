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

/**
 * Represents a basic storage for storing authentication sessions. This
 * is equivalent to the browser's local storage.
 */
interface AuthStorage {

    /**
     * Sets the value of the given key
     */
    operator fun set(key: String, value: String)

    /**
     * Returns the value of the given key
     */
    operator fun get(key: String): String?

    /**
     * Removes the given key
     */
    fun remove(key: String)

    /**
     * Saves to the disk
     */
    fun save() {}

}

/**
 * A basic implementation of [AuthStorage] that stores information inside
 * memory.
 */
class MemoryStorage : AuthStorage {

    private val data = mutableMapOf<String, String>()

    override fun set(key: String, value: String) {
        data[key] = value
    }

    override fun get(key: String): String? {
        return data[key]
    }

    override fun remove(key: String) {
        data.remove(key)
    }
}