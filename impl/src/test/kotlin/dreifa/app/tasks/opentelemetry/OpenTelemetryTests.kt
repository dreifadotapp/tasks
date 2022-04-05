package dreifa.app.tasks.opentelemetry

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dreifa.app.registry.Registry
import dreifa.app.tasks.TaskFactory
import dreifa.app.tasks.client.SimpleClientContext
import dreifa.app.tasks.client.SimpleTaskClient
import dreifa.app.tasks.demo.DemoTasks
import dreifa.app.tasks.demo.echo.EchoTasks
import dreifa.app.tasks.logging.DefaultLoggingChannelFactory
import dreifa.app.tasks.opentelemtry.InMemoryOpenTelemetryProvider
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import org.junit.jupiter.api.Test

class OpenTelemetryTests {
    private val registry = Registry()
    private val taskFactory = TaskFactory().register(DemoTasks()).register(EchoTasks())
    private val logChannelFactory = DefaultLoggingChannelFactory(registry)
    private val provider = InMemoryOpenTelemetryProvider()

    init {
        registry.store(taskFactory).store(logChannelFactory)
    }

    @Test
    fun `should trace request end to end`() {
        val clientContext = SimpleClientContext()
        val result = SimpleTaskClient(registry).execBlocking(
            clientContext,
            "dreifa.app.tasks.demo.echo.EchoStringTask",
            "Hello, world",
            String::class
        )

        assertThat(result, equalTo("Hello, world"))
        //assertNoOutput(clientContext)

    }

    @Test
    fun `should log something!`() {
//        val outOut = System.err
//        val x = ByteArrayOutputStream()
//        System.setErr(PrintStream( x))


        var tracer: Tracer = provider.provider().getTracer("dreifa.app.tasks.Tracer")

        val outerSpan: Span = tracer.spanBuilder("Client").setSpanKind(SpanKind.CLIENT).startSpan()

        val taskSpan: Span = tracer.spanBuilder("TaskName")
            .setSpanKind(SpanKind.SERVER)
            .addLink(outerSpan.spanContext)
            .startSpan()

        taskSpan.setAttribute("Attr 1", "first attribute value")
        taskSpan.setAttribute("Att2 2", "second attribute value")
        taskSpan.addEvent("something happened!! ")
        //Thread.sleep(1)

        taskSpan.end()
        outerSpan.end()

        provider.spans().forEach {
            println("A span")
            //println(it)
            println(it.parentSpanContext)
            println(it.name)
            println(it.startEpochNanos)
            println(it.endEpochNanos - it.startEpochNanos)
            println(it.kind)

        }
    }
}