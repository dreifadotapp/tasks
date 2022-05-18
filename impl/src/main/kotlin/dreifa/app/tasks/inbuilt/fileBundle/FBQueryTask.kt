package dreifa.app.tasks.inbuilt.fileBundle

import dreifa.app.registry.Registry
import dreifa.app.ses.*
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.eventClientContext
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.inbuilt.providers.TPQueryTask
import dreifa.app.types.LikeString
import dreifa.app.types.UniqueId

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

interface FBQueryTask : BlockingTask<FBQueryParams, FBQueryResult> {
    override fun taskName(): String = TPQueryTask::class.simpleName!!
}

class FBQueryTaskImpl(registry: Registry) : BlockingTask<FBQueryParams, FBQueryResult>, FBQueryTask {
    private val ses = registry.get(EventStore::class.java)

    override fun exec(ctx: ExecutionContext, input: FBQueryParams): FBQueryResult {
        val etx = ctx.eventClientContext()
        if (input.bundleId != null && input.nameIsLike != null) {
            val id = queryById(etx, input.bundleId)
            val likeString = LikeString(input.nameIsLike)
            return FBQueryResult(id.filter { likeString.matches(it.name) })
        }
        if (input.bundleId != null) return queryById(etx, input.bundleId)
        if (input.nameIsLike != null) return queryByName(etx, input.nameIsLike)
        return queryAll(etx)   // no params provided, so return all
    }

    private fun queryById(etx: ClientContext, bundleId: UniqueId): FBQueryResult {
        val query = AllOfQuery(
            listOf(
                EventTypeQuery(eventType = FBStoredEventFactory.eventType()),
                AggregateIdQuery(aggregateId = bundleId.toString())
            )
        )
        return runEventsQuery(etx, query)
    }

    private fun queryAll(etx: ClientContext): FBQueryResult {
        val query = EventTypeQuery(eventType = FBStoredEventFactory.eventType())
        return runEventsQuery(etx, query)
    }

    private fun queryByName(etx: ClientContext, nameLike: String): FBQueryResult {
        val all = queryAll(etx)
        val likeString = LikeString(nameLike)
        return FBQueryResult(all.filter { likeString.matches(it.name) })
    }

    private fun runEventsQuery(etx: ClientContext, query: EventQuery): FBQueryResult {
        val items = ses.read(etx, query).map { FBQueryResultItem(it.aggregateId!!, it.payload as String) }
        return FBQueryResult(items)
    }
}