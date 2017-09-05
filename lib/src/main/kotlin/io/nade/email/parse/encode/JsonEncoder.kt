package io.nade.email.parse.encode

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.nade.email.parse.ParsedMessage
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class JsonEncoder(private val gson: Gson) : EncoderInterface {
    companion object {
        fun create(): JsonEncoder {
            val gson = GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create()

            return JsonEncoder(gson)
        }
    }

    override fun encodeToStream(msg: ParsedMessage, ostream: OutputStream) {
        val writer = OutputStreamWriter(ostream, StandardCharsets.UTF_8)
        gson.toJson(msg, writer)
        writer.flush()
    }
}