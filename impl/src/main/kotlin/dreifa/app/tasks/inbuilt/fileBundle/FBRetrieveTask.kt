package dreifa.app.tasks.inbuilt.fileBundle

import dreifa.app.fileBundle.FileBundle
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.registry.Registry
import dreifa.app.ses.*
import dreifa.app.sks.SKS
import dreifa.app.tasks.BaseBlockingTask
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.types.Key
import dreifa.app.types.UniqueId
import java.lang.RuntimeException

interface FBRetrieveTask : BlockingTask<UniqueId, FileBundle>

class FBRetrieveTaskImpl(registry: Registry) : BaseBlockingTask<UniqueId, FileBundle>(), FBRetrieveTask {
    private val ses = registry.get(EventStore::class.java)
    private val sks = registry.get(SKS::class.java)

    override fun exec(ctx: ExecutionContext, input: UniqueId): FileBundle {
        if (!bundleStoredEventExists(input)) throw RuntimeException("No FileBundle for id:$input")

        val key = Key.fromUniqueId(input)
        if (!sks.exists(key)) throw RuntimeException("Internal error - no FileBundle for id:$input found in KV store")
        val bundleAsText = sks.get(key).value as String
        return TextAdapter().toBundle(bundleAsText)
    }

    private fun bundleStoredEventExists(bundleId: UniqueId): Boolean {
        val query = AllOfQuery(
            listOf(
                EventTypeQuery(eventType = FBStoredEventFactory.eventType()),
                AggregateIdQuery(aggregateId = bundleId.toString())
            )
        )
        return ses.read(query).isNotEmpty()
    }
}