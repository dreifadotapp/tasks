package dreifa.app.tasks.inbuilt.fileBundle

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.throws
import dreifa.app.fileBundle.FileBundle
import dreifa.app.fileBundle.TextBundleItem
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.fileBundle.builders.FileBundleBuilder
import dreifa.app.registry.Registry
import dreifa.app.ses.EventStore
import dreifa.app.ses.EverythingQuery
import dreifa.app.ses.InMemoryEventStore
import dreifa.app.sks.SKS
import dreifa.app.sks.SimpleKVStore
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.types.Key
import dreifa.app.types.UniqueId
import org.junit.jupiter.api.Test
import java.lang.RuntimeException

class FBTasksTest {
    private val adapter = TextAdapter()

    @Test
    fun `should upload FileBundle`() {
        // 1. setup
        val (reg, ses, sks) = setupRegistry()
        val ctx = SimpleExecutionContext()
        val bundle = Fixtures.helloWorldBundle()

        // 2. test
        FBStoreTaskImpl(reg).exec(ctx, adapter.fromBundle(bundle))

        // 3. verify
        val events = ses.read(EverythingQuery)
        assertThat(events.size, equalTo(1))
        assertThat(events[0].aggregateId, equalTo(bundle.id.toString()))
        val key = Key.fromUniqueId(bundle.id)
        assert(sks.exists(key))
    }

    @Test
    fun `should query for FileBundle`() {
        // 1. setup
        val (reg, _, _) = setupRegistry()
        val ctx = SimpleExecutionContext()
        val bundle1 = Fixtures.helloWorldBundle(UniqueId.fromString("001"), "Bundle1")
        val bundle2 = Fixtures.helloWorldBundle(UniqueId.fromString("002"), "Bundle2")
        val bundle3 = Fixtures.helloWorldBundle(UniqueId.fromString("003"), "bundleThree")
        val uploadTask = FBStoreTaskImpl(reg)
        val queryTask = FBQueryTaskImpl(reg)
        uploadTask.exec(ctx, adapter.fromBundle(bundle1))
        uploadTask.exec(ctx, adapter.fromBundle(bundle2))
        uploadTask.exec(ctx, adapter.fromBundle(bundle3))

        // 2. Query no filter
        assertThat(queryTask.exec(ctx, FBQueryParams()).size, equalTo(3))
        assertThat(
            queryTask.exec(ctx, FBQueryParams()),
            equalTo(
                FBQueryResult(
                    listOf(
                        FBQueryResultItem("001", "Bundle1"),
                        FBQueryResultItem("002", "Bundle2"),
                        FBQueryResultItem("003", "bundleThree")
                    )
                )
            )
        )

        // 3. Query with bundleId filter
        assertThat(queryTask.exec(ctx, FBQueryParams(bundleId = UniqueId("002"))).size, equalTo(1))
        assertThat(
            queryTask.exec(ctx, FBQueryParams(bundleId = UniqueId("002"))),
            equalTo(
                FBQueryResult(
                    listOf(
                        FBQueryResultItem("002", "Bundle2"),
                    )
                )
            )
        )

        // 3. Query with nameLike filter
        assertThat(queryTask.exec(ctx, FBQueryParams(nameIsLike = "Bundle%")).size, equalTo(2))

        // 3. Query with all filters
        assertThat(queryTask.exec(ctx, FBQueryParams("001", "Bundle%")).size, equalTo(1))
        assertThat(queryTask.exec(ctx, FBQueryParams("003", "Bundle%")).size, equalTo(0))
    }

    @Test
    fun `should retrieve stored bundle`() {
        // 1. setup
        val (reg, ses, sks) = setupRegistry()
        val ctx = SimpleExecutionContext()
        val bundle = Fixtures.helloWorldBundle()

        // 2. test
        FBStoreTaskImpl(reg).exec(ctx, adapter.fromBundle(bundle))

        // 3. verify can read
        val retrievedBundle = adapter.toBundle(FBRetrieveTaskImpl(reg).exec(ctx, bundle.id))
        assertThat(retrievedBundle, equalTo(bundle))

        // 3. verify missing bundle
        val randomId = UniqueId.randomUUID()
        assertThat(
            { FBRetrieveTaskImpl(reg).exec(ctx, randomId) },
            throws<RuntimeException>(
                has(
                    Exception::message,
                    present(equalTo("No FileBundle for id:$randomId"))
                )
            )
        )
    }

    private fun setupRegistry(): Triple<Registry, EventStore, SKS> {
        val ses = InMemoryEventStore()
        val sks = SimpleKVStore()
        val registry = Registry().store(ses).store(sks)
        return Triple(registry, ses, sks)
    }

    object Fixtures {
        fun helloWorldBundle(
            id: UniqueId = UniqueId.randomUUID(),
            name: String = "HelloWorldBundle"
        ): FileBundle {
            return FileBundleBuilder()
                .withName(name)
                .withId(id)
                .addItem(TextBundleItem("greeting.txt", "Hello, world"))
                .build()
        }
    }
}
