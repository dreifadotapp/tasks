package dreifa.app.tasks.logging

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dreifa.app.opentelemetry.OpenTelemetryContext
import org.junit.Test
import java.io.PrintStream
import java.util.*

class LoggingContextTests {
    private val telemetryContext = OpenTelemetryContext.root()
    private val debug = LogMessage.debug("Debug Message", telemetryContext)
    private val info = LogMessage.info("Info Message", telemetryContext)
    private val warn = LogMessage.warn("Warning Message", telemetryContext)
    private val error = LogMessage.error("Error Message", telemetryContext)

    @Test
    fun `should capture print stream and stored as stdout`() {
        val logConsumerContext = InMemoryLogging()
        val captured = CapturedOutputStream(logConsumerContext)
        val ps = PrintStream(captured)

        ps.println("Hello World")
        ps.print("Goodbye, ")
        ps.println("cruel World")
        println(logConsumerContext.stdout())
        assertThat(logConsumerContext.stdout(), equalTo("Hello World\nGoodbye, cruel World\n"))
        assertThat(logConsumerContext.stderr(), equalTo(""))
    }

    @Test
    fun `should capture print stream and stored as stderr`() {
        val logConsumerContext = InMemoryLogging()
        val captured = CapturedOutputStream(logConsumerContext, false)
        val ps = PrintStream(captured)

        ps.println("Opps, it went wrong!")
        assertThat(logConsumerContext.stderr(), equalTo("Opps, it went wrong!\n"))
        assertThat(logConsumerContext.stdout(), equalTo(""))
    }

    @Test
    fun `should connect InMemoryLoggingConsumerContext to InMemoryLoggingProducerContext`() {
        // simulate both side in memory
        val logConsumerContext = InMemoryLogging()
        val logProducerContext = LoggingProducerToConsumer(logConsumerContext)

        // test stdout
        logProducerContext.stdout().println("Hello World")
        assertThat(logConsumerContext.stdout(), equalTo("Hello World\n"))

        // test stderr
        logProducerContext.stderr().println("Opps!")
        assertThat(logConsumerContext.stderr(), equalTo("Opps!\n"))

        // test log messages
        logProducerContext.logger().accept(info)
        logProducerContext.log(warn)
        assertThat(logConsumerContext.messages(), equalTo(listOf(info, warn)))
    }

//    @Test
//    fun `create logging context with default wiring`() {
//        // note, these go stdout and allow INFO and above
//        val loggingContext = InjectableLoggingProducerContext()
//        loggingContext.log(debug)
//        loggingContext.log(info)
//        loggingContext.log(warn)
//        loggingContext.log(error)
//    }

}