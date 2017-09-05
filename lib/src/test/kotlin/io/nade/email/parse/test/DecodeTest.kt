package io.nade.email.parse.test

import mu.KLogging
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvFileSource

class DecodeTest {
    companion object: KLogging()

    @ParameterizedTest
    @CsvFileSource(resources = arrayOf("/test-emails.csv"))
    fun testEmails(inputEmailFile: String, expectedOutputFile: String) {
        logger.debug { "Reading from: $inputEmailFile, expected results file: $expectedOutputFile" }
    }
}