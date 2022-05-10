package dreifa.app.tasks.opentelemetry

import dreifa.app.opentelemetry.ContextHelper
import dreifa.app.opentelemetry.OpenTelemetryContext
import dreifa.app.opentelemetry.OpenTelemetryProvider
import dreifa.app.registry.Registry
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.logging.LogLevel
import dreifa.app.tasks.logging.LogMessage
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class BlockingTaskOTDecorator<in I, out O>(reg: Registry, private val task: BlockingTask<I, O>) : BlockingTask<I, O> {
    private val tracer = reg.getOrNull(Tracer::class.java)
    private val provider = reg.getOrNull(OpenTelemetryProvider::class.java)

    override fun exec(ctx: ExecutionContext, input: I): O {
        if (tracer != null && provider != null) {
            return runBlocking {
                val helper = ContextHelper(provider)
                withContext(helper.createContext(ctx.telemetryContext()).asContextElement()) {
                    val (span,modifiedCtx) = startSpan(ctx)
                    try {
                        val result = task.exec(modifiedCtx, input)
                        completeSpan(span)
                        result
                    } catch (ex: Throwable) {
                        // post the message back on the logging channel - independent of OpenTelemetry
                        withTimeoutOrNull(500L) {
                            reportExceptionToClient(modifiedCtx, ex)
                        }
                        // and close the span
                        completeSpan(span, ex)
                        throw ex
                    }
                }
            }
        } else {
            return runBlocking {
                try {
                    task.exec(ctx, input)
                } catch (ex: Throwable) {
                    // post the message back on the logging channel - independent of OpenTelemetry
                    withTimeoutOrNull(500L) {
                        reportExceptionToClient(ctx, ex)
                    }
                    throw ex
                }
            }
        }
    }

    private fun startSpan(ctx: ExecutionContext): Pair<Span,ExecutionContext> {
        val span = tracer!!.spanBuilder(task.taskName())
            .setSpanKind(SpanKind.SERVER)
            .startSpan()
        ctx.correlation().forEach {
            span.setAttribute(it.openTelemetryAttrName, it.id.toString())
        }
        // pass on the latest telemetryContext
        return Pair(span, ctx.withTelemetryContext(OpenTelemetryContext.fromSpan(span)))
    }

    private fun completeSpan(span: Span) {
        span.setStatus(StatusCode.OK)
        span.end()
    }

    private fun completeSpan(span: Span, ex: Throwable) {
        span.recordException(ex)
        span.setStatus(StatusCode.ERROR)
        span.end()
    }

    private fun reportExceptionToClient(ctx: ExecutionContext, ex: Throwable) {
        try {
            val message = LogMessage(
                openTelemetryContext = ctx.telemetryContext(),
                level = LogLevel.WARN,
                body = "Task '${task.taskName()}' threw exception: '${ex.message}' [${ex::class.simpleName}]"
            )
            ctx.log(message)
        } catch (ignoreMe: Exception) {
        }
    }
}