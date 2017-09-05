package io.nade.email.parse

import org.slf4j.Logger
import org.slf4j.MDC
import org.slf4j.event.Level
import java.text.SimpleDateFormat
import java.util.*

class ContextLogger(private val logger: Logger): Logger by logger {
    override fun warn(msg: String?) {
        saveLogLine(Level.WARN, msg)
        logger.warn(msg)
    }

    override fun info(msg: String?) {
        saveLogLine(Level.INFO, msg)
        logger.info(msg)
    }

    override fun error(msg: String?) {
        saveLogLine(Level.ERROR, msg)
        logger.error(msg)
    }

    override fun debug(msg: String?) {
        saveLogLine(Level.DEBUG, msg)
        logger.debug(msg)
    }

    override fun trace(msg: String?) {
        saveLogLine(Level.TRACE, msg)
        logger.trace(msg)
    }

    private fun saveLogLine(level: Level, msg: String?) {
        if (msg == null) {
            return
        }

        val contextId = MDC.get("emailParserContextId")
        if (contextId != null) {
            addLogLine(contextId, logger.name, level, msg)
        }
    }

    companion object {
        private val logLines: MutableMap<String, MutableList<LogLine>> = mutableMapOf()

        fun addLogLine(contextId: String, line: LogLine) {
            if (!logLines.containsKey(contextId)) {
                logLines[contextId] = mutableListOf()
            }

            logLines[contextId]!!.add(line)
        }

        fun addLogLine(contextId: String, loggerName: String, level: Level, message: String) {
            addLogLine(contextId, LogLine(loggerName, Date(), level, message))
        }

        fun getLogLines(contextId: String): List<LogLine> {
            return logLines[contextId]?.toList() ?: listOf()
        }

        fun clearContext(contextId: String) {
            logLines.remove(contextId)
        }
    }
}

val lineDateFormatter = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
fun List<LogLine>.toLogString(): String = this.joinToString("\n") { (loggerName, date, level, message) ->
    "[${lineDateFormatter.format(date)}] (${level.name}) $loggerName: $message"
}

data class LogLine(
    val loggerName: String,
    val date: Date,
    val level: Level,
    val message: String
)