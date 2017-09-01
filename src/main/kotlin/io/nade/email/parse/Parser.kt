package io.nade.email.parse

import org.apache.james.mime4j.dom.MessageBuilder
import org.apache.james.mime4j.field.LenientFieldParser
import org.apache.james.mime4j.field.MailboxFieldLenientImpl
import org.apache.james.mime4j.message.DefaultMessageBuilder
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
        val parsedMessage = msgBuilder.parseMessage(istream)

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

        val date       = HeaderUtils.guessDateFromMessage(parsedMessage) ?: Date()
        val headers    = parsedMessage.header.fields.map { HeaderUtils.fieldToHeader(it) }
        val references = HeaderUtils.parseReferences(parsedMessage.header.getFields("References"))

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
            bodyHtml = "",
            bodyText = "",
            headers = headers
        )
    }

    companion object {
        fun create(): Parser {
            val fieldParser = LenientFieldParser()
            fieldParser.setFieldParser("Return-Path", MailboxFieldLenientImpl.PARSER)

            val msgBuilder = DefaultMessageBuilder();
            msgBuilder.setFieldParser(fieldParser)

            return Parser(msgBuilder)
        }
    }
}