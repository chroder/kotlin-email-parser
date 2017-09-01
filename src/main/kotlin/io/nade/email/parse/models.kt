package io.nade.email.parse

import java.util.*

data class ParsedMessage(
    val subject: String = "",
    val messageId: String? = "",
    val from: Address? = null,
    val sender: Address? = null,
    val replyTo: List<Address>,
    val returnPath: Address? = null,
    val tos: List<Address>,
    val ccs: List<Address>,
    val date: Date,
    val references: List<String>,
    val bodyText: String? = null,
    val bodyHtml: String? = null,
    val headers: List<HeaderInterface>
)

interface HeaderInterface {
    val name: String
    val value: String
}

data class Address(
    val name: String? = "",
    val email: String = ""
) {
    override fun toString(): String {
        if (name != null) {
            return "$name <$email>"
        } else {
            return email
        }
    }
}

data class Header(
    override val name: String,
    override val value: String
) : HeaderInterface

data class ParseErrorHeader(
    override val name: String,
    override val value: String,
    val parseError: String
) : HeaderInterface

data class MailboxHeader(
    override val name: String,
    override val value: String,
    val address: Address
) : HeaderInterface

data class MailboxListHeader(
    override val name: String,
    override val value: String,
    val addresses: List<Address>
) : HeaderInterface

data class DateHeader(
    override val name: String,
    override val value: String,
    val date: Date
) : HeaderInterface