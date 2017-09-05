package io.nade.email.parse.cli

import io.nade.email.parse.Parser
import io.nade.email.parse.encode.DebugEncoder
import io.nade.email.parse.encode.JsonEncoder
import io.nade.email.parse.encode.MsgPackEncoder
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import java.io.File

class ParseEmailCommand(
    private val format: FormatOption,
    private val file: File,
    private val outFile: File?,
    private val isVerbose: Boolean = false
) {

    fun run() {
        val parser = Parser()
        val result = parser.parseToResult(file.inputStream())

        if (result.message == null) {
            println("There was a problem parsing the message")
            if (result.exception != null) {
                println(result.exception.toString())
            }
            println(result.log)
            System.exit(2)
            return
        }

        if (isVerbose) {
            println(result.log)
        }

        val message = result.message

        val encoder = when (format) {
            FormatOption.DEBUG   -> DebugEncoder.create()
            FormatOption.JSON    -> JsonEncoder.create()
            FormatOption.MSGPACK -> MsgPackEncoder.create()
        }

        if (outFile != null) {
            encoder.encodeToStream(message!!, outFile.outputStream())
        } else {
            encoder.encodeToOutput(message!!)
        }
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val parser = DefaultParser()

            val options = Options()
            options.addOption("m", "format", true, "The format to use: json, msgpack, debug. Defaults to debug.")
            options.addOption("h", "help", false, "Show help message")

            val fOpt = Option("f", "file", true, "Required. The path to the email file.")
            fOpt.argName = "FILE"
            options.addOption(fOpt)

            val foOpt = Option("o", "outfile", true, "Optionally a file to save the contents to.")
            foOpt.argName = "FILE"
            options.addOption(foOpt)

            options.addOption("v", "verbose", false, "Output debug log information to STDERR while parsing.")

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

            val formatOption = if (cmd.hasOption("m")) {
                try {
                    FormatOption.valueOf(cmd.getOptionValue("m").toUpperCase())
                } catch (e: IllegalArgumentException) {
                    println("Invalid format specified. Check out the help with --help.")
                    System.exit(1)
                    return
                }
            } else {
                FormatOption.DEBUG
            }

            if (!cmd.hasOption("f")) {
                println("You must specify the --file option. Check out the help with --help.")
                System.exit(1)
                return
            }

            val fileOption = File(cmd.getOptionValue("f"))
            if (!fileOption.exists()) {
                println("The --file you specified does not exist.")
                System.exit(1)
                return
            }
            if (!fileOption.isFile) {
                println("The --file you specified must be a plain file (i.e. not a directory).")
                System.exit(1)
                return
            }

            val outfileOption = if (cmd.hasOption("o")) {
                File(cmd.getOptionValue("o"))
            } else {
                null
            }

            ParseEmailCommand(
                formatOption,
                fileOption,
                outfileOption,
                cmd.hasOption("v")
            ).run()
        }
    }
}

enum class FormatOption {
    DEBUG, JSON, MSGPACK
}