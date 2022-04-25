package dreifa.app.tasks.client

import dreifa.app.registry.Registry
import dreifa.app.sis.JsonSerialiser
import dreifa.app.tasks.*
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.tasks.logging.*
import dreifa.app.tasks.opentelemetry.BlockingTaskOTDecorator
import dreifa.app.types.UniqueId
import java.lang.RuntimeException
import kotlin.reflect.KClass

/**
 * Marker interface for any type of security (authentication & authorisation) protocol
 */
interface SecurityPrinciple

/**
 * Pass a JWT token that can be checked
 */
//class JwtSecurityPrinciple(val jwtToken: String) : SecurityPrinciple

/**
 * Authenticated with just a username and set of roles. We trust an external system
 */
//class UserAndRoles(val userName: String, val roles: Set<String>) : SecurityPrinciple

/**
 * For testing, or environments where security is unimportant
 */
class NotAuthenticatedSecurityPrinciple(val userName: String = "unknown") : SecurityPrinciple

/**
 * The information that any client must provide
 */
interface ClientContext {

    /**
     * One of the security principles
     */
    fun securityPrinciples(): Set<SecurityPrinciple>

    /**
     * How to be sent back log messages
     */
    fun logChannelLocator(): LoggingChannelLocator

}

interface TaskClient {
    fun <I : Any, O : Any> execBlocking(
        ctx: ClientContext,
        taskName: String,
        input: I,
        outputClazz: KClass<O>  // need access to the output clazz for serialization
    ): O

    fun <I : Any, O : Any> execAsync(
        ctx: ClientContext,
        taskName: String,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: I,
        outputClazz: KClass<O>  // need access to the output clazz for serialization
    )

    /**
     * Returns TaskDoc information if implemented, else throws a RuntimeException
     */
    fun <I : Any, O : Any> taskDocs(
        ctx: ClientContext,
        taskName: String
    ): TaskDoc<I, O>
}

/**
 * Enough for unit test and to communicate with tasks running locally
 */
class SimpleClientContext(private val loggingChannelLocator: LoggingChannelLocator = LoggingChannelLocator.inMemory()) :
    ClientContext {
    private val principle = NotAuthenticatedSecurityPrinciple()
    override fun securityPrinciples(): Set<SecurityPrinciple> = setOf(principle)
    override fun logChannelLocator(): LoggingChannelLocator = loggingChannelLocator
}

/**
 * Enough for unit tests and tasks running locally
 */
class SimpleTaskClient(private val registry: Registry, private val clazzLoader: ClassLoader? = null) : TaskClient {
    private val taskFactory = registry.get(TaskFactory::class.java)
    private val serialiser = JsonSerialiser(clazzLoader)
    private val logChannelLocatorFactory =
        registry.getOrElse(LoggingChannelFactory::class.java, DefaultLoggingChannelFactory(registry))

    override fun <I : Any, O : Any> execBlocking(
        ctx: ClientContext,
        taskName: String,
        input: I,
        outputClazz: KClass<O>
    ): O {

        // hook in logging producer / consumer pair
        val loggingConsumerContext = logChannelLocatorFactory.consumer(ctx.logChannelLocator())
        val producerContext = LoggingProducerToConsumer(loggingConsumerContext)
        val executionContext = SimpleExecutionContext(producerContext)

        @Suppress("UNCHECKED_CAST")
        val task = taskFactory.createInstance(taskName) as BlockingTask<I, O>
        val decorated = BlockingTaskOTDecorator(registry, task)
        return decorated.exec(executionContext,input)

//        try {
//            return if (task is NotRemotableTask) {
//                task.exec(executionContext, input)
//            } else {
//                // note, force serialisation / de-serialisation locally to catch any problems early
//                val result = task.exec(executionContext, roundTripInput(input))
//                roundTripOutput(result)
//            }
//        } catch (e: Exception) {
//            val message = LogMessage(
//                openTelemetryContext = executionContext.openTelemetryContext(),
//                level = LogLevel.WARN,
//                body = "Task generated exception of: ${e.message}"
//            )
//            loggingConsumerContext.acceptLog(message)
//            throw e
//        }
    }

    private fun <I : Any> roundTripInput(input: I): I {
        @Suppress("UNCHECKED_CAST")
        return serialiser.fromPacket(serialiser.toPacket(input)).any() as I
    }

    private fun <O : Any> roundTripOutput(output: O): O {
        @Suppress("UNCHECKED_CAST")
        return serialiser.fromPacket(serialiser.toPacket(output)).any() as O
    }

    override fun <I : Any, O : Any> execAsync(
        ctx: ClientContext,
        taskName: String,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: I,
        outputClazz: KClass<O>
    ) {
        @Suppress("UNCHECKED_CAST")
        val task = taskFactory.createInstance(taskName) as AsyncTask<I, O>

        // hook in logging producer / consumer pair
        val loggingConsumerContext = logChannelLocatorFactory.consumer(ctx.logChannelLocator())
        val producerContext = LoggingProducerToConsumer(loggingConsumerContext)
        val executionContext = SimpleExecutionContext(producerContext)

        task.exec(executionContext, channelLocator, channelId, input)
    }

    override fun <I : Any, O : Any> taskDocs(
        ctx: ClientContext,
        taskName: String
    ): TaskDoc<I, O> {
        val task = taskFactory.createInstance(taskName)
        if (task is TaskDoc<*, *>) {
            @Suppress("UNCHECKED_CAST")
            return TaskDocHolder(
                task.description(),
                task.examples() as List<TaskExample<I, O>>
            )
        } else {
            throw RuntimeException("No TaskDoc for task: $taskName")
        }
    }
}