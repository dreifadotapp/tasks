package dreifa.app.tasks.inbuilt.providers

import dreifa.app.registry.Registry
import dreifa.app.ses.*
import dreifa.app.tasks.BaseBlockingTask
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.types.UniqueId
import java.lang.RuntimeException

data class TPInfoResult(
    val providerId: UniqueId,
    val jarBundleId: UniqueId,
    val name: String
)

interface TPInfoTask : BlockingTask<UniqueId, TPInfoResult>

class TPInfoTaskImpl(registry: Registry) : BaseBlockingTask<UniqueId, TPInfoResult>(), TPInfoTask {
    private val ses = registry.get(EventStore::class.java)

    override fun exec(ctx: ExecutionContext, input: UniqueId): TPInfoResult {
        return queryById(input)
    }

    private fun queryById(providerId: UniqueId): TPInfoResult {
        val query = AllOfQuery(
            EventTypeQuery(eventType = TPProviderRegisteredEventFactory.eventType()),
            AggregateIdQuery(aggregateId = providerId.toString())
        )
        ses.read(query).lastOrNull()?.let {
            val payload = it.payloadAs(TPRegisterProviderRequest::class.java)
            return TPInfoResult(payload.providerId, payload.jarBundleId, payload.providerName)
        }
        throw RuntimeException("Not Found")
    }
}