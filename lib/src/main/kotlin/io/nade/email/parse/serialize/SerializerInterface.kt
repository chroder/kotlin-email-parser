package io.nade.email.parse.serialize

import io.nade.email.parse.ParsedMessage
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

interface SerializerInterface {
    /**
     * Encode the message to an OutputStream
     *
     * @param msg The parsed message you want to encode
     * @param ostream The OutputStream you want to write the result to
     */
    fun writeToStream(msg: ParsedMessage, ostream: OutputStream)

    /**
     * Encode the message to a string.
     *
     * @param msg The message to encode
     * @return The encoded message to a string
     */
    fun writeToString(msg: ParsedMessage): String {
        val ostream = ByteArrayOutputStream()
        writeToStream(msg, ostream)
        return ostream.toString(StandardCharsets.UTF_8.name())
    }

    /**
     * Encode the message and output to stdout
     *
     * @param msg The message to encode
     */
    fun writeToOutput(msg: ParsedMessage) {
        writeToStream(msg, System.out)
    }
}