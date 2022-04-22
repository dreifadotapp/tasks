package dreifa.app.tasks

import dreifa.app.helpers.random
import dreifa.app.tasks.executionContext.ExecutionContext
import java.io.File

/**
 * One class for all location information. The implementing class should ensure that the
 * requested directories exist.
 */
interface Locations {

    /**
     * The home directory for a service
     */
    fun serviceHomeDirectory(product: String, service: String? = null, instance: String? = null): String

    /**
     * The home directory for a service, picking up the
     * instance qualifier from the execution context
     */
    fun serviceHomeDirectory(ctx: ExecutionContext, product: String, service: String? = null): String {
        return serviceHomeDirectory(product, service, ctx.instanceQualifier()?.lowercase())
    }

    /**
     * Where any cached data is stored.
     */
    fun cacheDirectory(): String

    /**
     * Where any persistent data is stored
     */
    fun dataDirectory(): String

    /**
     * The place for any temporary files. Applications are responsible for ensuring the uniqueness of any
     * files stored here
     */
    fun tempDirectory(): String
}

// for use in test cases
class TestLocations(
    private val baseDir: String = ".",
    private val suffix: String = String.random(),
    private val useGlobalCache: Boolean = true
) : Locations {
    init {
        File(cacheDirectory()).mkdirs()
        File(dataDirectory()).mkdirs()
        File(tempDirectory()).mkdirs()
    }

    override fun cacheDirectory(): String {
        return if (useGlobalCache) {
            System.getProperty("user.home") + "/.tasks/cache"
        } else {
            return "${root()}/cache"
        }
    }

    override fun dataDirectory(): String {
        return "${root()}/data"
    }

    override fun tempDirectory(): String {
        return "${root()}/tmp"
    }

    override fun serviceHomeDirectory(product: String, service: String?, instance: String?): String {
        val result = StringBuffer()
        result.append(root()).append("/").append(product)
        if (service != null && service.isNotEmpty()) {
            result.append("/").append(service)
        }
        if (instance != null && instance.isNotEmpty()) {
            result.append("-").append(instance)
        }
        return result.toString()
    }

    fun suffix(): String {
        return suffix
    }

    fun homeDirectory(): String {
        return File(root()).absolutePath
    }

    private fun root(): String {
        return if (baseDir == ".") ".testing/$suffix" else "$baseDir/.testing/$suffix"
    }
}

// for use locally
class LocalLocations(private val root: String = System.getProperty("user.home") + "/.tasks") : Locations {

    init {
        File(cacheDirectory()).mkdirs()
        File(dataDirectory()).mkdirs()
        File(dataDirectory()).mkdirs()
    }

    override fun cacheDirectory(): String {
        return "$root/cache"
    }

    override fun dataDirectory(): String {
        return "$root/data"
    }

    override fun tempDirectory(): String {
        return "$root/tmp"
    }

    override fun serviceHomeDirectory(product: String, service: String?, instance: String?): String {
        val result = StringBuffer()
        result.append(root).append("/").append(product)
        if (service != null && service.isNotEmpty()) {
            result.append("/").append(service)
        }
        if (instance != null && instance.isNotEmpty()) {
            result.append("-").append(instance)
        }
        File(result.toString()).mkdirs()
        return result.toString()
    }

    fun homeDirectory(): String {
        return File(root).absolutePath
    }
}

// on a (unix) server
class UnixServerLocations(private val root: String = "/opt/tasks") : Locations {

    init {
        File(cacheDirectory()).mkdirs()
        File(dataDirectory()).mkdirs()
        File(dataDirectory()).mkdirs()
    }

    override fun cacheDirectory(): String {
        return "$root/cache"
    }

    override fun dataDirectory(): String {
        return "$root/data"
    }

    override fun tempDirectory(): String {
        return "$root/tmp"
    }

    override fun serviceHomeDirectory(product: String, service: String?, instance: String?): String {
        val result = StringBuffer()
        result.append(root).append("/").append(product)
        if (service != null && service.isNotEmpty()) {
            result.append("/").append(service)
        }
        if (instance != null && instance.isNotEmpty()) {
            result.append("-").append(instance)
        }
        File(result.toString()).mkdirs()
        return result.toString()
    }
}
