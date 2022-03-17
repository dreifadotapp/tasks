package dreifa.app.tasks.inbuilt.fileBundle

import dreifa.app.fileBundle.FileBundle
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.registry.Registry
import dreifa.app.ses.Event
import dreifa.app.ses.EventFactory
import dreifa.app.ses.EventStore
import dreifa.app.sks.SKS
import dreifa.app.sks.SKSKeyValue
import dreifa.app.sks.SKSValueType
import dreifa.app.tasks.BaseBlockingTask
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.types.Key

interface FBStoreTask : BlockingTask<String, Unit>

object FBStoredEventFactory : EventFactory {
    fun create(params: FileBundle): Event {
        return Event(
            type = eventType(),
            aggregateId = params.id.toString(),
            payload = params.name
        )
    }

    override fun eventType(): String = "dreifa.app.tasks.inbuilt.fileBundle.FBStored"
}

class FBStoreTaskImpl(registry: Registry) : BaseBlockingTask<String, Unit>(), FBStoreTask {
    private val ses = registry.get(EventStore::class.java)
    private val sks = registry.get(SKS::class.java)

    override fun exec(ctx: ExecutionContext, input: String) {
        val bundleAdapter = TextAdapter()
        val bundle = bundleAdapter.toBundle(input)

        // store content in KV store
        val kv = SKSKeyValue(
            Key.fromUniqueId(bundle.id),
            input,
            SKSValueType.Text
        )
        sks.put(kv)
        // store event - the bundle is only considered "committed" once the event is written
        ses.store(FBStoredEventFactory.create(bundle))
    }
}