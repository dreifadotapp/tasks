package dreifa.app.tasks.inbuilt.providers

import dreifa.app.fileBundle.adapters.FilesAdapter
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.registry.Registry
import dreifa.app.ses.*
import dreifa.app.tasks.BaseBlockingTask
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.Locations
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.inbuilt.fileBundle.FBRetrieveTaskImpl
import dreifa.app.types.UniqueId
import java.io.File
import java.net.URL
import java.net.URLClassLoader


data class TPLoadProviderResult(
    val providerId: UniqueId
)

/**
 * This Task loads a provider for use. Thr provider must have been registered using
 * the TPRegisterProviderTask.
 *
 * This task does three things:
 *  - download the JAR file(s) in the associated FileBundler to local storage
 *  - setup a custom Java classloader for the JAR files(s)
 *  - configure and return a TaskRegistry
 */
interface TPLoadProviderTask : BlockingTask<UniqueId, TPLoadProviderResult>

class TPLoadProviderTaskImpl(private val reg: Registry) : BaseBlockingTask<UniqueId, TPLoadProviderResult>(),
    TPLoadProviderTask {
    //private val ses = reg.get(EventStore::class.java)
    private val retrieveBundleTask = FBRetrieveTaskImpl(reg)
    private val locations = reg.get(Locations::class.java)
    private val adapter = TextAdapter()

    override fun exec(ctx: ExecutionContext, input: UniqueId): TPLoadProviderResult {
        val provider = TPInfoTaskImpl(reg).exec(ctx, input)

        val jarFilePath = downloadJarToTempDirectory(ctx, provider, input)

        val classLoader = URLClassLoader(
            arrayOf<URL>(File(jarFilePath).toURI().toURL()),
            this.javaClass.classLoader
        )


        println(classLoader)
        println(provider.providerClazz)


        return TPLoadProviderResult(provider.providerId)
    }

    private fun downloadJarToTempDirectory(
        ctx: ExecutionContext,
        provider: TPInfoResult,
        input: UniqueId
    ): String {
        val bundle = adapter.toBundle(retrieveBundleTask.exec(ctx, provider.jarBundleId))
        if (bundle.items.size != 1) {
            throw RuntimeException("Expected one item in FileBundle(${provider.jarBundleId}), found ${bundle.items.size}")
        }

        val tmpDir = tempDir(input)
        val bundleAdapter = FilesAdapter(tmpDir)
        bundleAdapter.fromBundle(bundle)
        return "${tmpDir}/${bundle.items.single().path}"
    }

    private fun tempDir(input: UniqueId): String {
        val dir = "${locations.tempDirectory()}/${input}"
        val f = File(dir)
        f.mkdirs()
        return f.canonicalPath
    }
}