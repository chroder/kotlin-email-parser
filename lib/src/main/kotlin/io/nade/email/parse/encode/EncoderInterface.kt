package io.nade.email.parse.encode

import io.nade.email.parse.ParsedMessage
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

interface EncoderInterface {
    /**
     * Encode the message to an OutputStream
     *
     * @param msg The parsed message you want to encode
     * @param ostream The OutputStream you want to write the result to
     */
    fun encodeToStream(msg: ParsedMessage, ostream: OutputStream)

    /**
     * Encode the message to a string.
     *
     * @param msg The message to encode
     * @return The encoded message to a string
     */
    fun encodeToString(msg: ParsedMessage): String {
        val ostream = ByteArrayOutputStream()
        encodeToStream(msg, ostream)
        return ostream.toString(StandardCharsets.UTF_8.name())
    }

    /**
     * Encode the message and output to stdout
     *
     * @param msg The message to encode
     */
    fun encodeToOutput(msg: ParsedMessage) {
        encodeToStream(msg, System.out)
    }
}