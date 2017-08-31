package io.nade.email_decoder

import org.apache.james.mime4j.dom.MessageBuilder
import org.apache.james.mime4j.field.LenientFieldParser
import org.apache.james.mime4j.field.MailboxFieldLenientImpl
import org.apache.james.mime4j.field.MailboxListFieldImpl
import org.apache.james.mime4j.message.DefaultMessageBuilder
import java.io.InputStream
import java.util.*

class Decoder(private val msgBuilder: MessageBuilder) {
    /**
     * Decode an RFC 822 message into a structured object.
     *
     * @param istream An input stream with the raw email source
     * @return The decoded message
     */
    fun decode(istream: InputStream): DecodedMessage {
        val parsedMessage = msgBuilder.parseMessage(istream)

        val subject   = parsedMessage.subject ?: ""
        val messageId = parsedMessage.messageId ?: ""

        val fromAddr: Addr? = if (parsedMessage.from != null && parsedMessage.from.size >= 1) {
            HeaderUtils.mailboxToAddr(parsedMessage.from[0])
        } else {
            null
        }

        val senderAddr: Addr? = if (parsedMessage.sender != null) {
            HeaderUtils.mailboxToAddr(parsedMessage.sender)
        } else {
            null
        }

        val replyToAddrs: List<Addr> = if (parsedMessage.replyTo != null && parsedMessage.replyTo.size >= 1) {
            HeaderUtils.mailboxListToAddrList(parsedMessage.replyTo)
        } else {
            listOf()
        }

        val toAddrs: List<Addr> = if (parsedMessage.to != null && parsedMessage.to.size >= 1) {
            HeaderUtils.mailboxListToAddrList(parsedMessage.to)
        } else {
            listOf()
        }

        val ccAddr: List<Addr> = if (parsedMessage.cc != null && parsedMessage.cc.size >= 1) {
            HeaderUtils.mailboxListToAddrList(parsedMessage.cc)
        } else {
            listOf()
        }

        val returnPathField = parsedMessage.header.getField("Return-Path")
        val returnPath = if (returnPathField != null) HeaderUtils.getReturnPathAddr(returnPathField) else null

        val date       = HeaderUtils.guessDateFromMessage(parsedMessage) ?: Date()
        val headers    = parsedMessage.header.fields.map { HeaderUtils.fieldToHeader(it) }
        val references = HeaderUtils.parseReferences(parsedMessage.header.getFields("References"))

        return DecodedMessage(
            subject      = subject,
            messageId    = messageId,
            from         = fromAddr,
            sender       =  senderAddr,
            replyTo      = replyToAddrs,
            returnPath   = returnPath,
            tos          = toAddrs,
            ccs          = ccAddr,
            date         = date,
            references   = references,
            bodyHtml     = "",
            bodyText     = "",
            headers      = headers
        )
    }

    companion object {
        fun create(): Decoder {
            val fieldParser = LenientFieldParser()
            fieldParser.setFieldParser("Return-Path", MailboxFieldLenientImpl.PARSER)

            val msgBuilder = DefaultMessageBuilder();
            msgBuilder.setFieldParser(fieldParser)

            return Decoder(msgBuilder)
        }
    }
}