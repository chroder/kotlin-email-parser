package io.nade.email.parse.encode

import io.nade.email.parse.ParsedMessage
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class DebugEncoder : EncoderInterface {
    companion object {
        fun create(): DebugEncoder {
            return DebugEncoder()
        }
    }

    override fun encodeToStream(msg: ParsedMessage, ostream: OutputStream) {
        val writer = OutputStreamWriter(ostream, StandardCharsets.UTF_8)
        writer.appendln("Subject: %s".format(msg.subject))
        writer.appendln("From: %s".format(msg.from?.toString() ?: ""))
        writer.appendln("To: %s".format(msg.tos.joinToString(", ") { it.toString() }))
        writer.appendln("CCs: %s".format(msg.ccs.joinToString(", ") { it.toString() }))

        writer.appendln("")
        writer.appendln("")

        if (msg.bodyHtml != null && msg.bodyText != null) {
            writer.appendln("HTML:\n==============================================================================")
        }
        if (msg.bodyHtml != null) {
            writer.appendln(msg.bodyHtml)
        }
        if (msg.bodyHtml != null && msg.bodyText != null) {
            writer.appendln("Text:\n==============================================================================")
        }
        if (msg.bodyText != null) {
            writer.appendln(msg.bodyText)
        }

        writer.flush()
    }
}