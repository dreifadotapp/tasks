package dreifa.app.tasks.opentelemetry

import dreifa.app.opentelemetry.OpenTelemetryProvider
import dreifa.app.registry.Registry
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.executionContext.ExecutionContext
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

abstract class SuspendableBlockingTaskReporter<in I, out O>(reg: Registry) : BlockingTask<I, O> {
    private val provider = reg.get(OpenTelemetryProvider::class.java)
    private val tracer = reg.get(Tracer::class.java)

    final override fun exec(ctx: ExecutionContext, input: I): O {
        return runBlocking {
            withContext(Context.current().asContextElement()) {
                val span = startSpan()
                try {
                    val result = doExecWithTelemetry(ctx, input)
                    completeSpan(span)
                    result
                } catch (ex: Exception) {
                    completeSpan(span, ex)
                    throw ex
                }
            }
        }
    }

    private fun startSpan(): Span {
        return tracer.spanBuilder(this.taskName())
            .setSpanKind(SpanKind.CLIENT)
            .startSpan()
            .setAttribute("client.attr", "foo")
    }

    private fun completeSpan(span: Span) {
        span.setStatus(StatusCode.OK)
        span.end()
    }

    private fun completeSpan(span: Span, ex: Exception) {
        span.recordException(ex)
        span.setStatus(StatusCode.ERROR)
        span.end()
    }

    abstract suspend fun doExecWithTelemetry(ctx: ExecutionContext, input: I): O
}


class EchoStringTask(reg: Registry) : SuspendableBlockingTaskReporter<String, String>(reg) {
    override suspend fun doExecWithTelemetry(ctx: ExecutionContext, input: String): String {
        return input
    }
}
