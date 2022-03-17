package dreifa.app.tasks.inbuilt.providers

import dreifa.app.fileBundle.adapters.FilesAdapter
import dreifa.app.fileBundle.adapters.TextAdapter
import dreifa.app.registry.Registry
import dreifa.app.tasks.BaseBlockingTask
import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.Locations
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.inbuilt.fileBundle.FBRetrieveTaskImpl
import dreifa.app.types.StringList
import dreifa.app.types.UniqueId
import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfoList
import java.io.File
import java.lang.RuntimeException
import java.net.URL
import java.net.URLClassLoader

data class TPScanJarRequest(
    val bundleId: UniqueId,
    val packageNames: StringList = StringList(emptyList())
) {
    constructor(bundleId: String, packageNames: List<String> = emptyList()) : this(
        UniqueId.fromString(
            bundleId
        ), StringList(packageNames)
    )
}

interface TPScanJarTask : BlockingTask<TPScanJarRequest, StringList>

class TPScanJarTaskImpl(reg: Registry) : BaseBlockingTask<TPScanJarRequest, StringList>(), TPScanJarTask {
    private val retrieveBundleTask = FBRetrieveTaskImpl(reg)
    private val locations = reg.get(Locations::class.java)
    private val adapter = TextAdapter()
    override fun exec(ctx: ExecutionContext, input: TPScanJarRequest): StringList {
        val bundle = adapter.toBundle(retrieveBundleTask.exec(ctx, input.bundleId))
        if (bundle.items.size != 1) {
            throw RuntimeException("Expected one item in FileBundle(${input.bundleId}), found ${bundle.items.size}")
        }

        val tmpDir = tempDir(input)
        val bundleAdapter = FilesAdapter(tmpDir)
        bundleAdapter.fromBundle(bundle)

        val url = URL("file:$tmpDir/${bundle.items[0].path}")
        val loader = URLClassLoader(listOf(url).toTypedArray())
        val builder = ClassGraph()
            .enableAllInfo()
            .enableRemoteJarScanning()
            .addClassLoader(loader)
        if (input.packageNames.isNotEmpty()) {
            builder.acceptPackages(*input.packageNames.toTypedArray())
        }

        val graph = builder.scan()
        val registrations: ClassInfoList = graph.getClassesImplementing("dreifa.app.tasks.TaskRegistrations")
        val result = registrations
            .filter { it.classpathElementURL == url }
            .map { it.name }

        return StringList(result)
    }

    private fun tempDir(input: TPScanJarRequest): String {
        val dir = "${locations.tempDirectory()}/${input.bundleId}"
        val f = File(dir)
        f.mkdirs()
        return f.canonicalPath
    }
}