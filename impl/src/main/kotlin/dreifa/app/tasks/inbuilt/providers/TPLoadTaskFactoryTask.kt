package dreifa.app.tasks.inbuilt.providers

import dreifa.app.fileBundle.adapters.FilesAdapter
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.registry.Registry
import dreifa.app.tasks.*
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.inbuilt.classloader.CLLoadJarTaskImpl
import dreifa.app.tasks.inbuilt.fileBundle.FBRetrieveTaskImpl
import dreifa.app.types.UniqueId
import java.io.File

/**
 * This Task loads a provider for use. Thr provider must have been registered using
 * the TPRegisterProviderTask.
 *
 * Note this 'NotRemotableTask', i.e the resulting TaskFactory can only be used locally
 *
 * This task does three things:
 *  - download the JAR file(s) in the associated FileBundler to local storage
 *  - setup a custom Java classloader for the JAR files(s)
 *  - configure and return a TaskFactory
 */
interface TPLoadTaskFactoryTask : BlockingTask<UniqueId, TaskFactory>, NotRemotableTask {
    override fun taskName(): String = TPQueryTask::class.simpleName!!
}

class TPLoadTaskFactoryTaskImpl(private val reg: Registry) : BlockingTask<UniqueId, TaskFactory>,
    TPLoadTaskFactoryTask {
    private val retrieveBundleTask = FBRetrieveTaskImpl(reg)
    private val locations = reg.get(Locations::class.java)
    private val adapter = TextAdapter()

    override fun exec(ctx: ExecutionContext, input: UniqueId): TaskFactory {
        val provider = TPInfoTaskImpl(reg).exec(ctx, input)

        val clazzLoader = CLLoadJarTaskImpl(reg).exec(ctx, provider.jarBundleId)
        val factory = TaskFactory(reg, clazzLoader)
        factory.register(provider.providerClazz)

        return factory
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