package io.nade.email_decoder

import java.util.*

data class DecodedMessage(
    val subject: String = "",
    val messageId: String? = "",
    val from: Addr? = null,
    val sender: Addr? = null,
    val replyTo: List<Addr>,
    val returnPath: Addr? = null,
    val tos: List<Addr>,
    val ccs: List<Addr>,
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

data class Addr(
    val name: String? = "",
    val email: String = ""
)

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
    val address: Addr
) : HeaderInterface

data class MailboxListHeader(
    override val name: String,
    override val value: String,
    val addresses: List<Addr>
) : HeaderInterface

data class DateHeader(
    override val name: String,
    override val value: String,
    val date: Date
) : HeaderInterface