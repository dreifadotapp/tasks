package dreifa.app.tasks.opentelemtry

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.SpanLimits
import io.opentelemetry.sdk.trace.data.LinkData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.sdk.trace.samplers.SamplingDecision
import io.opentelemetry.sdk.trace.samplers.SamplingResult

class Config {
    private val err = System.err

    fun main() {

        var openTelemetrySdk: OpenTelemetrySdk = OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
                    .build()
            )
            .build()
        printSpanLimits(openTelemetrySdk)

        // OpenTelemetry has a maximum of 128 Attributes by default for Spans, Links, and Events.
        // OpenTelemetry has a maximum of 128 Attributes by default for Spans, Links, and Events.
        var tracer: Tracer = openTelemetrySdk.getTracer("ConfigureTraceExample")
        val multiAttrSpan: Span = tracer.spanBuilder("Example Span Attributes").startSpan()
        multiAttrSpan.setAttribute("Attribute 1", "first attribute value")
        multiAttrSpan.setAttribute("Attribute 2", "second attribute value")
        multiAttrSpan.end()


        // The configuration can be changed in the trace provider.
        // For example, we can change the maximum number of Attributes per span to 1.
        // The configuration can be changed in the trace provider.
        // For example, we can change the maximum number of Attributes per span to 1.
        val newConf = SpanLimits.builder().setMaxNumberOfAttributes(1).build()

        openTelemetrySdk = OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
                    .setSpanLimits(newConf)
                    .build()
            )
            .build()
        printSpanLimits(openTelemetrySdk)

        // If more attributes than allowed by the configuration are set, they are dropped.
        // If more attributes than allowed by the configuration are set, they are dropped.
        tracer = openTelemetrySdk.getTracer("ConfigureTraceExample")
        val singleAttrSpan = tracer.spanBuilder("Example Span Attributes").startSpan()
        singleAttrSpan.setAttribute("Attribute 1", "first attribute value")
        singleAttrSpan.setAttribute("Attribute 2", "second attribute value")
        singleAttrSpan.end()

        // OpenTelemetry offers three different default samplers:
        //  - alwaysOn: it samples all traces
        //  - alwaysOff: it rejects all traces
        //  - probability: it samples traces based on the probability passed in input
        // OpenTelemetry offers three different default samplers:
        //  - alwaysOn: it samples all traces
        //  - alwaysOff: it rejects all traces
        //  - probability: it samples traces based on the probability passed in input
        val traceIdRatioBased = Sampler.traceIdRatioBased(0.5)

        // We build an SDK with the alwaysOff sampler.

        // We build an SDK with the alwaysOff sampler.
        openTelemetrySdk = OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
                    .setSampler(Sampler.alwaysOff())
                    .build()
            )
            .build()
        printSpanLimits(openTelemetrySdk)

        tracer = openTelemetrySdk.getTracer("ConfigureTraceExample");
        tracer.spanBuilder("Not forwarded to any processors").startSpan().end();
        tracer.spanBuilder("Not forwarded to any processors").startSpan().end();

        // We build an SDK with the alwaysOn sampler.
        openTelemetrySdk =
            OpenTelemetrySdk.builder()
                .setTracerProvider(
                    SdkTracerProvider.builder()
                        .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
                        .setSampler(Sampler.alwaysOn())
                        .build())
                .build();
        printSpanLimits(openTelemetrySdk);

        tracer = openTelemetrySdk.getTracer("ConfigureTraceExample");
        tracer.spanBuilder("Forwarded to all processors").startSpan().end();
        tracer.spanBuilder("Forwarded to all processors").startSpan().end();

        // We build an SDK with the configuration to use the probability sampler which was configured to
        // sample
        // only 50% of the spans.
        // We build an SDK with the configuration to use the probability sampler which was configured to
        // sample
        // only 50% of the spans.
        openTelemetrySdk = OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
                    .setSampler(traceIdRatioBased)
                    .build()
            )
            .build()
        printSpanLimits(openTelemetrySdk)

        tracer = openTelemetrySdk.getTracer("ConfigureTraceExample")
        for (i in 0..9) {
            tracer
                .spanBuilder(String.format("Span %d might be forwarded to all processors", i))
                .startSpan()
                .end()
        }

        // Add MySampler to the Trace Configuration
        // Add MySampler to the Trace Configuration
        openTelemetrySdk = OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
                    .setSampler(MySampler())
                    .build()
            )
            .build()
        printSpanLimits(openTelemetrySdk)

        tracer = openTelemetrySdk.getTracer("ConfigureTraceExample")

        tracer.spanBuilder("#1 - SamPleD").startSpan().end()
        tracer
            .spanBuilder("#2 - SAMPLE this trace will be the first to be printed in the console output")
            .startSpan()
            .end()
        tracer.spanBuilder("#3 - Smth").startSpan().end()
        tracer
            .spanBuilder("#4 - SAMPLED this trace will be the second one shown in the console output")
            .startSpan()
            .end()
        tracer.spanBuilder("#5").startSpan().end()

    }

    private fun printSpanLimits(sdk: OpenTelemetrySdk) {
        val config = sdk.sdkTracerProvider.spanLimits
        err.println("==================================")
        err.print("Max number of attributes: ")
        err.println(config.maxNumberOfAttributes)
        err.print("Max number of attributes per event: ")
        err.println(config.maxNumberOfAttributesPerEvent)
        err.print("Max number of attributes per link: ")
        err.println(config.maxNumberOfAttributesPerLink)
        err.print("Max number of events: ")
        err.println(config.maxNumberOfEvents)
        err.print("Max number of links: ")
        err.println(config.maxNumberOfLinks)
        err.print("Sampler: ")
        err.println(sdk.sdkTracerProvider.sampler.description)
    }

    // We can also implement our own sampler. We need to implement the
    // io.opentelemetry.sdk.trace.Sampler interface.
    internal class MySampler : Sampler {
        override fun shouldSample(
            parentContext: Context?,
            traceId: String?,
            name: String,
            spanKind: SpanKind?,
            attributes: Attributes?,
            parentLinks: List<LinkData?>?
        ): SamplingResult {
            return SamplingResult.create(
                if (name.contains("SAMPLE")) SamplingDecision.RECORD_AND_SAMPLE else SamplingDecision.DROP
            )
        }

        override fun getDescription(): String {
            return "My Sampler Implementation!"
        }
    }
}

fun main() {
    Config().main()
}