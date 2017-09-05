package io.nade.email.parse.serialize

import com.fasterxml.jackson.databind.ObjectMapper
import io.nade.email.parse.ParsedMessage
import org.msgpack.jackson.dataformat.MessagePackFactory
import java.io.OutputStream

class MsgPackSerializer(private val objectMapper: ObjectMapper) : SerializerInterface {
    companion object {
        fun create(): MsgPackSerializer {
            val om = ObjectMapper(MessagePackFactory())
            return MsgPackSerializer(om)
        }
    }

    override fun writeToStream(msg: ParsedMessage, ostream: OutputStream) {
        objectMapper.writeValue(ostream, msg)
        ostream.flush()
    }
}