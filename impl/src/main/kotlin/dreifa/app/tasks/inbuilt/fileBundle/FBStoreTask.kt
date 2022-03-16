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

interface FBStoreTask : BlockingTask<FileBundle, Unit>

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

class FBStoreTaskImpl(registry: Registry) : BaseBlockingTask<FileBundle, Unit>(), FBStoreTask {
    private val ses = registry.get(EventStore::class.java)
    private val sks = registry.get(SKS::class.java)

    override fun exec(ctx: ExecutionContext, input: FileBundle) {
        val bundleAdapter = TextAdapter()

        // store content in KV store
        val text = bundleAdapter.fromBundle(input)
        val kv = SKSKeyValue(
            Key.fromUniqueId(input.id),
            text,
            SKSValueType.Text
        )
        sks.put(kv)
        // store event - the bundle is only considered "committed" once the event is written
        ses.store(FBStoredEventFactory.create(input))
    }
}