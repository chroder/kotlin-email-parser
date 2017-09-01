package io.nade.email.parse.cli

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option

//fun main(args: Array<String>) {
//    val filePath = "/Users/chroder/Downloads/original_msg-1.txt"
//    val istream  = FileInputStream(filePath)
//    val decoder  = Parser.create()
//    val message  = decoder.parse(istream)
//
//    val gson = GsonBuilder()
//        .serializeNulls()
//        .setPrettyPrinting()
//        .setDateFormat("yyyy-MM-dd HH:mm:ss")
//        .create()
//    val json = gson.toJson(message)
//    println(json)
//}

class ParseEmailCommand(file: String) {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val parser = DefaultParser()

            val options = Options()
            options.addOption("m", "format", true, "The format to use: json, msgpack, debug. Defaults to debug.")
            options.addOption("h", "help", false, "Show help message")

            val fOpt = Option("f", "file", true, "Required. The path to the email file.")
            fOpt.argName = "FILE"
            options.addOption(fOpt)

            val cmd = try {
                parser.parse(options, args)
            } catch (e: Exception) {
                println("There was a problem trying to parse your command-line options. Check out the help with --help.")
                System.exit(1)
                return
            }

            if (cmd.hasOption("h")) {
                val formatter = HelpFormatter()
                formatter.printHelp("parse-email", "Decode an email into a simple data structure", options, "", true)
                System.exit(0)
                return
            }
        }
    }
}