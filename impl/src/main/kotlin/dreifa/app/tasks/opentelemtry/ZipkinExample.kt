package dreifa.app.tasks.opentelemtry

import dreifa.app.tasks.opentelemtry.ExampleConfiguration.initializeOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.api.trace.TracerProvider


class ZipkinExample(tracerProvider: TracerProvider) {
    // The Tracer we'll use for the example
    private val tracer: Tracer

    // This method instruments doWork() method
    fun myWonderfulUseCase() {
        // Generate span
        val span: Span = tracer.spanBuilder("Start my wonderful use case").startSpan()
        try {
            span.makeCurrent().use { scope ->
                // Add some Event to the span
                span.addEvent("Event 0")
                // execute my use case - here we simulate a wait
                doWork()
                // Add some Event to the span
                span.addEvent("Event 1")
            }
        } finally {
            span.end()
        }
    }

    fun doWork() {
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            // ignore in an example
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // Parsing the input
            if (args.size < 2) {
                println("Missing [hostname] [port]")
                System.exit(1)
            }
            val ip = args[0]
            val port = args[1].toInt()

            // it is important to initialize the OpenTelemetry SDK as early as possible in your process.
            val openTelemetry = initializeOpenTelemetry(ip, port)
            val tracerProvider = openTelemetry.tracerProvider

            // start example
            val example = ZipkinExample(tracerProvider)
            example.myWonderfulUseCase()
            println("Bye")
        }
    }

    init {
        tracer = tracerProvider["io.opentelemetry.example.ZipkinExample"]
    }
}