package dreifa.app.tasks.opentelemtry

import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor


interface OpenTelemetryProvider {
    fun provider(): OpenTelemetrySdk
}

class InMemoryOpenTelemetryProvider() : OpenTelemetryProvider {
    private val inMemory = InMemorySpanExporter()

    private val sdk: OpenTelemetrySdk = OpenTelemetrySdk.builder()
        .setTracerProvider(
            SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(inMemory))
                .build()
        )
        .build()

    override fun provider() = sdk

    fun spans() = inMemory.allSpans
}