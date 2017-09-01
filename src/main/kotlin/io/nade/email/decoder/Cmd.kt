package io.nade.email.decoder

import com.google.gson.GsonBuilder
import java.io.FileInputStream

fun main(args: Array<String>) {
    val filePath = "/Users/chroder/Downloads/original_msg-1.txt"
    val istream  = FileInputStream(filePath)
    val decoder  = Decoder.create()
    val message  = decoder.decode(istream)

    val gson = GsonBuilder()
        .serializeNulls()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .create()
    val json = gson.toJson(message)
    println(json)
}

