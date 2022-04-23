package dreifa.app.tasks.opentelemetry

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dreifa.app.opentelemetry.ZipKinOpenTelemetryProvider
import dreifa.app.opentelemetry.analyser
import dreifa.app.registry.Registry
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import io.opentelemetry.api.trace.Tracer
import org.junit.jupiter.api.Test

class OpenTelemetryScenarios {

    @Test
    fun `should do something`() {
        // 1. setup
        val (reg, provider, tracer) = init()

        // 2. test
        val task = EchoStringTask(reg)
        val ctx = SimpleExecutionContext()
        task.exec(ctx, "foo")

        // 3. verify
        val spansAnalyser = provider.spans().analyser()
        assertThat(spansAnalyser.traceIds().size, equalTo(1))
        assertThat(spansAnalyser.spanIds().size, equalTo(1))

        val spanAnalyser = spansAnalyser.firstSpan().analyser()
        assertThat(spanAnalyser.name , equalTo("EchoStringTask"))
    }

    private fun init(): Triple<Registry, ZipKinOpenTelemetryProvider, Tracer> {
        val reg = Registry()
        val provider = ZipKinOpenTelemetryProvider()
        val tracer = provider.provider().getTracer("OpenTelemetryScenarios")
        reg.store(provider).store(tracer)
        return Triple(reg, provider, tracer)
    }
}