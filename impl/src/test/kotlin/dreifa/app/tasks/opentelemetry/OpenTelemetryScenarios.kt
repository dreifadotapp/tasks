package dreifa.app.tasks.opentelemetry

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dreifa.app.opentelemetry.OpenTelemetryContext
import dreifa.app.opentelemetry.ZipKinOpenTelemetryProvider
import dreifa.app.opentelemetry.analyser
import dreifa.app.registry.Registry
import dreifa.app.tasks.TaskFactory
import dreifa.app.tasks.client.SimpleClientContext
import dreifa.app.tasks.client.SimpleTaskClient
import dreifa.app.tasks.demo.DemoTasks
import dreifa.app.tasks.demo.echo.EchoTasks
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.tasks.logging.DefaultLoggingChannelFactory
import dreifa.app.tasks.logging.InMemoryLoggingRepo
import dreifa.app.tasks.logging.LoggingChannelLocator
import dreifa.app.tasks.logging.LoggingReaderFactory
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.sdk.trace.data.StatusData
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
        val clientContext = SimpleClientContext(telemetryContext = traceCtx)

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
        val clientContext = SimpleClientContext(telemetryContext = traceCtx)

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
        assertThat( reader.messages()[0].body, equalTo("Task 'ExceptionGeneratingBlockingTask' threw exception: 'This will create an Exception' [RuntimeException]"))

        // 4. verify telemetry
        val spansAnalyser = provider.spans().analyser()
        assertThat(spansAnalyser.traceIds().size, equalTo(1))
        assertThat(spansAnalyser.spanIds().size, equalTo(2))
        val taskSpan = spansAnalyser.secondSpan()
        assertThat(taskSpan.name, equalTo("ExceptionGeneratingBlockingTask"))
        assertThat(taskSpan.kind, equalTo(SpanKind.SERVER))
        assert(taskSpan.parentSpanId != Span.getInvalid().toString())
        assertThat(taskSpan.status, equalTo(StatusData.error()))
    }

    @AfterAll
    fun `wait for zipkin`() {
        // give it time to flush to zipkin before closing
        Thread.sleep(50)
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


    private fun init(): Triple<Registry, ZipKinOpenTelemetryProvider, Tracer> {
        val reg = Registry()
        val provider = ZipKinOpenTelemetryProvider()
        val tracer = provider.sdk().getTracer("OpenTelemetryScenarios")
        val inMemoryLogging = InMemoryLoggingRepo()
        reg.store(provider).store(tracer).store(taskFactory).store(inMemoryLogging)

        // is this needed ?
        val logChannelFactory = DefaultLoggingChannelFactory(reg)
        reg.store(logChannelFactory)

        return Triple(reg, provider, tracer)
    }
}