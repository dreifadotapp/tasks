package dreifa.app.tasks.inbuilt.providers

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dreifa.app.fileBundle.BinaryBundleItem
import dreifa.app.fileBundle.FileBundle
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.fileBundle.builders.FileBundleBuilder
import dreifa.app.registry.Registry
import dreifa.app.ses.*
import dreifa.app.sks.SKS
import dreifa.app.sks.SimpleKVStore
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
            bundle.id, providerId, "Example Provider"
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
            id: UniqueId = UniqueId.randomUUID()
        ): FileBundle {
            return FileBundleBuilder()
                .withName("Terraform Tasks")
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

    }
}