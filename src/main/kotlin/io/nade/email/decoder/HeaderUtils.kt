package io.nade.email.decoder

import org.apache.james.mime4j.dom.Message
import org.apache.james.mime4j.dom.address.AddressList
import org.apache.james.mime4j.dom.address.Mailbox
import org.apache.james.mime4j.dom.address.MailboxList
import org.apache.james.mime4j.dom.field.*
import org.apache.james.mime4j.field.datetime.parser.DateTimeParser
import org.apache.james.mime4j.stream.Field
import java.io.StringReader
import java.lang.Exception
import java.util.*

object HeaderUtils {
    /**
     * Create an Address model from a Mailbox field.
     *
     * @param mb The Mailbox field
     * @return An Address model
     */
    fun mailboxToAddr(mb: Mailbox): Address {
        val cleanName = mb.name?.trim()?.trim('"', '\'') ?: ""
        val name      = if (cleanName.isNotEmpty()) cleanName else null

        return Address(name, "${mb.localPart}@${mb.domain}")
    }

    /**
     * Create a list of Address models from a MailboxList.
     *
     * @param mbs The MailboxList to process
     * @return A list of Address models
     */
    fun mailboxListToAddrList(mbs: MailboxList): List<Address> {
        return mbs.map { mailboxToAddr(it) }
    }

    /**
     * Create a list of Address models from a AddressList. Note that this creates a flat list. Any grouping
     * data that might be contained within the AddressList will be lost.
     *
     * @param mbs The AddressList to process
     * @return A list of Address models (flat)
     */
    fun mailboxListToAddrList(mbs: AddressList): List<Address> {
        return mbs.flatten().map { mailboxToAddr(it) }
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
            return msg.date
        }

        val rec = msg.header.getField("Received")?.body ?: return null
        val parts = rec.split(';', limit = 2)
        if (parts.size != 2) {
            return null
        }

        val dateStr = parts[1]
        return try {
            DateTimeParser(StringReader(dateStr)).parseAll().date
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Given a Return-Path header, get an Address. If the Return-Path is <>, that value will be
     * returned in an Address even though it's not a valid addr-spec.
     *
     * The <> value is often used to signify a client does not want a bounce in the event of an error.
     * This is usualy necessary because in the absense of a Return-Path, many servers bounce the message
     * to the From or Sender as a fallback. The <> value essentially communicates that the absense of a value
     * was on purpose.
     *
     * @return Address with the return path, or null.
     */
    fun getReturnPathAddr(field: Field): Address? {
        if (field is MailboxListField && field.mailboxList?.isNotEmpty() == true) {
            return mailboxToAddr(field.mailboxList[0])
        }

        val body = field.body ?: return null
        if (body == "<>") {
            // special no return value should be kept
            return Address("NORETURN", "<>")
        }

        return null
    }

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
            return ParseErrorHeader(name, value, field.parseException.message ?: "Unknown")
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
}