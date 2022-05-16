package dreifa.app.tasks.inbuilt.classloader

import dreifa.app.fileBundle.adapters.FilesAdapter
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.registry.Registry
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.Locations
import dreifa.app.tasks.NotRemotableTask
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.inbuilt.fileBundle.FBRetrieveTaskImpl
import dreifa.app.tasks.inbuilt.providers.TPQueryTask
import dreifa.app.types.UniqueId
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.HashMap

interface CLLoadJarTask : BlockingTask<UniqueId, URLClassLoader>, NotRemotableTask {
    override fun taskName(): String = TPQueryTask::class.simpleName!!
}

class CLLoadJarTaskImpl(reg: Registry) : BlockingTask<UniqueId, URLClassLoader>, CLLoadJarTask {
    private val retrieveBundleTask = FBRetrieveTaskImpl(reg)
    private val locations = reg.get(Locations::class.java)
    private val adapter = TextAdapter()

    override fun exec(ctx: ExecutionContext, input: UniqueId): URLClassLoader {
        if (!ClazzLoaderCache.contains(input)) {
            val jarFilePath = downloadJarToTempDirectory(ctx, input, input)

            val classLoader = URLClassLoader(
                arrayOf<URL>(File(jarFilePath).toURI().toURL()),
                this.javaClass.classLoader
            )

            ClazzLoaderCache.store(input, classLoader)
        }
        return ClazzLoaderCache.lookup(input)
    }

    private fun downloadJarToTempDirectory(
        ctx: ExecutionContext,
        bundleId: UniqueId,
        input: UniqueId
    ): String {
        val bundle = adapter.toBundle(retrieveBundleTask.exec(ctx, bundleId))
        if (bundle.items.size != 1) {
            throw RuntimeException("Expected one item in FileBundle($bundleId), found ${bundle.items.size}")
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

object ClazzLoaderCache {
    private val lookup = HashMap<UniqueId, URLClassLoader>()
    fun store(bundleId: UniqueId, loader: URLClassLoader) {
        synchronized(this) {
            lookup[bundleId] = loader
        }
    }

    fun lookup(bundleId: UniqueId): URLClassLoader {
        synchronized(this) {
            return lookup[bundleId]!!
        }
    }

    fun contains(bundleId: UniqueId): Boolean {
        synchronized(this) {
            return lookup.contains(bundleId)
        }
    }
}