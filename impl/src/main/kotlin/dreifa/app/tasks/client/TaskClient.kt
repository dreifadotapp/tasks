package dreifa.app.tasks.client

import dreifa.app.tasks.*
import dreifa.app.types.UniqueId
import kotlin.reflect.KClass

/**
 * Use a TaskClient when remoting is necessary, i.e. there is a client - server relationship
 * and the client doesn't have direct access to the implementing Task
 *
 * The TaskClient is not generally recommended for local access. For this use case either call
 * the Task(s) directly or through one of the decorators (e.g. BlockingTaskOTDecorator) for standardised
 * handling of telemetry, logging and exception handling.
 */

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
