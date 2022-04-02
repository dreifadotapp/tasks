package dreifa.app.tasks.opentelemtry

import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor


interface OpenTelemetryProvider {
    fun provider(): OpenTelemetrySdk
}

class SimpleOpenTelemetryProvider() : OpenTelemetryProvider {
    private val sdk :OpenTelemetrySdk = OpenTelemetrySdk.builder()
        .setTracerProvider(
            SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
                .build()
        )
        .build()

    override fun provider() = sdk
}