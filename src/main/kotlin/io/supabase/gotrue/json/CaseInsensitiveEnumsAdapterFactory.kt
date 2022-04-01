package io.supabase.gotrue.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import java.lang.reflect.Type

object CaseInsensitiveEnumsAdapterFactory : JsonAdapter.Factory {

    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (type !is Class<*> || !type.isEnum) return null
        val enumClass = type.asSubclass(Enum::class.java)
        val keyToValues = enumClass.enumConstants.associateBy { it.name.lowercase() }
        val valuesToKeys = keyToValues.entries.associateBy({ it.value }) { it.key }
        return object : JsonAdapter<Any>() {
            override fun fromJson(reader: JsonReader): Any? {
                return keyToValues[reader.nextString().lowercase()]
            }

            override fun toJson(writer: JsonWriter, value: Any?) {
                writer.value(valuesToKeys[(value as Enum<*>)])
            }
        }.nullSafe()
    }
}