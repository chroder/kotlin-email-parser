package io.nade.email.parse.encode

import com.fasterxml.jackson.databind.ObjectMapper
import io.nade.email.parse.ParsedMessage
import org.msgpack.jackson.dataformat.MessagePackFactory
import java.io.OutputStream

class MsgPackEncoder(private val objectMapper: ObjectMapper) : EncoderInterface {
    companion object {
        fun create(): MsgPackEncoder {
            val om = ObjectMapper(MessagePackFactory())
            return MsgPackEncoder(om)
        }
    }

    override fun encodeToStream(msg: ParsedMessage, ostream: OutputStream) {
        objectMapper.writeValue(ostream, msg)
        ostream.flush()
    }
}