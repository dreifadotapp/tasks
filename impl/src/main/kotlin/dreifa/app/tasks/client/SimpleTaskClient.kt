package dreifa.app.tasks.client

import dreifa.app.opentelemetry.*
import dreifa.app.registry.Registry
import dreifa.app.sis.JsonSerialiser
import dreifa.app.tasks.*
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.tasks.logging.DefaultLoggingChannelFactory
import dreifa.app.tasks.logging.LoggingChannelFactory
import dreifa.app.tasks.logging.LoggingProducerToConsumer
import dreifa.app.tasks.opentelemetry.BlockingTaskOTDecorator
import dreifa.app.types.UniqueId
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.lang.RuntimeException
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

/**
 * Enough for unit tests and tasks running locally.
 */
class SimpleTaskClient(private val registry: Registry, clazzLoader: ClassLoader? = null) : TaskClient {
    private val taskFactory = registry.get(TaskFactory::class.java)
    private val serialiser = JsonSerialiser(clazzLoader)

    private val logChannelLocatorFactory =
        registry.getOrElse(LoggingChannelFactory::class.java, DefaultLoggingChannelFactory(registry))

    override fun <I : Any, O : Any> execBlocking(
        ctx: ClientContext, qualifiedTaskName: String, input: I, outputClazz: KClass<O>
    ): O {
        @Suppress("UNCHECKED_CAST") val task = taskFactory.createInstance(qualifiedTaskName) as BlockingTask<I, O>

        val decorated = BlockingTaskOTDecorator(registry, task)
        val executionContext = buildExecutionContext(ctx)
        return if (task is NotRemotableTask) {
            // TODO - should we simply be throwing an Exception here ?
            //        it makes no sense to call a NonRemotable task via the TaskClient
            decorated.exec(executionContext, input)
        } else {
            val roundTrippedInput = roundTripInput(ctx, input)
            val output = decorated.exec(executionContext, roundTrippedInput)
            roundTripOutput(ctx, output)
        }
    }

    override fun <I : Any, O : Any> execAsync(
        ctx: ClientContext,
        qualifiedTaskName: String,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: I,
        outputClazz: KClass<O>
    ) {
        @Suppress("UNCHECKED_CAST") val task = taskFactory.createInstance(qualifiedTaskName) as AsyncTask<I, O>

        // hook in logging producer / consumer pair
        val loggingConsumerContext = logChannelLocatorFactory.consumer(ctx.logChannelLocator())
        val producerContext = LoggingProducerToConsumer(loggingConsumerContext)
        val executionContext = SimpleExecutionContext(producerContext)

        task.exec(executionContext, channelLocator, channelId, input)
    }

    override fun <I : Any, O : Any> taskDocs(
        ctx: ClientContext, qualifiedTaskName: String
    ): TaskDoc<I, O> {
        val task = taskFactory.createInstance(qualifiedTaskName)
        if (task is TaskDoc<*, *>) {
            @Suppress("UNCHECKED_CAST") return TaskDocHolder(
                task.description(), task.examples() as List<TaskExample<I, O>>
            )
        } else {
            throw RuntimeException("No TaskDoc for task: $qualifiedTaskName")
        }
    }

    private fun buildExecutionContext(ctx: ClientContext): SimpleExecutionContext {
        // hook in logging producer / consumer pair
        val loggingConsumerContext = logChannelLocatorFactory.consumer(ctx.logChannelLocator())
        val producerContext = LoggingProducerToConsumer(loggingConsumerContext)

        // as this a local client, explicitly set to INTERNAL span kind
        val telemetryContext = ctx.telemetryContext().context().copy(spanKind = SpanKind.INTERNAL)

        return SimpleExecutionContext(
            loggingProducerContext = producerContext,
            telemetryContext = telemetryContext,
            correlation = ctx.correlation()
        )
    }

    private fun <I : Any> roundTripInput(ctx: ClientContext, input: I): I {
        return Helpers.runWithTelemetry(
            registry = registry,
            telemetryContext = ctx.telemetryContext().context(),
            spanDetails = SpanDetails("roundTripOutput", SpanKind.INTERNAL),
        ) {
            @Suppress("UNCHECKED_CAST") serialiser.fromPacket(serialiser.toPacket(input)).any() as I
        }
    }

    private fun <O : Any> roundTripOutput(ctx: ClientContext, output: O): O {
        return Helpers.runWithTelemetry(
            registry = registry,
            telemetryContext = ctx.telemetryContext().context(),
            spanDetails = SpanDetails("roundTripOutput", SpanKind.INTERNAL),
        ) {
            @Suppress("UNCHECKED_CAST") serialiser.fromPacket(serialiser.toPacket(output)).any() as O
        }
    }
}
