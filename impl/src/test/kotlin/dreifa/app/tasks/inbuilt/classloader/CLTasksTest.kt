package dreifa.app.tasks.inbuilt.classloader

import dreifa.app.fileBundle.BinaryBundleItem
import dreifa.app.fileBundle.FileBundle
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.fileBundle.builders.FileBundleBuilder
import dreifa.app.registry.Registry
import dreifa.app.ses.EventStore
import dreifa.app.ses.InMemoryEventStore
import dreifa.app.sks.SKS
import dreifa.app.sks.SimpleKVStore
import dreifa.app.tasks.TestLocations
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.tasks.inbuilt.fileBundle.FBStoreTaskImpl
import dreifa.app.types.UniqueId
import org.junit.jupiter.api.Test

class CLTasksTest {
    private val adapter = TextAdapter()

    @Test
    fun `should load JAR in existing FileBundle`() {
        // 1. setup
        val (reg, _, _) = setupRegistry()
        val ctx = SimpleExecutionContext()
        val bundle = Fixtures.terraformTaskJar()
        FBStoreTaskImpl(reg).exec(ctx, adapter.fromBundle(bundle))

        // 2. load the JAR
        val loader1 = CLLoadJarTaskImpl(reg).exec(ctx,  bundle.id)

        // 3. Subsequent call should use the cached ClassLoader
        val loader2 = CLLoadJarTaskImpl(reg).exec(ctx,  bundle.id)

        assert(loader1 == loader2)
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
}