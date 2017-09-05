package io.nade.email.parse.test

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvFileSource

class DecodeTest {

    @ParameterizedTest
    @CsvFileSource(resources = arrayOf("/test-emails.csv"))
    fun testEmails(inputEmailFile: String, expectedOutputFile: String) {
        println("Reading from: $inputEmailFile, expected results file: $expectedOutputFile")
    }
}