package io.nade.email.parse.serialize

import com.fasterxml.jackson.databind.ObjectMapper
import io.nade.email.parse.ParsedMessage
import java.io.OutputStream

open class JacksonSerializer(private val mapper: ObjectMapper) : SerializerInterface {
    override fun writeToStream(msg: ParsedMessage, ostream: OutputStream) {
        mapper.writeValue(ostream, msg)
        ostream.flush()
    }
}