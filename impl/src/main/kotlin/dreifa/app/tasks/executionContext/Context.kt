package dreifa.app.tasks.executionContext

import dreifa.app.opentelemetry.OpenTelemetryContext
import dreifa.app.tasks.logging.*
import dreifa.app.tasks.processManager.ProcessManager
import java.util.*
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
    fun openTelemetryContext(): OpenTelemetryContext = OpenTelemetryContext.root

    /**
     * Instance qualifier - if multiple services are deployed to a server,
     * this gives that task the additional information needed to disambiguate names and
     * directories, e.g. if we have both alice and bob on the same server, then obviously
     * they need separate install directories. This does not help with other parts of the problem,
     * such as picking different port numbers. By default it is null, as the usual
     * server based deploy is a single service per VM / container.
     */
    fun instanceQualifier(): String?

}

/**
 * A standard way to modify an existing ExecutionContext
 */
interface ExecutionContextModifier {

    fun withInstanceQualifier(instanceQualifier: String?): ExecutionContext

    fun withLoggingProducerContext(newLoggingProducerContext: LoggingProducerContext): ExecutionContext

    fun withInMemoryLogging(logging: InMemoryLogging): ExecutionContext

    fun withOpenTelemetryContext(openTelemetryContext: OpenTelemetryContext): ExecutionContext
}

/**
 * The
 */
class DefaultExecutionContextModifier(original: ExecutionContext) : ExecutionContextModifier {
    private var working = original

    override fun withInstanceQualifier(instanceQualifier: String?): ExecutionContext {
        working = SimpleExecutionContext(
            loggingProducerContext = working,
            openTelemetryContext = working.openTelemetryContext(),
            executor = working.executorService(),
            pm = working.processManager(),
            instanceQualifier = instanceQualifier
        )
        return working
    }

    override fun withLoggingProducerContext(newLoggingProducerContext: LoggingProducerContext): ExecutionContext {
        working = SimpleExecutionContext(
            loggingProducerContext = newLoggingProducerContext,
            openTelemetryContext = working.openTelemetryContext(),
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
            openTelemetryContext = working.openTelemetryContext(),
            executor = working.executorService(),
            pm = working.processManager(),
            instanceQualifier = working.instanceQualifier()
        )
        return working
    }

    override fun withOpenTelemetryContext(openTelemetryContext: OpenTelemetryContext): ExecutionContext {
        working = SimpleExecutionContext(
            loggingProducerContext = working,
            openTelemetryContext = openTelemetryContext,
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
    private val executionId: UUID = UUID.randomUUID(),
    private val openTelemetryContext: OpenTelemetryContext = OpenTelemetryContext.root,
    private val instanceQualifier: String? = null,
    private val executor: ExecutorService = Executors.newFixedThreadPool(10),
    private val pm: ProcessManager = ProcessManager()
) : ExecutionContext, ExecutionContextModifier {

    override fun processManager(): ProcessManager = pm

    override fun executorService(): ExecutorService = executor

    override fun openTelemetryContext() = openTelemetryContext

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

}
