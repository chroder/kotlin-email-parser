package io.nade.email.parse.serialize

import com.fasterxml.jackson.databind.ObjectMapper
import org.msgpack.jackson.dataformat.MessagePackFactory

class MsgPackSerializer(mapper: ObjectMapper) : JacksonSerializer(mapper) {
    companion object {
        fun create(): MsgPackSerializer {
            val om = ObjectMapper(MessagePackFactory())
            return MsgPackSerializer(om)
        }
    }
}