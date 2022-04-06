package dreifa.app.tasks.executionContext

import dreifa.app.tasks.logging.*
import dreifa.app.tasks.Task
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
     * Typically a sequence of task is run in a "pipeline". The pipeline
     * context holds the logical identifier. This is used as the key to lookup any
     * state information that has supplied by earlier tasks
     */
    @Deprecated(
        "this was a bad idea :(. Need a better solution, " +
                "though I'm still not sure what that will be!"
    )
    fun pipelineContext(): PipelineContext

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

    /*
      This should link back to the original invocation, so that all activity 
      linked to it can be tracked against a single ID. So typically 
      an API controller would set on up if not present and then pass it on. 
      
      The intention is that this id can be used for distributed logging and tracing, 
      so that if calls cross process boundaries this ID is retained, i.e. a new 
      executionId is only generated for the original invocation, after that it is retained 
      and passed between services, though the current implementation (Dec 2019) 
      doesn't really enforce this.

      In ZipKin terminology this is called the traceId
    */
    fun executionId(): UUID

    fun taskId(): UUID?


    /**
     * Instance qualifier - if multiple services are deployed to a server,
     * this gives that task the additional information needed to disambiguate names and
     * directories, e.g. if we have both alice and bob on the same server, then obviously
     * they need separate install directories. This does not help with other parts of the problem,
     * such as picking different port numbers. By default it is null, as the usual
     * server based deploy is a single service per VM / container.
     */
    fun instanceQualifier(): String?

    /**
     * A short cut for logging that pulls in all the information
     * on the ExecutionContext
     */
    @Deprecated(message = "this is just confusing - better to have an explicit builder for LogMessage")
    fun logIt(body: String, level: LogLevel = LogLevel.INFO) {
        val msg = LogMessage(
            executionId = this.executionId(),
            taskId = this.taskId(),
            body = body,
            level = level
        )
        log(msg)
    }

    // convenience methods - gives standard PrintStream style
    // access to the LoggingConsumerContext
    //fun stdout(): PrintStream
    //fun stderr(): PrintStream

}

/**
 * A standard way to modify an existing ExecutionContext
 */
interface ExecutionContextModifier {

    fun withTaskId(taskId: UUID): ExecutionContext

    fun withTaskId(task: Task): ExecutionContext = withTaskId(task.taskId())

    fun withInstanceQualifier(instanceQualifier: String?): ExecutionContext

    fun withLoggingProducerContext(newLoggingProducerContext: LoggingProducerContext): ExecutionContext

    fun withInMemoryLogging(logging: InMemoryLogging): ExecutionContext

    fun withPipelineContext(newPipelineContext: PipelineContext): ExecutionContext
}

/**
 * The
 */
class DefaultExecutionContextModifier(original: ExecutionContext) : ExecutionContextModifier {
    private var working = original
    override fun withTaskId(taskId: UUID): ExecutionContext {
        working = SimpleExecutionContext(
            loggingProducerContext = working,
            executionId = working.executionId(),
            taskId = taskId,
            executor = working.executorService(),
            pm = working.processManager(),
            instanceQualifier = working.instanceQualifier(),
            pipelineContext = working.pipelineContext()
        )
        return working
    }

    override fun withInstanceQualifier(instanceQualifier: String?): ExecutionContext {
        working = SimpleExecutionContext(
            loggingProducerContext = working,
            executionId = working.executionId(),
            taskId = working.taskId(),
            executor = working.executorService(),
            pm = working.processManager(),
            instanceQualifier = instanceQualifier,
            pipelineContext = working.pipelineContext()
        )
        return working
    }

    override fun withLoggingProducerContext(newLoggingProducerContext: LoggingProducerContext): ExecutionContext {
        working = SimpleExecutionContext(
            loggingProducerContext = newLoggingProducerContext,
            executionId = working.executionId(),
            taskId = working.taskId(),
            executor = working.executorService(),
            pm = working.processManager(),
            instanceQualifier = working.instanceQualifier(),
            pipelineContext = working.pipelineContext()
        )
        return working
    }

    override fun withInMemoryLogging(logging: InMemoryLogging): ExecutionContext {
        val logProducerContext = LoggingProducerToConsumer(logging)
        working = SimpleExecutionContext(
            loggingProducerContext = logProducerContext,
            executionId = working.executionId(),
            taskId = working.taskId(),
            executor = working.executorService(),
            pm = working.processManager(),
            instanceQualifier = working.instanceQualifier(),
            pipelineContext = working.pipelineContext()
        )
        return working
    }

    override fun withPipelineContext(newPipelineContext: PipelineContext): ExecutionContext {
        working = SimpleExecutionContext(
            loggingProducerContext = working,
            executionId = working.executionId(),
            taskId = working.taskId(),
            executor = working.executorService(),
            pm = working.processManager(),
            instanceQualifier = working.instanceQualifier(),
            pipelineContext = newPipelineContext
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
    private val taskId: UUID? = null,
    private val instanceQualifier: String? = null,
    private val executor: ExecutorService = Executors.newFixedThreadPool(10),
    private val pm: ProcessManager = ProcessManager(),
    private val pipelineContext: PipelineContext = PipelineContext.DEFAULT,
    //private val openTelemetryProvider: OpenTelemetryProvider = InMemoryOpenTelemetryProvider()
) : ExecutionContext, ExecutionContextModifier {

    override fun pipelineContext(): PipelineContext = pipelineContext

    override fun processManager(): ProcessManager = pm

    override fun executorService(): ExecutorService = executor

    override fun executionId(): UUID = executionId

    override fun taskId(): UUID? = taskId

    override fun instanceQualifier(): String? = instanceQualifier

    override fun logger(): LogMessageConsumer = loggingProducerContext.logger()

    override fun stdout() = loggingProducerContext.stdout()

    override fun stderr() = loggingProducerContext.stderr()

    override fun withTaskId(taskId: UUID): ExecutionContext {
        return DefaultExecutionContextModifier(this).withTaskId(taskId)
    }

    override fun withInstanceQualifier(instanceQualifier: String?): ExecutionContext {
        return DefaultExecutionContextModifier(this).withInstanceQualifier(instanceQualifier)
    }

    override fun withLoggingProducerContext(newLoggingProducerContext: LoggingProducerContext): ExecutionContext {
        return DefaultExecutionContextModifier(this).withLoggingProducerContext(newLoggingProducerContext)
    }

    override fun withInMemoryLogging(logging: InMemoryLogging): ExecutionContext {
        return DefaultExecutionContextModifier(this).withInMemoryLogging(logging)
    }

    override fun withPipelineContext(newPipelineContext: PipelineContext): ExecutionContext {
        return DefaultExecutionContextModifier(this).withPipelineContext(newPipelineContext)
    }
}

//
//interface ExecutionContextFactory {
//
//    /**
//     * Inject in the key context specific information here.
//     * Other values are overridden with the .withXXX methods
//     * on the built execution context.
//     */
//    fun get(
//        executionId: UUID = UUID.randomUUID(),
//        taskId: UUID? = null,
//        scoped: Registry = Registry(),
//        logMessageConsumer: LogMessageConsumer? = null
//
//    ): ExecutionContext
//}
