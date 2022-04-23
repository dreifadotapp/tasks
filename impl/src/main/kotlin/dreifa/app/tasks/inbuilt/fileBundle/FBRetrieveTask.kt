package dreifa.app.tasks.inbuilt.fileBundle

import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.registry.Registry
import dreifa.app.ses.*
import dreifa.app.sks.SKS
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.types.Key
import dreifa.app.types.UniqueId
import java.lang.RuntimeException

interface FBRetrieveTask : BlockingTask<UniqueId, String>

class FBRetrieveTaskImpl(registry: Registry) : BlockingTask<UniqueId, String>, FBRetrieveTask {
    private val ses = registry.get(EventStore::class.java)
    private val sks = registry.get(SKS::class.java)

    override fun exec(ctx: ExecutionContext, input: UniqueId): String {
        if (!bundleStoredEventExists(input)) throw RuntimeException("No FileBundle for id:$input")

        val key = Key.fromUniqueId(input)
        if (!sks.exists(key)) throw RuntimeException("Internal error - no FileBundle for id:$input found in KV store")

        try {
            val bundleAsText = sks.get(key).value as String
            TextAdapter().toBundle(bundleAsText)
            return bundleAsText
        } catch (re: RuntimeException) {
            throw RuntimeException("The value stored for key:$key is not a valid FileBundle")
        }
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