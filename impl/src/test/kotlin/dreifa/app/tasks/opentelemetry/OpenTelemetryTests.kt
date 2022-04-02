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
import dreifa.app.tasks.opentelemtry.SimpleOpenTelemetryProvider
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import org.junit.jupiter.api.Test

class OpenTelemetryTests {
    private val registry = Registry()
    private val taskFactory = TaskFactory().register(DemoTasks()).register(EchoTasks())
    private val logChannelFactory = DefaultLoggingChannelFactory(registry)

    private val provider = SimpleOpenTelemetryProvider()

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
        var tracer: Tracer = provider.provider().getTracer("dreifa.app.tasks.Tracer")
        val multiAttrSpan: Span = tracer.spanBuilder("Example Span Attributes").startSpan()
        multiAttrSpan.setAttribute("Attribute 1", "first attribute value")
        multiAttrSpan.setAttribute("Attribute 2", "second attribute value")
        multiAttrSpan.addEvent("something happened!! ")
        multiAttrSpan.end()

    }
}