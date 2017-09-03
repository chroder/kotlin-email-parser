package io.nade.email.parse

import java.util.*

data class ParseResult(
    val message: ParsedMessage?,
    val log: String,
    val exception: Exception?,
    val contextId: String
)

data class ParsedMessage(
    val subject: String,
    val messageId: String? = null,
    val from: Addr? = null,
    val sender: Addr? = null,
    val replyTo: List<Addr>,
    val returnPath: Addr? = null,
    val tos: List<Addr>,
    val ccs: List<Addr>,
    val date: Date?,
    val references: List<String>,
    val bodyText: String? = null,
    val bodyHtml: String? = null,
    val headers: List<HeaderInterface>,
    val size: Int = 0
) {
    fun getReadableSize(si: Boolean = true): String {
        val unit = if (si) 1000 else 1024
        if (size < unit) return size.toString() + " B"
        val exp = (Math.log(size.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i"
        return String.format("%.1f %sB", size / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }
}

interface HeaderInterface {
    val name: String
    val value: String
}

data class Addr(
    val name: String? = "",
    val email: String
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
    val addr: Addr
) : HeaderInterface

data class MailboxListHeader(
    override val name: String,
    override val value: String,
    val addrs: List<Addr>
) : HeaderInterface

data class DateHeader(
    override val name: String,
    override val value: String,
    val date: Date
) : HeaderInterface