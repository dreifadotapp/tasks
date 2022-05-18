package dreifa.app.tasks.inbuilt.providers

import dreifa.app.registry.Registry
import dreifa.app.ses.*
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.eventClientContext
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.types.UniqueId
import java.lang.RuntimeException

data class TPInfoResult(
    val jarBundleId: UniqueId,
    val providerId: UniqueId,
    val providerClazz: String,
    val providerName: String
)

interface TPInfoTask : BlockingTask<UniqueId, TPInfoResult> {
    override fun taskName(): String = TPQueryTask::class.simpleName!!
}

class TPInfoTaskImpl(registry: Registry) : BlockingTask<UniqueId, TPInfoResult>, TPInfoTask {
    private val ses = registry.get(EventStore::class.java)

    override fun exec(ctx: ExecutionContext, input: UniqueId): TPInfoResult {
        return queryById(ctx.eventClientContext(), input)
    }

    private fun queryById(etx: ClientContext, providerId: UniqueId): TPInfoResult {
        val query = AllOfQuery(
            EventTypeQuery(eventType = TPProviderRegisteredEventFactory.eventType()),
            AggregateIdQuery(aggregateId = providerId.toString())
        )
        ses.read(etx, query).lastOrNull()?.let {
            val payload = it.payloadAs(TPRegisterProviderRequest::class.java)
            return TPInfoResult(
                jarBundleId = payload.jarBundleId,
                providerId = payload.providerId,
                providerClazz = payload.providerClazz,
                providerName = payload.providerName
            )
        }
        throw RuntimeException("Not Found")
    }
}