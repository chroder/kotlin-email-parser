package io.nade.email.parse

import mu.KotlinLogging
import org.apache.commons.io.IOUtils
import org.apache.james.mime4j.dom.*
import org.apache.james.mime4j.field.LenientFieldParser
import org.apache.james.mime4j.field.MailboxFieldLenientImpl
import org.apache.james.mime4j.message.DefaultMessageBuilder
import org.slf4j.MDC
import java.io.InputStream
import java.util.*

class Parser {
    /**
     * Decode an RFC 822 message into a structured object using the default message builder.s
     *
     * @param istream An input stream with the raw email source
     * @return The decoded message
     */
    fun parse(istream: InputStream): ParsedMessage = parse(istream, createDefaultMessageBuilder())

    /**
     * Decode an RFC 822 message into a structured object.
     *
     * @param istream An input stream with the raw email source
     * @param msgBuilder Use the specified message builder
     * @return The decoded message
     */
    fun parse(istream: InputStream, msgBuilder: MessageBuilder): ParsedMessage {
        val sizedIstream = SizeInputStream(istream)
        val parsedMessage = msgBuilder.parseMessage(sizedIstream)

        val messageId = parsedMessage.messageId ?: null
        logger.debug("messageID = ${messageId ?: "null"}")

        val subject                  = parsedMessage.subject ?: ""
        val fromAddr: Addr?          = parsedMessage.from?.toAddrList()?.first()
        val senderAddr: Addr?        = parsedMessage.sender?.toAddr()
        val replyToAddrs: List<Addr> = parsedMessage.replyTo?.toAddrList() ?: listOf()
        val toAddrs: List<Addr>      = parsedMessage.to?.toAddrList() ?: listOf()
        val ccAddrs: List<Addr>      = parsedMessage.cc?.toAddrList() ?: listOf()

        val returnPathField = parsedMessage.header.getField("Return-Path")
        val returnPath      = if (returnPathField != null) getReturnPathAddr(returnPathField) else null

        val date       = guessDateFromMessage(parsedMessage)
        val headers    = parsedMessage.header.fields.map { fieldToHeader(it) }
        val references = parseReferences(parsedMessage.header.getFields("References"))

        val textParts: MutableList<String> = mutableListOf()
        val htmlParts: MutableList<String> = mutableListOf()

        walkMessageParts(parsedMessage) { part ->
            // - we dont care about processing multi-parts which are essentially containers
            // for the real parts we want to read. So we have this condition to ignore them
            // and only process actual parts.
            if (!part.isMultipart) {
                val disposition = part.dispositionType
                val filename    = part.filename
                val mimeType    = part.mimeType
                val contentId   = part.parent?.header?.getField("Content-Id")?.body?.toString()
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
                    //TODO
                }
            }
        }

        val bodyHtml = if (htmlParts.isNotEmpty()) htmlParts.joinToString("").replace("\r\n", "\n").replace("\r", "\n") else null
        val bodyText = if (textParts.isNotEmpty()) textParts.joinToString("\n").replace("\r\n", "\n").replace("\r", "\n") else null

        return ParsedMessage(
            subject = subject,
            messageId = messageId,
            from = fromAddr,
            sender = senderAddr,
            replyTo = replyToAddrs,
            returnPath = returnPath,
            tos = toAddrs,
            ccs = ccAddrs,
            date = date,
            references = references,
            bodyHtml = bodyHtml,
            bodyText = bodyText,
            headers = headers,
            size = sizedIstream.bytesRead
        )
    }

    fun parseToResult(istream: InputStream, msgBuilder: MessageBuilder): ParseResult {
        val contextId = UUID.randomUUID().toString()
        var exception: Exception? = null
        val logLines: String

        MDC.put("emailParserContextId", contextId)
        val message = try {
            parse(istream, msgBuilder)
        } catch (e: Exception) {
            logger.error("Exception while parsing: [${e.javaClass}] ${e.message}")
            exception = e
            null
        } finally {
            MDC.remove("emailParserContextId")

            logLines = ContextLogger.getLogLines(contextId).toLogString()
            ContextLogger.clearContext(contextId)
        }

        return ParseResult(
            message = message,
            log = logLines,
            exception = exception,
            contextId = contextId
        )
    }

    fun parseToResult(istream: InputStream): ParseResult = parseToResult(istream, createDefaultMessageBuilder())

    companion object {
        val logger = ContextLogger(KotlinLogging.logger{})

        fun createDefaultMessageBuilder(): MessageBuilder {
            val fieldParser = LenientFieldParser()
            fieldParser.setFieldParser("Return-Path", MailboxFieldLenientImpl.PARSER)

            val msgBuilder = DefaultMessageBuilder()
            msgBuilder.setFieldParser(fieldParser)
            return msgBuilder
        }
    }
}