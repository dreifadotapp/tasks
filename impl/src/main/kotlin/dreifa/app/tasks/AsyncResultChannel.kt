package dreifa.app.tasks

import dreifa.app.types.UniqueId
import java.util.*
import java.util.concurrent.Future
import java.util.function.Consumer

/**
 * The logical channel for passing results between services.
 * Note that writing to a channel is ALWAYS idempotent, i.e. we NEVER
 * generate different results for the same channelId
 */
data class AsyncResultChannelMessage<T>(
    val channelId: UniqueId,
    val result: AsyncResult<T>,
    val resultClazz: Class<T>   // for serializer and helping with type safety. Is it s good idea?
                                // it does mean that the client must be Java / Kotlin ? I think this
                                // is OK. With the really simple serializer concept, we are not
                                // putting up a big barrier to non Java clients in the future.
)

/**
 * The client (consumer) side of ResultChannel.
 * Note at this
 */
interface AsyncResultChannelSink : Consumer<AsyncResultChannelMessage<*>> {
    override fun accept(result: AsyncResultChannelMessage<*>)
}


/**
 * Query API over a result channel.
 */
interface AsyncResultChannelQuery {
    fun hasResult(channelId: UniqueId): Boolean
    fun <T> result(channelId: UniqueId): AsyncResult<T>
}

/**
 * As the AsyncTask can be long running and may live between restarts,
 * then there needs to a way for the server side to know how to
 * recreate the AsyncResultChannelSource that was specified
 * when the original request was made.
 *
 * There are some assumptions and conventions here:
 *   - the first part of the string specifies the type of channel,
 *     for example "LOCAL:", "REST:", "AWSSMS:"
 *   - the second part of the string encodes all other connection information
 *   - the server side will store the locator linked to the originating
 *     request
 *   - an implementation of AsyncResultChannelSourceFactory will
 *     know how to create a suitable concrete implementations of AsyncResultChannelSource
 *     from the Locator
 *
 */
data class AsyncResultChannelSinkLocator(val locator: String) {
    companion object {
        val LOCAL = AsyncResultChannelSinkLocator("LOCAL")
    }
}

/**
 * Given a AsyncResultChannelSourceLocator, return the actual channel
 */
interface AsyncResultChannelSinkFactory {
    fun create(locator: AsyncResultChannelSinkLocator): AsyncResultChannelSink
}

/**
 * TODO - A Future based "waitfor" style API
 */
interface AsyncResultChannelFuture {
    fun <T> get(channelId: UniqueId): Future<AsyncResult<T>>
}

/**
 * A simple in memory implementation for testing
 */
class InMemoryAsyncResultChannel : AsyncResultChannelSink, AsyncResultChannelQuery {
    private val channel = LinkedList<AsyncResultChannelMessage<*>>()

    override fun accept(result: AsyncResultChannelMessage<*>) {
        // todo - should be checking if a result for that channel already exists
        channel.add(result)
    }

    override fun hasResult(channelId: UniqueId): Boolean {
        return channel.any { it.channelId == channelId }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> result(channelId: UniqueId): AsyncResult<T> {
        if (hasResult(channelId)) {
            return channel.single { it.channelId == channelId }.result as AsyncResult<T>
        } else {
            throw TaskException("no result yet for $channelId")
        }
    }
}

/**
 * This needs some thought. Can we limit it to just the "LOCAL" channel?
 */
class DefaultAsyncResultChannelSinkFactory : AsyncResultChannelSinkFactory {
    private val inMemoryAsyncResultChannel = InMemoryAsyncResultChannel()
    override fun create(locator: AsyncResultChannelSinkLocator): AsyncResultChannelSink {
        if (locator.locator == "LOCAL") {
            return inMemoryAsyncResultChannel
        } else {
            throw RuntimeException("Don't know about $locator")
        }
    }

    fun channelQuery(locator: AsyncResultChannelSinkLocator): AsyncResultChannelQuery {
        if (locator.locator == "LOCAL") {
            return inMemoryAsyncResultChannel
        } else {
            throw RuntimeException("Don't know about $locator")
        }
    }

}
