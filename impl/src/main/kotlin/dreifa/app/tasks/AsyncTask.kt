package dreifa.app.tasks

import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.types.UniqueId


/**
 * The three basic result types for onSuccess, onFail
 * and onTimeout
 */
sealed class AsyncResult<T>
class Success<T>(val result: T) : AsyncResult<T>() {
    override fun equals(other: Any?): Boolean {
        return if (other is Success<*>) {
            this.result == other.result
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return result!!.hashCode()
    }
}

class Fail<T>(val message: String) : AsyncResult<T>()
class Timeout<T>(val message: String) : AsyncResult<T>()


/**
 * The server (generator) side of ResultChannel
 */
interface AsyncResultChannelSource {
    fun create(): AsyncResultChannelMessage<Any>
}


/**
 * The usual way of handling an AsyncResult in client code
 */
interface AsyncResultHandler<T> {
    fun onSuccess(result: T)
    fun onFail(message: String)
    fun onTimeout(message: String)
}

/**
 * register a handler for the result
 */
interface RegisterAsyncResultHandler<T> {
    fun register(channelId: UniqueId, handler: AsyncResultHandler<T>)
}


interface AsyncTask<I, O> : Task {
    /**
     * Execute the task.
     */
    fun exec(
        ctx: ExecutionContext = SimpleExecutionContext(),

        /**
         * Where to send the result back to? Should be stored
         * with the original request.
         */
        channelLocator: AsyncResultChannelSinkLocator,

        /**
         * The unique channelId
         */
        channelId: UniqueId,

        /**
         * The actual input
         */
        input: I
    )

}








