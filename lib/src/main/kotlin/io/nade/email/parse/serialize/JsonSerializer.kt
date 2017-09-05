package io.nade.email.parse.serialize

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.nade.email.parse.ParsedMessage
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class JsonSerializer(private val gson: Gson) : SerializerInterface {
    companion object {
        fun create(): JsonSerializer {
            val gson = GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create()

            return JsonSerializer(gson)
        }
    }

    override fun writeToStream(msg: ParsedMessage, ostream: OutputStream) {
        val writer = OutputStreamWriter(ostream, StandardCharsets.UTF_8)
        gson.toJson(msg, writer)
        writer.flush()
    }
}