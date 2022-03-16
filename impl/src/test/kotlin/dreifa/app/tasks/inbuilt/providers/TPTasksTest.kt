package dreifa.app.tasks.inbuilt.providers

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dreifa.app.fileBundle.BinaryBundleItem
import dreifa.app.fileBundle.FileBundle
import dreifa.app.fileBundle.builders.FileBundleBuilder
import dreifa.app.registry.Registry
import dreifa.app.ses.EventStore
import dreifa.app.ses.InMemoryEventStore
import dreifa.app.sks.SKS
import dreifa.app.sks.SimpleKVStore
import dreifa.app.tasks.TestLocations
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.tasks.inbuilt.fileBundle.*
import dreifa.app.types.StringList
import dreifa.app.types.UniqueId
import org.junit.jupiter.api.Test

class TPTasksTest {

    @Test
    fun `should scan jar `() {
        // 1. setup
        val (reg, ses, sks) = setupRegistry()
        val ctx = SimpleExecutionContext()
        val bundle = Fixtures.terraformTaskJar()
        FBStoreTaskImpl(reg).exec(ctx, bundle)

        // Scan the uploaded Jar
        val request = TPScanJarRequest(bundle.id)
        val result = TPScanJarTaskImpl(reg).exec(ctx, request)

        assertThat(result, equalTo(StringList(listOf("dreifa.app.terraform.tasks.TFTasks"))))
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
}