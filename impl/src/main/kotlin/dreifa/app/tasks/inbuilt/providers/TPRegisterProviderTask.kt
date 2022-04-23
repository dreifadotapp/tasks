package dreifa.app.tasks.inbuilt.providers

import dreifa.app.registry.Registry
import dreifa.app.ses.Event
import dreifa.app.ses.EventFactory
import dreifa.app.ses.EventStore
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.IdempotentTask
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.types.UniqueId

data class TPRegisterProviderRequest(
    /**
     * The id of the FileBundle holding the JAR(s). Must have
     * already been stored
     */
    val jarBundleId: UniqueId,

    /**
     * The id of this provider
     */
    val providerId: UniqueId,

    /**
     * The class name, i.e. the class that implements 'TaskRegistrations'
     */
    val providerClazz: String,

    /**
     * A human readable name, e.g. 'Google Cloud Compute Tasks'
     */
    val providerName: String
)

interface TPRegisterProviderTask : BlockingTask<TPRegisterProviderRequest, Unit>, IdempotentTask

object TPProviderRegisteredEventFactory : EventFactory {
    fun create(params: TPRegisterProviderRequest): Event {
        return Event(
            type = eventType(),
            aggregateId = params.providerId.toString(),
            payload = params
        )
    }

    override fun eventType(): String = "dreifa.app.tasks.inbuilt.providers.TPProviderRegistered"
}

class TPRegisterProviderTaskImpl(val registry: Registry) : BlockingTask<TPRegisterProviderRequest, Unit>,
    TPRegisterProviderTask {
    private val ses = registry.get(EventStore::class.java)
    override fun exec(ctx: ExecutionContext, input: TPRegisterProviderRequest) {
        val ev = TPProviderRegisteredEventFactory.create(input)
        ses.store(ev)
    }
}