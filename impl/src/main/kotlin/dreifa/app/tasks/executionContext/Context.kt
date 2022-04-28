package dreifa.app.tasks.executionContext

import dreifa.app.opentelemetry.OpenTelemetryContext
import dreifa.app.tasks.logging.*
import dreifa.app.tasks.processManager.ProcessManager
import dreifa.app.types.CorrelationContexts
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * A standard context to pass around. It provides access to services and
 * information that are deferred until execution time, as they may
 * change on each run
 */
interface ExecutionContext : LoggingProducerContext, ExecutionContextModifier {

    /**
     *  One single place for running and checking the status of processes.
     *
     *  @see ProcessManager
     */
    fun processManager(): ProcessManager

    /*
      The Java ExecutorService to be used.
     */
    fun executorService(): ExecutorService

    /**
     * Well behaved tasks should implement OpenTelemetry
     */
    fun telemetryContext(): OpenTelemetryContext = OpenTelemetryContext.root

    /**
     * Instance qualifier - if multiple services are deployed to a server,
     * this gives that task the additional information needed to disambiguate names and
     * directories, e.g. if we have both alice and bob on the same server, then obviously
     * they need separate install directories. This does not help with other parts of the problem,
     * such as picking different port numbers. By default it is null, as the usual
     * server based deploy is a single service per VM / container.
     */
    fun instanceQualifier(): String?

    fun correlation(): CorrelationContexts

}

/**
 * A standard way to modify an existing ExecutionContext
 */
interface ExecutionContextModifier {

    fun withInstanceQualifier(instanceQualifier: String?): ExecutionContext

    fun withLoggingProducerContext(newLoggingProducerContext: LoggingProducerContext): ExecutionContext

    fun withInMemoryLogging(logging: InMemoryLogging): ExecutionContext

    fun withOpenTelemetryContext(openTelemetryContext: OpenTelemetryContext): ExecutionContext

    fun withCorrelationContexts(correlationContexts: CorrelationContexts): ExecutionContext
}

/**
 * The
 */
class DefaultExecutionContextModifier(original: ExecutionContext) : ExecutionContextModifier {
    private var working = original

    override fun withInstanceQualifier(instanceQualifier: String?): ExecutionContext {
        working = SimpleExecutionContext(
            loggingProducerContext = working,
            telemetryContext = working.telemetryContext(),
            correlation = working.correlation(),
            executor = working.executorService(),
            pm = working.processManager(),
            instanceQualifier = instanceQualifier
        )
        return working
    }

    override fun withLoggingProducerContext(newLoggingProducerContext: LoggingProducerContext): ExecutionContext {
        working = SimpleExecutionContext(
            loggingProducerContext = newLoggingProducerContext,
            telemetryContext = working.telemetryContext(),
            correlation = working.correlation(),
            executor = working.executorService(),
            pm = working.processManager(),
            instanceQualifier = working.instanceQualifier()
        )
        return working
    }

    override fun withInMemoryLogging(logging: InMemoryLogging): ExecutionContext {
        val logProducerContext = LoggingProducerToConsumer(logging)
        working = SimpleExecutionContext(
            loggingProducerContext = logProducerContext,
            telemetryContext = working.telemetryContext(),
            correlation = working.correlation(),
            executor = working.executorService(),
            pm = working.processManager(),
            instanceQualifier = working.instanceQualifier()
        )
        return working
    }

    override fun withOpenTelemetryContext(openTelemetryContext: OpenTelemetryContext): ExecutionContext {
        working = SimpleExecutionContext(
            loggingProducerContext = working,
            telemetryContext = openTelemetryContext,
            correlation = working.correlation(),
            executor = working.executorService(),
            pm = working.processManager(),
            instanceQualifier = working.instanceQualifier()
        )
        return working
    }

    override fun withCorrelationContexts(correlationContexts: CorrelationContexts): ExecutionContext {
        working = SimpleExecutionContext(
            loggingProducerContext = working,
            telemetryContext = working.telemetryContext(),
            correlation = correlationContexts,
            executor = working.executorService(),
            pm = working.processManager(),
            instanceQualifier = working.instanceQualifier()
        )
        return working
    }

}

/**
 * A simple service, suitable for unit test
 */
class SimpleExecutionContext(
    private val loggingProducerContext: LoggingProducerContext = ConsoleLoggingProducerContext(),
    private val telemetryContext: OpenTelemetryContext = OpenTelemetryContext.root,
    private val correlation: CorrelationContexts = CorrelationContexts.empty(),
    private val instanceQualifier: String? = null,
    private val executor: ExecutorService = Executors.newFixedThreadPool(10),
    private val pm: ProcessManager = ProcessManager()
) : ExecutionContext, ExecutionContextModifier {

    override fun processManager(): ProcessManager = pm

    override fun executorService(): ExecutorService = executor

    override fun telemetryContext() = telemetryContext

    override fun correlation(): CorrelationContexts = correlation

    override fun instanceQualifier(): String? = instanceQualifier

    override fun logger(): LogMessageConsumer = loggingProducerContext.logger()

    override fun stdout() = loggingProducerContext.stdout()

    override fun stderr() = loggingProducerContext.stderr()

    override fun withInstanceQualifier(instanceQualifier: String?): ExecutionContext {
        return DefaultExecutionContextModifier(this).withInstanceQualifier(instanceQualifier)
    }

    override fun withLoggingProducerContext(newLoggingProducerContext: LoggingProducerContext): ExecutionContext {
        return DefaultExecutionContextModifier(this).withLoggingProducerContext(newLoggingProducerContext)
    }

    override fun withInMemoryLogging(logging: InMemoryLogging): ExecutionContext {
        return DefaultExecutionContextModifier(this).withInMemoryLogging(logging)
    }

    override fun withOpenTelemetryContext(openTelemetryContext: OpenTelemetryContext): ExecutionContext {
        return DefaultExecutionContextModifier(this).withOpenTelemetryContext(openTelemetryContext)
    }

    override fun withCorrelationContexts(correlationContexts: CorrelationContexts): ExecutionContext {
        return DefaultExecutionContextModifier(this).withCorrelationContexts(correlationContexts)
    }

}
