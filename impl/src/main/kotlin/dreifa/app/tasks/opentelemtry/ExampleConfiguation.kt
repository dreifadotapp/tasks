package dreifa.app.tasks.opentelemtry

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes


object ExampleConfiguration {
    // Name of the service
    private const val SERVICE_NAME = "myExampleService"

    /** Adds a SimpleSpanProcessor initialized with ZipkinSpanExporter to the TracerSdkProvider  */
    fun initializeOpenTelemetry(ip: String?, port: Int): OpenTelemetry {
        val endpoint = String.format("http://%s:%s/api/v2/spans", ip, port)
        val zipkinExporter: ZipkinSpanExporter = ZipkinSpanExporter.builder().setEndpoint(endpoint).build()
        val serviceNameResource: Resource =
            Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, SERVICE_NAME))

        // Set to process the spans by the Zipkin Exporter
        val tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(zipkinExporter))
            .setResource(Resource.getDefault().merge(serviceNameResource))
            .build()
        val openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).buildAndRegisterGlobal()

        // add a shutdown hook to shut down the SDK
        Runtime.getRuntime().addShutdownHook(Thread { tracerProvider.close() })

        // return the configured instance so it can be used for instrumentation.
        return openTelemetry
    }
}