package dreifa.app.tasks.inbuilt.providers

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import dreifa.app.fileBundle.BinaryBundleItem
import dreifa.app.fileBundle.FileBundle
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.fileBundle.builders.FileBundleBuilder
import dreifa.app.registry.Registry
import dreifa.app.ses.*
import dreifa.app.sks.SKS
import dreifa.app.sks.SimpleKVStore
import dreifa.app.tasks.TaskFactory
import dreifa.app.tasks.TestLocations
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.tasks.inbuilt.fileBundle.*
import dreifa.app.types.StringList
import dreifa.app.types.UniqueId
import org.junit.jupiter.api.Test

class TPTasksTest {
    private val adapter = TextAdapter()

    @Test
    fun `should scan jar`() {
        // 1. setup
        val (reg, ses, sks) = setupRegistry()
        val ctx = SimpleExecutionContext()
        val bundle = Fixtures.terraformTaskJar()
        val bundleId = bundle.id.toString()
        FBStoreTaskImpl(reg).exec(ctx, adapter.fromBundle(bundle))

        // 2a. Scan the uploaded Jar with no package filter
        val resultA = TPScanJarTaskImpl(reg).exec(ctx, TPScanJarRequest(bundleId))
        assertThat(resultA, equalTo(StringList(listOf("dreifa.app.terraform.tasks.TFTasks"))))

        // 2b. Scan the uploaded Jar with package filter
        val resultB = TPScanJarTaskImpl(reg).exec(ctx, TPScanJarRequest(bundleId, listOf("dreifa.app")))
        assertThat(resultB, equalTo(StringList(listOf("dreifa.app.terraform.tasks.TFTasks"))))

        // 2c. Scan the uploaded Jar with bad package filter
        val resultC = TPScanJarTaskImpl(reg).exec(ctx, TPScanJarRequest(bundleId, listOf("not.exist")))
        assert(resultC.isEmpty())
    }

    @Test
    fun `should register provider by generating an event`() {
        // 1. Setup
        val (reg, ses, sks) = setupRegistry()
        val bundle = Fixtures.terraformTaskJar()
        Pipelines.storeJar(reg, bundle)

        // 2. Run Task
        val ctx = SimpleExecutionContext()
        val providerId = UniqueId.alphanumeric()
        val request = TPRegisterProviderRequest(
            bundle.id, providerId, "com.example.Provider",
            "Example Provider"
        )
        TPRegisterProviderTaskImpl(reg).exec(ctx, request)

        // 3. Check events
        val query = AllOfQuery(
            listOf(
                EventTypeQuery(eventType = TPProviderRegisteredEventFactory.eventType()),
                AggregateIdQuery(aggregateId = providerId.toString())
            )
        )

        val events = ses.read(query)
        assertThat(events.size, equalTo(1))
    }

    @Test
    fun `should query for Task Providers`() {
        // 1. setup
        val (reg, _, _) = setupRegistry()
        val ctx = SimpleExecutionContext()
        Pipelines.registerProvider(reg, UniqueId.fromString("001"), "com.example.Provider1", "Provider1")
        Pipelines.registerProvider(reg, UniqueId.fromString("002"), "com.example.Provider2", "Provider2")
        Pipelines.registerProvider(reg, UniqueId.fromString("003"), "com.example.Provider3", "providerThree")

        val queryTask = TPQueryTaskImpl(reg)
        //val infoTask = TPInfoTaskImpl(reg)

        // 2. Query no filter
        assertThat(queryTask.exec(ctx, TPQueryParams()).size, equalTo(3))
        assertThat(
            queryTask.exec(ctx, TPQueryParams()),
            equalTo(
                TPQueryResult(
                    listOf(
                        TPQueryResultItem("001", "com.example.Provider1", "Provider1"),
                        TPQueryResultItem("002", "com.example.Provider2", "Provider2"),
                        TPQueryResultItem("003", "com.example.Provider3", "providerThree")
                    )
                )
            )
        )

        // 3. Query with providerId filter
        assertThat(queryTask.exec(ctx, TPQueryParams(providerId = UniqueId("002"))).size, equalTo(1))
        assertThat(
            queryTask.exec(ctx, TPQueryParams(providerId = UniqueId("002"))),
            equalTo(
                TPQueryResult(
                    listOf(
                        TPQueryResultItem("002", "com.example.Provider2", "Provider2"),
                    )
                )
            )
        )

        // 3. Query with nameLike filter
        assertThat(queryTask.exec(ctx, TPQueryParams(nameIsLike = "Provider%")).size, equalTo(2))

        // 3. Query with all filters
        assertThat(queryTask.exec(ctx, TPQueryParams("001", "Provider%")).size, equalTo(1))
        assertThat(queryTask.exec(ctx, TPQueryParams("003", "Provider%")).size, equalTo(0))
    }

    @Test
    fun `should return info for registered providers`() {
        // 1. setup
        val (reg, _, _) = setupRegistry()
        val bundle = Fixtures.terraformTaskJar()
        Pipelines.registerProvider(
            reg, UniqueId.fromString("001"),
            "com.example.Provider1",
            "Provider1",
            bundle
        )

        // 2. Get info back for a provider
        val ctx = SimpleExecutionContext()
        val infoTask = TPInfoTaskImpl(reg)
        val info = infoTask.exec(ctx, UniqueId("001"))
        assertThat(info.providerClazz, equalTo("com.example.Provider1"))
        assertThat(info.providerId, equalTo(UniqueId("001")))
        assertThat(info.jarBundleId, equalTo(bundle.id))
    }

    @Test
    fun `should load TaskFactory for the provider`() {
        val (reg, _, _) = setupRegistry()
        val (providerId, _) = Pipelines.registerProvider(
            reg = reg,
            providerClazz = "dreifa.app.terraform.tasks.TFTasks" // must be the correct class name
        )

        // get back a TaskFactory
        val ctx = SimpleExecutionContext()
        val result = TPLoadTaskFactoryTaskImpl(reg).exec(ctx, providerId)

        assert(result is TaskFactory)
        assertThat(result.list(), !isEmpty)
    }

    private fun setupRegistry(): Triple<Registry, EventStore, SKS> {
        val ses = InMemoryEventStore()
        val sks = SimpleKVStore()
        val location = TestLocations(baseDir = "..")
        val registry = Registry().store(ses).store(sks).store(location)
        return Triple(registry, ses, sks)
    }

    object Fixtures {
        /**
         * Use a prebuilt copy of the terraform-tasks jar
         * See https://github.com/dreifadotapp/terraform-tasks
         */
        fun terraformTaskJar(
            id: UniqueId = UniqueId.randomUUID(),
            name: String = "Terraform Tasks"
        ): FileBundle {
            return FileBundleBuilder()
                .withName(name)
                .withId(id)
                .addItem(BinaryBundleItem.fromResource("/terraform-tasks.jar", "terraform-tasks.jar"))
                .build()
        }
    }

    // some prebuilt pipelines to get the system into the correct state
    object Pipelines {

        fun storeJar(
            reg: Registry,
            bundle: FileBundle = Fixtures.terraformTaskJar()
        ) {
            val ctx = SimpleExecutionContext()
            val adapter = TextAdapter()
            FBStoreTaskImpl(reg).exec(ctx, adapter.fromBundle(bundle))
        }

        fun registerProvider(
            reg: Registry,
            providerId: UniqueId = UniqueId.alphanumeric(),
            providerClazz: String = "com.example.Tasks", // TODO - how to set this up better - it must a the name of class in the bundle
            providerName: String = "Example Tasks", // TODO - how to set this up better - it must a the name of class in the bundle
            bundle: FileBundle = Fixtures.terraformTaskJar(),
        ): Pair<UniqueId, UniqueId> {
            storeJar(reg, bundle)
            val ctx = SimpleExecutionContext()
            val input = TPRegisterProviderRequest(
                jarBundleId = bundle.id,
                providerId = providerId,
                providerClazz = providerClazz,
                providerName = providerName
            )
            TPRegisterProviderTaskImpl(reg).exec(ctx, input)
            return Pair(providerId, bundle.id)
        }
    }
}