package io.nade.email.parse.test

import io.nade.email.parse.Address
import io.nade.email.parse.ParsedMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class DecodeSimpleTest {

    private fun assertCommon(message: ParsedMessage) {
        assertEquals("Test Subject", message.subject)

        assertNotNull(message.date)
        assertEquals(1502972313000, message.date!!.time)

        assertEquals(Address(null, "from@email.com"), message.from)

        assertEquals(2, message.tos.size)
        assertEquals(Address(null, "to@email.com"), message.tos[0])
        assertEquals(Address("Name", "to2@email.com"), message.tos[1])

        assertEquals(2, message.ccs.size)
        assertEquals(Address("Name", "cc@email.com"), message.ccs[0])
        assertEquals(Address(null, "cc2@email.com"), message.ccs[1])

//        assertNotNull(message.returnPath)
//        assertEquals(message.returnPath, Address(nul, "return@path.com"))
    }

    @Test
    fun testSimpleText() {
        val message = parseFile(javaClass.getResourceAsStream("/email-sources/simple-text.eml"))
        assertCommon(message)

        assertNull(message.bodyHtml)
        assertNotNull(message.bodyText)
        assertEquals("This is a test email.\n\nThis is a test email.\n\nThis is a test email.\n", message.bodyText)
    }

    @Test
    fun testSimpleHtml() {
        val message = parseFile(javaClass.getResourceAsStream("/email-sources/simple-html.eml"))
        assertCommon(message)

        assertNull(message.bodyText)
        assertNotNull(message.bodyHtml)
        assertEquals("This is a <strong>test</strong> email.\n\nThis is a <strong>test</strong> email.\n\nThis is a <strong>test</strong> email.\n", message.bodyHtml)
    }

    @Test
    fun testSimpleMulti() {
        val message = parseFile(javaClass.getResourceAsStream("/email-sources/simple-multi.eml"))
        assertCommon(message)

        assertNotNull(message.bodyText)
        assertEquals("This is a test email.\n\nThis is a test email.\n\nThis is a test email.\n\nDOES end with a new line.\n", message.bodyText)

        assertNotNull(message.bodyHtml)
        assertEquals("This is a <strong>test</strong> email.\n\nThis is a <strong>test</strong> email.\n\nThis is a <strong>test</strong> email.\n\nDoes NOT end with a new line.", message.bodyHtml)
    }
}