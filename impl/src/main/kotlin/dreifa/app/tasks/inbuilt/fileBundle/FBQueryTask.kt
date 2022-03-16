package dreifa.app.tasks.inbuilt.fileBundle

import dreifa.app.registry.Registry
import dreifa.app.ses.*
import dreifa.app.tasks.BaseBlockingTask
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.types.LikeString
import dreifa.app.types.UniqueId
import java.lang.RuntimeException

data class FBQueryParams(
    val bundleId: UniqueId? = null,
    val nameIsLike: String? = null
) {
    constructor(bundleId: String, name: String) : this(UniqueId.fromString(bundleId), name)
}

data class FBQueryResultItem(val bundleId: UniqueId, val name: String) {
    constructor(bundleId: String, name: String) : this(UniqueId.fromString(bundleId), name)
}

class FBQueryResult(data: List<FBQueryResultItem>) : ArrayList<FBQueryResultItem>(data)

interface FBQueryTask : BlockingTask<FBQueryParams, FBQueryResult>

class FBQueryTaskImpl(registry: Registry) : BaseBlockingTask<FBQueryParams, FBQueryResult>(), FBQueryTask {
    private val ses = registry.get(EventStore::class.java)

    override fun exec(ctx: ExecutionContext, input: FBQueryParams): FBQueryResult {
        if (input.bundleId != null && input.nameIsLike != null) {
            val id = queryById(input.bundleId)
            val likeString = LikeString(input.nameIsLike)
            return FBQueryResult(id.filter { likeString.matches(it.name) })
        }
        if (input.bundleId != null) return queryById(input.bundleId)
        if (input.nameIsLike != null) return queryByName(input.nameIsLike)
        if (input.bundleId == null && input.nameIsLike == null) return queryAll()
        throw RuntimeException("Opps, unknown a query combination: $input")
    }

    private fun queryById(bundleId: UniqueId): FBQueryResult {
        val query = AllOfQuery(
            listOf(
                EventTypeQuery(eventType = FBUploadedEventFactory.eventType()),
                AggregateIdQuery(aggregateId = bundleId.toString())
            )
        )
        return runEventsQuery(query)
    }

    private fun queryAll(): FBQueryResult {
        val query = EventTypeQuery(eventType = FBUploadedEventFactory.eventType())
        return runEventsQuery(query)
    }

    private fun queryByName(nameLike: String): FBQueryResult {
        val all = queryAll()
        val likeString = LikeString(nameLike)
        return FBQueryResult(all.filter { likeString.matches(it.name) })
    }

    private fun runEventsQuery(query: EventQuery): FBQueryResult {
        val items = ses.read(query).map {
            FBQueryResultItem(UniqueId(it.aggregateId!!), it.payload as String)
        }
        return FBQueryResult(items)
    }
}

object Queries {


}