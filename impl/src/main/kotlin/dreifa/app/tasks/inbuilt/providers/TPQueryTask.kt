package dreifa.app.tasks.inbuilt.providers

import dreifa.app.registry.Registry
import dreifa.app.ses.*
import dreifa.app.tasks.BaseBlockingTask
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.types.LikeString
import dreifa.app.types.UniqueId
import java.lang.RuntimeException

data class TPQueryParams(
    val providerId: UniqueId? = null,
    val nameIsLike: String? = null
) {
    constructor(providerId: String, name: String) : this(UniqueId.fromString(providerId), name)
}

data class TPQueryResultItem(val providerId: UniqueId, val name: String) {
    constructor(providerId: String, name: String) : this(UniqueId.fromString(providerId), name)
}

class TPQueryResult(data: List<TPQueryResultItem>) : ArrayList<TPQueryResultItem>(data)

interface TPQueryTask : BlockingTask<TPQueryParams, TPQueryResult>

class TPQueryTaskImpl(registry: Registry) : BaseBlockingTask<TPQueryParams, TPQueryResult>(), TPQueryTask {
    private val ses = registry.get(EventStore::class.java)

    override fun exec(ctx: ExecutionContext, input: TPQueryParams): TPQueryResult {
        if (input.providerId != null && input.nameIsLike != null) {
            val id = queryById(input.providerId)
            val likeString = LikeString(input.nameIsLike)
            return TPQueryResult(id.filter { likeString.matches(it.name) })
        }
        if (input.providerId != null) return queryById(input.providerId)
        if (input.nameIsLike != null) return queryByName(input.nameIsLike)
        if (input.providerId == null && input.nameIsLike == null) return queryAll()
        throw RuntimeException("Opps, unknown query combination: $input")
    }

    private fun queryById(providerId: UniqueId): TPQueryResult {
        val query = AllOfQuery(
            EventTypeQuery(eventType = TPProviderRegisteredEventFactory.eventType()),
            AggregateIdQuery(aggregateId = providerId.toString())
        )
        return runEventsQuery(query)
    }

    private fun queryAll(): TPQueryResult {
        val query = EventTypeQuery(eventType = TPProviderRegisteredEventFactory.eventType())
        return runEventsQuery(query)
    }

    private fun queryByName(nameLike: String): TPQueryResult {
        val all = queryAll()
        val likeString = LikeString(nameLike)
        return TPQueryResult(all.filter { likeString.matches(it.name) })
    }

    private fun runEventsQuery(query: EventQuery): TPQueryResult {
        val items = ses.read(query)
            .map {
                TPQueryResultItem(
                    it.aggregateId!!,
                    it.payloadAs(TPRegisterProviderRequest::class.java).providerName
                )
            }
        return TPQueryResult(items)
    }
}