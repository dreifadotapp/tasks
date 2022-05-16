package dreifa.app.tasks.inbuilt.fileBundle

import dreifa.app.fileBundle.FileBundle
import dreifa.app.fileBundle.TextBundleItem
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.fileBundle.builders.FileBundleBuilder
import dreifa.app.registry.Registry
import dreifa.app.ses.Event
import dreifa.app.ses.EventFactory
import dreifa.app.ses.EventStore
import dreifa.app.sks.SKS
import dreifa.app.sks.SKSKeyValue
import dreifa.app.sks.SKSValueType
import dreifa.app.tasks.*
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.inbuilt.providers.TPQueryTask
import dreifa.app.types.Key
import dreifa.app.types.UniqueId

interface FBStoreTask : BlockingTask<String, Unit>, TaskDoc<String, Unit> {
    override fun taskName(): String = TPQueryTask::class.simpleName!!
}

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

class FBStoreTaskImpl(registry: Registry) : BlockingTask<String, Unit>, FBStoreTask {
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

    override fun description(): String = "This task stores a FileBundle. The FileBundle must be passed in its text " +
            "format by using the `dreifa.app.fileBundle.adapters.TextAdapter` adapter"

    override fun examples(): List<TaskExample<String, Unit>> {
        return TaskExamplesBuilder()
            .example("A simple FileBundle ")
            .input(TextAdapter().fromBundle(Fixtures.helloWorldBundle()))
            .done()
            .build()
    }

    object Fixtures {
        fun helloWorldBundle(): FileBundle {
            return FileBundleBuilder()
                .withName("hello-world-bundle")
                .withId(UniqueId.randomUUID())
                .addItem(TextBundleItem("greeting.txt", "Hello, world"))
                .build()
        }
    }

}