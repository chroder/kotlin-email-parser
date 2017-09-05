package io.nade.email.parse

import org.apache.james.mime4j.dom.Entity
import org.apache.james.mime4j.dom.Message
import org.apache.james.mime4j.dom.Multipart
import org.apache.james.mime4j.dom.SingleBody
import org.apache.james.mime4j.dom.address.AddressList
import org.apache.james.mime4j.dom.address.Mailbox
import org.apache.james.mime4j.dom.address.MailboxList
import org.apache.james.mime4j.dom.field.*
import org.apache.james.mime4j.field.datetime.parser.DateTimeParser
import org.apache.james.mime4j.stream.Field
import org.slf4j.LoggerFactory
import java.io.StringReader
import java.lang.Exception
import java.util.*
import org.apache.james.mime4j.dom.Header as DomHeader

val logger = ContextLogger(LoggerFactory.getLogger(Parser::class.java))

/**
 * Create an Addr model from a Mailbox field.
 *
 * @param mb The Mailbox field
 * @return An Addr model
 */
fun mailboxToAddr(mb: Mailbox): Addr {
    val cleanName = mb.name?.trim()?.trim('"', '\'') ?: ""
    val name      = if (cleanName.isNotEmpty()) cleanName else null

    return Addr(name, "${mb.localPart}@${mb.domain}")
}

fun Mailbox.toAddr(): Addr = mailboxToAddr(this)

/**
 * Create a list of Addr models from a MailboxList.
 *
 * @param mbs The MailboxList to process
 * @return A list of Addr models
 */
fun mailboxListToAddrList(mbs: MailboxList): List<Addr> {
    return mbs.map { mailboxToAddr(it) }
}

fun MailboxList.toAddrList(): List<Addr> = mailboxListToAddrList(this)

/**
 * Create a list of Addr models from a AddressList. Note that this creates a flat list. Any grouping
 * data that might be contained within the AddressList will be lost.
 *
 * @param mbs The AddressList to process
 * @return A list of Addr models (flat)
 */
fun mailboxListToAddrList(mbs: AddressList): List<Addr> {
    return mbs.flatten().map { mailboxToAddr(it) }
}

fun AddressList.toAddrList(): List<Addr> = mailboxListToAddrList(this)

/**
 * Convert fields to our own header models.
 *
 * @param field The Mime4j field
 * @return Our own header model
 */
fun fieldToHeader(field: Field): HeaderInterface {
    val name  = field.name ?: ""
    val value = field.body ?: ""

    if (field is ParsedField && !field.isValidField) {
        val errMsg = field.parseException.message ?: "Unknown"
        logger.info("Invalid Field<${field.javaClass}>: $errMsg")
        return ParseErrorHeader(name, value, errMsg)
    }

    // DateTimeFieldLenientImpl doesnt raise any errors,
    // it just returns a null date
    if (field is DateTimeField && field.date == null) {
        val errMsg = field.parseException.message ?: "Invalid date format"
        logger.info("Invalid Field<${field.javaClass}>: $errMsg")
        return ParseErrorHeader(name, value, errMsg)
    }

    return when (field) {
        is DateTimeField    -> DateHeader(name, value, field.date)
        is AddressListField -> MailboxListHeader(name, value, mailboxListToAddrList(field.addressList))
        is MailboxListField -> MailboxListHeader(name, value, mailboxListToAddrList(field.mailboxList))
        is MailboxField     -> MailboxHeader(name, value, mailboxToAddr(field.mailbox))
        else                -> Header(name, value)
    }
}

/**
 * Get a list of references given an array of References fields.
 *
 * @param fields The fields you want to parse
 * @return The list of references parsed from each field
 */
fun parseReferences(fields: List<Field>): List<String> {
    return fields.flatMap { field ->
        field.body?.split("\\s+".toRegex())?.map { it.trim() } ?: listOf()
    }
}

/**
 * Given a message, try to guess the date it was sent based on the Date header, and if that doesn't exist,
 * try to parse the Receive header.
 *
 * @param msg The message to parse
 * @return The guessed date, or null
 */
fun guessDateFromMessage(msg: Message): Date? {
    if (msg.date != null) {
        logger.debug("guessDateFromMessage on ${msg.messageId ?: msg.hashCode()}: using msg.date")
        return msg.date
    }

    val rec = msg.header.getField("Received")?.body
    if (rec == null) {
        logger.debug("guessDateFromMessage on ${msg.messageId ?: msg.hashCode()}: no Received header")
        return null
    }

    val parts = rec.split(';', limit = 2)
    if (parts.size != 2) {
        logger.debug("guessDateFromMessage on ${msg.messageId ?: msg.hashCode()}: unknown format on Received header")
        return null
    }

    val dateStr = parts[1]
    return try {
        logger.debug("guessDateFromMessage on ${msg.messageId ?: msg.hashCode()}: parsing date from: $dateStr")
        DateTimeParser(StringReader(dateStr)).parseAll().date
    } catch (e: Exception) {
        logger.debug("guessDateFromMessage on ${msg.messageId ?: msg.hashCode()}: failed to parse: $dateStr")
        null
    }
}

/**
 * Given a Return-Path header, get an Addr. If the Return-Path is <>, that value will be
 * returned in an Addr even though it's not a valid addr-spec.
 *
 * The <> value is often used to signify a client does not want a bounce in the event of an error.
 * This is usualy necessary because in the absense of a Return-Path, many servers bounce the message
 * to the From or Sender as a fallback. The <> value essentially communicates that the absense of a value
 * was on purpose.
 *
 * @return Addr with the return path, or null.
 */
fun getReturnPathAddr(field: Field): Addr? {
    if (field is MailboxField) {
        return if (field.mailbox != null) {
            logger.debug("getReturnPathAddr: got from mailbox")
            mailboxToAddr(field.mailbox)
        } else {
            logger.debug("getReturnPathAddr: return path mailbox is null")
            null
        }
    }

    val body = field.body
    if (body == null) {
        logger.debug("getReturnPathAddr: no valid return path: ${field.javaClass}: null")
    }

    if (body == "<>") {
        logger.debug("getReturnPathAddr: return path is $body, making special Addr with <>")
        // special no return value should be kept
        return Addr("NORETURN", "<>")
    }

    logger.debug("getReturnPathAddr: no valid return path: ${field.javaClass}: $body")
    return null
}

/**
 * Given a message entity, walk over each and every part.
 *
 * @param message The message to walk.
 * @param block The function to execute for each part
 */
fun walkMessageParts(message: Entity, block: (b: Entity) -> Unit) {
    val body = message.body
    when (body) {
        is Multipart  -> {
            block(message)
            for (sub in body.bodyParts) {
                walkMessageParts(sub, block)
            }
        }

        is SingleBody -> {
            block(message)
        }

        is Message    -> {
            block(message)
            walkMessageParts(message, block)
        }
    }
}

/**
 * Guess is a message was an automatic message or auto-response. These
 * are messages which we typically don't want to send our own auto-replies to.
 */
fun guessIsAutoMessage(msg: Message): Boolean {
    if (msg.header.hasHeaderEqual("Preference", "auto_reply")) {
        return true
    }

    if (
        msg.header.hasHeaderEqual("Auto-Submitted", "auto-replied")
        || msg.header.hasHeaderEqual("X-Autoreply", "yes")
    ) {
        return true
    }

    if (msg.header.hasHeaderEqual("X-POST-MessageClass", "9; Autoresponder")) {
        return true
    }

    if (
        msg.header.hasHeader("X-Autorespond")
        || msg.header.hasHeader("X-AutoReply-From")
        || msg.header.hasHeader("X-Mail-Autoreply")
        || msg.header.hasHeader("X-FC-MachineGenerated")
    ) {
        return true
    }

    if (msg.header.hasHeaderEqual("Delivered-To", "Autoresponder")) {
        return true;
    }

    if (msg.header.hasHeader("Auto-Submitted") && !msg.header.hasHeaderEqual("Auto-Submitted", "no")) {
        return true
    }

    if (msg.header.hasHeader("X-Cron-Env")) {
        return true
    }

    if (msg.header.hasHeaderEqual("X-Auto-Response-Suppress", "OOF")) {
        return true
    }

    listOf("junk", "bulk", "list", "auto_reply").forEach {
        if (msg.header.hasHeaderEqual("Precedence", it) || msg.header.hasHeaderEqual("X-Precedence", it)) {
            return true
        }
    }

    return false
}

/**
 * Try to guess based on the subject of a message if it's an OOF reply.
 * Usually used with guessIsAutoMessage to detect OOO replies.
 */
fun guessIsOooSubject(subject: String): Boolean {
    val s = subject.toLowerCase()
    return s.startsWith("out of office:")
        || s.startsWith("out of the office:")
        || s.startsWith("out of office autoreply:")
        || s.startsWith("out of office reply:")
        || s.startsWith("automatic reply:")
        || s.endsWith("is out of the office")
}

fun guessIsReply(msg: Message): Boolean {
    if (msg.header.getFields("References").isNotEmpty()) {
        return true
    }

    val s = msg.subject.toLowerCase()
    return s.matches("^re:".toRegex())
}

fun guessIsForward(msg: Message): Boolean {
    val s = msg.subject.toLowerCase()
    return s.matches("^(fwd|fw):".toRegex())
}

fun DomHeader.hasHeader(name: String): Boolean = this.getField(name) != null

fun DomHeader.hasHeaderEqual(name: String, value: String, icase: Boolean = true): Boolean {
    if (icase) {
        val lowerValue = value.toLowerCase()
        return this.getFields(name).any { it.body != null && it.body.toLowerCase() == lowerValue }
    } else {
        return this.getFields(name).any { it.body == value }
    }
}