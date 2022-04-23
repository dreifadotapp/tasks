package dreifa.app.tasks.opentelemetry

import dreifa.app.opentelemetry.OpenTelemetryProvider
import dreifa.app.registry.Registry
import dreifa.app.tasks.BaseBlockingTask
import dreifa.app.tasks.executionContext.ExecutionContext
import io.opentelemetry.context.Context

abstract class BlockingTaskReporter<in I, out O>(reg: Registry) : BaseBlockingTask<I, O>() {
    private val provider = reg.get(OpenTelemetryProvider::class.java)

    final override fun exec(ctx: ExecutionContext, input: I): O {

        Context.current() .makeCurrent()



        return doExecWithTelemetry(ctx, input)
    }

    abstract fun doExecWithTelemetry(ctx: ExecutionContext, input: I): O
}

class EchoStringTask(reg: Registry) : BlockingTaskReporter<String, String>(reg) {
    override fun doExecWithTelemetry(ctx: ExecutionContext, input: String): String {
        return input
    }
}
