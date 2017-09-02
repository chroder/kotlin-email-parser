package io.nade.email.parse.test

import io.nade.email.parse.ParsedMessage
import io.nade.email.parse.Parser
import java.io.File
import java.io.InputStream

fun parseFile(istream: InputStream): ParsedMessage {
    val parser = Parser.create()
    return parser.parse(istream)
}

fun parseFile(file: File): ParsedMessage = parseFile(file.inputStream())