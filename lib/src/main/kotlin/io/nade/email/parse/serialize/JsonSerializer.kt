package io.nade.email.parse.serialize

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import java.text.SimpleDateFormat

class JsonSerializer(mapper: ObjectMapper) : JacksonSerializer(mapper) {
    companion object {
        fun create(): JsonSerializer {
            val om = ObjectMapper()
            om.enable(SerializationFeature.INDENT_OUTPUT)
            om.dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return JsonSerializer(om)
        }
    }
}