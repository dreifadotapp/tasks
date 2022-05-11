package dreifa.app.tasks.opentelemetry

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dreifa.app.opentelemetry.*
import dreifa.app.registry.Registry
import dreifa.app.tasks.TaskFactory
import dreifa.app.tasks.client.SimpleClientContext
import dreifa.app.tasks.client.SimpleTaskClient
import dreifa.app.tasks.demo.DemoTasks
import dreifa.app.tasks.demo.echo.EchoTasks
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.tasks.logging.DefaultLoggingChannelFactory
import dreifa.app.tasks.logging.InMemoryLoggingRepo
import dreifa.app.tasks.logging.LoggingReaderFactory
import dreifa.app.types.CorrelationContexts
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.trace.data.StatusData
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenTelemetryScenarios {
    private val taskFactory = TaskFactory().register(DemoTasks()).register(EchoTasks())

    @Test
    fun `should create new span`() {
        // 1. setup
        val (reg, provider, _) = init()

        // 2. test
        val task = EchoStringTask(reg)
        val ctx = SimpleExecutionContext()
        task.exec(ctx, "foo")

        // 3. verify
        val spansAnalyser = provider.spans().analyser()
        assertThat(spansAnalyser.traceIds().size, equalTo(1))
        assertThat(spansAnalyser.spanIds().size, equalTo(1))

        val spanAnalyser = spansAnalyser.firstSpan().analyser()
        assertThat(spanAnalyser.name, equalTo("EchoStringTask"))
    }

    @Test
    fun `should create new span when calling task client`() {
        val (reg, provider, tracer) = init()
        val taskClient = SimpleTaskClient(reg)

        val outerSpan = outerSpan(tracer)
        val traceCtx = OpenTelemetryContext.fromSpan(outerSpan)
        val clientContext = SimpleClientContext(telemetryContext = traceCtx.dto())

        taskClient.execBlocking(
            clientContext,
            "dreifa.app.tasks.demo.echo.EchoStringTask",
            "Hello, world",
            String::class
        )
        completeSpan(outerSpan)

        // 3. verify
        val spansAnalyser = provider.spans().analyser()
        assertThat(spansAnalyser.traceIds().size, equalTo(1))
        assertThat(spansAnalyser.spanIds().size, equalTo(2))
        val taskSpan = spansAnalyser.secondSpan()
        assertThat(taskSpan.name, equalTo("EchoStringTask"))
        assertThat(taskSpan.kind, equalTo(SpanKind.SERVER))
        assert(taskSpan.parentSpanId != Span.getInvalid().toString())
        assertThat(taskSpan.status, equalTo(StatusData.ok()))
    }

    @Test
    fun `should create new span when calling failing task via task client`() {
        val (reg, provider, tracer) = init()
        val taskClient = SimpleTaskClient(reg)

        val outerSpan = outerSpan(tracer)
        val traceCtx = OpenTelemetryContext.fromSpan(outerSpan)
        val clientContext = SimpleClientContext(telemetryContext = traceCtx.dto())

        try {
            taskClient.execBlocking(
                clientContext,
                "dreifa.app.tasks.demo.ExceptionGeneratingBlockingTask",
                "This will create an Exception",
                String::class
            )
        } catch (ignoreMe: Exception) {
        }
        completeSpan(outerSpan)

        // 3. verify logging channel
        val logReaderFactory = reg.get(LoggingReaderFactory::class.java)
        val reader = logReaderFactory.query(clientContext.logChannelLocator())
        assertThat(
            reader.messages()[0].body,
            equalTo("Task 'ExceptionGeneratingBlockingTask' threw exception: 'This will create an Exception' [RuntimeException]")
        )

        // 4. verify telemetry
        val spansAnalyser = provider.spans().analyser()
        assertThat(spansAnalyser.traceIds().size, equalTo(1))
        assertThat(spansAnalyser.spanIds().size, equalTo(2))
        val taskSpan = spansAnalyser.secondSpan()
        assertThat(taskSpan.name, equalTo("ExceptionGeneratingBlockingTask"))
        assertThat(taskSpan.kind, equalTo(SpanKind.SERVER))
        assert(taskSpan.parentSpanId != Span.getInvalid().toString())
        assertThat(taskSpan.status, equalTo(StatusData.create(StatusCode.ERROR, "This will create an Exception")))
    }

    @Test
    fun `should include correlation data in telemetry`() {
        val (reg, provider, tracer) = init()
        val taskClient = SimpleTaskClient(reg)

        val outerSpan = outerSpan(tracer)
        val traceCtx = OpenTelemetryContext.fromSpan(outerSpan)
        val correlation = CorrelationContexts.single("testid", "abc123")
        val clientContext = SimpleClientContext(telemetryContext = traceCtx.dto(), correlation = correlation)

        taskClient.execBlocking(
            clientContext,
            "dreifa.app.tasks.demo.echo.EchoStringTask",
            "Hello, world",
            String::class
        )
        completeSpan(outerSpan)

        // 3. verify
        val spansAnalyser = provider.spans().analyser()
        assertThat(spansAnalyser.traceIds().size, equalTo(1))
        assertThat(spansAnalyser.spanIds().size, equalTo(2))
        val clientSpan = spansAnalyser.firstSpan()
        assertThat(clientSpan.name, equalTo("DummyClient"))
        val taskSpan = spansAnalyser.secondSpan()
        assertThat(taskSpan.name, equalTo("EchoStringTask"))
        assertThat(taskSpan.kind, equalTo(SpanKind.SERVER))
        assert(taskSpan.parentSpanId != Span.getInvalid().toString())
        assertThat(taskSpan.status, equalTo(StatusData.ok()))
        assertThat(taskSpan.attributes.size(), equalTo(1))
        assertThat(taskSpan.attributes.get(AttributeKey.stringKey("dreifa.correlation.testid")), equalTo("abc123"))
    }

    @AfterAll
    fun `wait to flush telemetry`() {
        // give some time to flush to the exporter before closing
        Thread.sleep(100)
    }

    private fun outerSpan(tracer: Tracer): Span {
        return tracer.spanBuilder("DummyClient")
            .setSpanKind(SpanKind.CLIENT)
            .startSpan()
    }

    private fun completeSpan(span: Span) {
        span.setStatus(StatusCode.OK)
        span.end()
    }

    private fun init(): Triple<Registry, OpenTelemetryProvider, Tracer> {
        val reg = Registry()
        val provider = JaegerOpenTelemetryProvider(true)
        val tracer = provider.sdk().getTracer("OpenTelemetryScenarios")
        val inMemoryLogging = InMemoryLoggingRepo()
        reg.store(provider).store(tracer).store(taskFactory).store(inMemoryLogging)

        // is this needed ?
        val logChannelFactory = DefaultLoggingChannelFactory(reg)
        reg.store(logChannelFactory)

        return Triple(reg, provider, tracer)
    }
}