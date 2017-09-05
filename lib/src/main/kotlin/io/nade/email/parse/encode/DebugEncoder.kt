package io.nade.email.parse.encode

import io.nade.email.parse.ParsedMessage
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat

class DebugEncoder : EncoderInterface {
    companion object {
        fun create(): DebugEncoder {
            return DebugEncoder()
        }
    }

    override fun encodeToStream(msg: ParsedMessage, ostream: OutputStream) {
        val writer = OutputStreamWriter(ostream, StandardCharsets.UTF_8)
        writer.appendln("Message Size: %s".format(msg.getReadableSize()))
        writer.appendln("Subject: %s".format(msg.subject))
        if (msg.date != null) {
            writer.appendln("Date: %s".format(SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(msg.date)))
        }
        writer.appendln("From: %s".format(msg.from?.toString() ?: ""))
        if (msg.tos.isNotEmpty()) {
            writer.appendln("To: %s".format(msg.tos.joinToString(", ") { it.toString() }))
        }
        if (msg.ccs.isNotEmpty()) {
            writer.appendln("CCs: %s".format(msg.ccs.joinToString(", ") { it.toString() }))
        }
        if (msg.sender != null) {
            writer.appendln("Sender: %s".format(msg.sender.toString()))
        }
        if (msg.replyTo.isNotEmpty()) {
            writer.appendln("Reply-To: %s".format(msg.replyTo.toString()))
        }
        if (msg.returnPath != null) {
            writer.appendln("Return Path: %s".format(msg.returnPath.toString()))
        }
        if (msg.messageId != null) {
            writer.appendln("Message-ID: %s".format(msg.messageId))
        }

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