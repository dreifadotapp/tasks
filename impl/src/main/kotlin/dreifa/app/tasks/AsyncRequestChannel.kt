package dreifa.app.tasks

import dreifa.app.types.UniqueId

/**
 * Send a request to a task. This works for both Blocking and Async Tasks
 */

data class AsyncRequestChannelMessage<T>(

    /**
    The channel for the result. We assume that the type of channel
    has already been selected , so all that is needed here is a unique id
    within that channel
     */
    val resultChannelId: UniqueId,


    /**
    The actual request
     */
    val request: T,

    /**
    The Java class of the request.
     */
    val requestClazz: Class<T>
)