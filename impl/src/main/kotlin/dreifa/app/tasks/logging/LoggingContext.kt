package dreifa.app.tasks.logging

import dreifa.app.helpers.random
import dreifa.app.opentelemetry.OpenTelemetryContext
import dreifa.app.registry.Registry
import dreifa.app.tasks.*
import dreifa.app.tasks.client.ClientContext
import java.io.OutputStream
import java.io.PrintStream
import java.util.*
import java.util.function.Consumer
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * A common logging message and supporting classes plus access to stdout and stderr. The basis concept
 * is that any Task write and consumes log messages in a standard way, but underneath different logging
 * frameworks can be plugged in. Although not yet included, the plan is to integrate in distributed
 * tracing concepts for use with tools like Zipkin (https://zipkin.io/).
 *
 * On the producer side there is [dreifa.app.tasks.logging.LoggingProducerContext]
 * On the consumer (client) side there is [LoggingConsumerContext]
 *
 * The design allows for the producer and consumer to be running in separate processes
 * with communication over commons protocols including https and Kafka.
 *
 * For local development and unit tests there is `InMemoryLoggingProducerContext` and `InMemoryLoggingConsumerContext`
 *
 */


/**
 * The Logging Context for generation of output.
 * This is passed to the task's exec() method in the ExecutionContext
 */
interface LoggingProducerContext {
    /**
     * Abstract generating a log message
     */
    fun logger(): LogMessageConsumer

    /**
     * Abstract writing to the console
     */
    fun stdout(): PrintStream

    /**
     * Abstract writing to the error stream
     */
    fun stderr(): PrintStream

    /**
     * Shortcut for writing a log message. The message must be fully
     * constructed manually in order to correctly record within the distributed
     * logging framework
     */
    fun log(msg: LogMessage) {
        logger().accept(msg)
    }

    fun log(clientContext: ClientContext, msg: LogMessage) {
        logger().accept(msg)
    }
}

/**
 * The consumer of a logging context. This is on the client
 * side. There is a design assumption that the 'channel' linking the
 * the LoggingProducerContext to the LoggingConsumerContext will be as
 * timely as is reasonably possible.
 */
interface LoggingConsumerContext {
    fun acceptLog(msg: LogMessage)
    fun acceptStdout(output: String)
    fun acceptStderr(error: String)
}

/**
 * Logging information can be sent back to the client  As the tasks can be long running
 * and may live between restarts, then there needs to a way for the server side to know how to
 * recreate the LoggingConsumerContext associated with the channel when the original request was made.
 *
 * There are some assumptions and conventions here:
 *   - the first part of the string specifies the type of channel,
 *     for example "LOCAL:", "REST:", "AWSSMS:"
 *   - the second part of the string contains a unique channel id
 *   - we don't pass security credentials here, the factory must know how to establish a secure
 *     connection
 *   - an implementation of LogChannelFactory will
 *     know how to create a suitable concrete implementations of AsyncResultChannelSource
 *     from the Locator
 *
 */
data class LoggingChannelLocator(val locator: String) {
    companion object {
        fun inMemory(channelId: String = String.random(length = 6)) = LoggingChannelLocator("INMEMORY;${channelId}")
        fun console() = LoggingChannelLocator("CONSOLE;")
    }
}

/**
 * Given a AsyncResultChannelSourceLocator, return the actual channel
 */
interface LoggingChannelFactory {
    fun consumer(locator: LoggingChannelLocator): LoggingConsumerContext
}

interface LoggingReaderFactory {
    fun query(locator: LoggingChannelLocator): LoggingReaderContext
}

class InMemoryLoggingRepo {
    private val inMemoryLookup = HashMap<String, InMemoryLogging>()

    fun consume(channelId: String): LoggingConsumerContext = lookupInMemoryConsumer(channelId)

    fun query(channelId: String): LoggingReaderContext = lookupInMemoryConsumer(channelId)

    fun lookup(channelId: String): InMemoryLogging = lookupInMemoryConsumer((channelId))


    private fun lookupInMemoryConsumer(channelId: String): InMemoryLogging {
        if (!inMemoryLookup.containsKey(channelId)) {
            inMemoryLookup[channelId] = InMemoryLogging()
        }
        return inMemoryLookup[channelId]!!
    }
}

/**
 * This needs some thought. Can we limit it to just the "LOCAL" channel?
 */
class DefaultLoggingChannelFactory(registry: Registry) : LoggingChannelFactory, LoggingReaderFactory {
    private val inMemoryLoggingRepo = registry.getOrElse(InMemoryLoggingRepo::class.java, InMemoryLoggingRepo())
    private val consoleContext = ConsoleLoggingConsumerContext()

    override fun consumer(locator: LoggingChannelLocator): LoggingConsumerContext {
        if (locator.locator.startsWith("INMEMORY;")) {
            return lookupInMemoryConsumer(locator.locator)

        } else if (locator.locator == "CONSOLE;") {
            return consoleContext
        } else {
            throw RuntimeException("Don't know about $locator")
        }
    }

    override fun query(locator: LoggingChannelLocator): LoggingReaderContext {
        if (locator.locator.startsWith("INMEMORY;")) {
            return lookupInMemoryConsumer(locator.locator)
        } else {
            throw RuntimeException("Don't know about $locator")
        }
    }

    private fun lookupInMemoryConsumer(locator: String): InMemoryLogging {
        val id = locator.split(";")[1]
        return inMemoryLoggingRepo.lookup(id)
    }
}

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

/**
 * There are a fixed set of format supported. If something custom
 * is needed, the downstream system is responsible
 */
enum class LogFormat {
    Full, /* All fields in Java style */
    Test,   /* Timestamp excluded and IDs masked, good for use in test cases*/
    Simple, /* A short format. Useful for test cases */
}

/**
 * Format the log as string
 */
interface StringLogFormatter {
    fun toString(msg: LogMessage, format: LogFormat): String
}

/**
 * A standard String format that is human readable
 */
class DefaultStringLogFormatter : StringLogFormatter {
    override fun toString(msg: LogMessage, format: LogFormat): String {
        return when (format) {
            LogFormat.Simple -> {
                val buffer = StringBuilder()
                buffer.append(msg.level)
                buffer.append(" ")
                buffer.append(msg.body)
                return buffer.toString()
            }
            LogFormat.Test -> {
                val buffer = StringBuilder()
                buffer.append("level=${msg.level}, message=${msg.body}")
                buffer.toString()
            }
            LogFormat.Full -> {
                val buffer = StringBuilder()
                buffer.append("level=${msg.level}, message=${msg.body}, traceId=${msg.openTelemetryContext.traceId}")
                buffer.append(", timestamp=${msg.timestamp}")
                buffer.toString()
            }
        }
    }
}

/**
 * The common attributes in a single log message
 */
data class LogMessage(

    val openTelemetryContext: OpenTelemetryContext,

    val level: LogLevel,

    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
) {
    companion object {
        fun debug(
            body: String,
            openTelemetryContext: OpenTelemetryContext = OpenTelemetryContext.root()
        ): LogMessage = LogMessage(
            openTelemetryContext = openTelemetryContext,
            body = body,
            level = LogLevel.DEBUG
        )

        fun info(
            body: String,
            openTelemetryContext: OpenTelemetryContext = OpenTelemetryContext.root()
        ): LogMessage = LogMessage(
            openTelemetryContext = openTelemetryContext,
            body = body,
            level = LogLevel.INFO
        )

        fun warn(
            body: String,
            openTelemetryContext: OpenTelemetryContext = OpenTelemetryContext.root()
        ): LogMessage = LogMessage(
            openTelemetryContext = openTelemetryContext,
            body = body,
            level = LogLevel.WARN
        )

        fun error(
            body: String,
            openTelemetryContext: OpenTelemetryContext = OpenTelemetryContext.root()
        ): LogMessage = LogMessage(
            openTelemetryContext = openTelemetryContext,
            body = body,
            level = LogLevel.ERROR
        )
    }
}


/**
 * We always write to a sink.
 */
interface LogMessageConsumer : Consumer<LogMessage>

/**
 * The very basic default sink that simply writes to the console. All configuration is
 * via the registry. If settings are missing, the following defaults are used:
 *
 *  - format is LogFormat.Simple
 *  - formatter is DefaultStringLogFormatter
 */
class ConsoleLogMessageConsumer(registry: Registry = Registry()) :
    LogMessageConsumer {
    private val formatter = registry.getOrElse(StringLogFormatter::class.java, DefaultStringLogFormatter())
    private val format = registry.getOrElse(LogFormat::class.java, LogFormat.Simple)
    override fun accept(msg: LogMessage) {
        println(formatter.toString(msg, format))
    }
}

class ConsoleLoggingConsumerContext() :
    LoggingConsumerContext {
    override fun acceptLog(msg: LogMessage) {
        println(msg)
    }

    override fun acceptStdout(output: String) {
        print(output)
    }

    override fun acceptStderr(error: String) {
        System.err.print(error)
    }
}

/**
 * A standard interface for readers of the logging
 */
interface LoggingReaderContext {
    fun stdout(): String
    fun stderr(): String
    fun messages(): List<LogMessage>
}

class InMemoryLogging : LoggingConsumerContext, LoggingReaderContext {
    private val stdout = StringBuilder()
    private val stderr = StringBuilder()
    private val logMessages = ArrayList<LogMessage>()

    override fun acceptLog(msg: LogMessage) {
        logMessages.add(msg)
    }

    override fun acceptStdout(output: String) {
        stdout.append(output).append("\n")
    }

    override fun acceptStderr(error: String) {
        stderr.append(error).append("\n")
    }

    override fun stdout(): String = stdout.toString()

    override fun stderr(): String = stderr.toString()

    override fun messages(): List<LogMessage> = ArrayList(logMessages)
}

/**
 * Link a logging producer to a consumer.
 * This is the common pattern for implementing a LoggingProducerContext - simply connect it to
 * consumer
 */
class LoggingProducerToConsumer(private val consumer: LoggingConsumerContext) : LoggingProducerContext {
    private val stdout: PrintStream = PrintStream(CapturedOutputStream(consumer, true))
    private val stderr: PrintStream = PrintStream(CapturedOutputStream(consumer, false))

    override fun logger(): LogMessageConsumer =
        object : LogMessageConsumer {
            override fun accept(m: LogMessage) {
                consumer.acceptLog(m)
            }
        }

    override fun stdout(): PrintStream = stdout

    override fun stderr(): PrintStream = stderr
}


/**
 * Simple class for capturing an input stream and passing onto a
 * LoggingConsumerContext.
 */
class CapturedOutputStream(
    private val loggingConsumerContext: LoggingConsumerContext,
    private val isStdout: Boolean = true
) : OutputStream() {
    private var data = StringBuffer()

    override fun write(p0: Int) {
        if (p0 == 10) { // newline
            if (isStdout) {
                loggingConsumerContext.acceptStdout(data.toString())
            } else {
                loggingConsumerContext.acceptStderr(data.toString())
            }
            data = StringBuffer()
        } else {
            data.append(p0.toChar())
        }
    }
}


/**
 * Everything is directed to stdout & stderr
 */
class ConsoleLoggingProducerContext : LoggingProducerContext {
    private val sink = ConsoleLogMessageConsumer()

    override fun logger(): LogMessageConsumer = sink

    override fun stdout(): PrintStream = System.out

    override fun stderr(): PrintStream = System.err
}

