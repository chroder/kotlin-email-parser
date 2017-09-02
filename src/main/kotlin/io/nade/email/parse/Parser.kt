package io.nade.email.parse

import org.apache.commons.io.IOUtils
import org.apache.james.mime4j.dom.*
import org.apache.james.mime4j.field.LenientFieldParser
import org.apache.james.mime4j.field.MailboxFieldLenientImpl
import org.apache.james.mime4j.message.DefaultMessageBuilder
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.InputStream
import java.util.*

class Parser(private val msgBuilder: MessageBuilder) {
    /**
     * Decode an RFC 822 message into a structured object.
     *
     * @param istream An input stream with the raw email source
     * @return The decoded message
     */
    fun parse(istream: InputStream): ParsedMessage {
        val sizedIstream = SizeInputStream(istream)
        val parsedMessage = msgBuilder.parseMessage(sizedIstream)

        val subject   = parsedMessage.subject ?: ""
        val messageId = parsedMessage.messageId ?: null

        val fromAddress: Address? = if (parsedMessage.from?.isNotEmpty() == true) {
            HeaderUtils.mailboxToAddr(parsedMessage.from[0])
        } else {
            null
        }

        val senderAddress: Address? = if (parsedMessage.sender != null) {
            HeaderUtils.mailboxToAddr(parsedMessage.sender)
        } else {
            null
        }

        val replyToAddresses: List<Address> = if (parsedMessage.replyTo?.isNotEmpty() == true) {
            HeaderUtils.mailboxListToAddrList(parsedMessage.replyTo)
        } else {
            listOf()
        }

        val toAddresses: List<Address> = if (parsedMessage.to?.isNotEmpty() == true) {
            HeaderUtils.mailboxListToAddrList(parsedMessage.to)
        } else {
            listOf()
        }

        val ccAddress: List<Address> = if (parsedMessage.cc?.isNotEmpty() == true) {
            HeaderUtils.mailboxListToAddrList(parsedMessage.cc)
        } else {
            listOf()
        }

        val returnPathField = parsedMessage.header.getField("Return-Path")
        val returnPath      = if (returnPathField != null) HeaderUtils.getReturnPathAddr(returnPathField) else null

        val date       = HeaderUtils.guessDateFromMessage(parsedMessage)
        val headers    = parsedMessage.header.fields.map { HeaderUtils.fieldToHeader(it) }
        val references = HeaderUtils.parseReferences(parsedMessage.header.getFields("References"))

        val textParts: MutableList<String> = mutableListOf()
        val htmlParts: MutableList<String> = mutableListOf()

        walkParts(parsedMessage) { part ->
            // - we dont care about processing multi-parts which are essentially containers
            // for the real parts we want to read. So we have this condition to ignore them
            // and only process actual parts.
            if (!part.isMultipart) {
                val disposition = part.dispositionType
                var filename    = part.filename
                val mimeType    = part.mimeType
                val contentId   = part.parent?.header?.getField("Content-Id")?.body?.toString() ?: null
                val body        = part.body

                // A body part is any text part that has no disposition
                // And we also handle the edge-case where the disposition is inline, but it's not a
                // a file (no filename, no content-id) which is something some clients do sometimes
                // when they feel like it
                if (body is TextBody && (
                    disposition == null
                    || (disposition == "inline" && filename == null && contentId == null)
                )) {
                    if (mimeType == "text/html") {
                        htmlParts.add(IOUtils.toString(body.reader))
                    } else {
                        textParts.add(IOUtils.toString(body.reader))
                    }
                } else {
                    TODO("Save the part as an attachment")
                }
            }
        }

        val bodyHtml = if (htmlParts.isNotEmpty()) htmlParts.joinToString("").replace("\r\n", "\n").replace("\r", "\n") else null
        val bodyText = if (textParts.isNotEmpty()) textParts.joinToString("\n").replace("\r\n", "\n").replace("\r", "\n") else null

        return ParsedMessage(
            subject = subject,
            messageId = messageId,
            from = fromAddress,
            sender = senderAddress,
            replyTo = replyToAddresses,
            returnPath = returnPath,
            tos = toAddresses,
            ccs = ccAddress,
            date = date,
            references = references,
            bodyHtml = bodyHtml,
            bodyText = bodyText,
            headers = headers,
            size = sizedIstream.bytesRead
        )
    }

    private fun walkParts(message: Entity, block: (b: Entity) -> Unit) {
        val body = message.body
        when (body) {
            is Multipart -> {
                block(message)
                for (sub in body.bodyParts) {
                    walkParts(sub, block)
                }
            }

            is SingleBody -> {
                block(message)
            }

            is Message -> {
                block(message)
                walkParts(message, block)
            }
        }
    }

    companion object {
        fun create(): Parser {
            val fieldParser = LenientFieldParser()
            fieldParser.setFieldParser("Return-Path", MailboxFieldLenientImpl.PARSER)

            val msgBuilder = DefaultMessageBuilder()
            msgBuilder.setFieldParser(fieldParser)

            return Parser(msgBuilder)
        }
    }
}