package dreifa.app.tasks.test

import dreifa.app.tasks.*
import dreifa.app.registry.Registry
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.logging.LogMessage
import dreifa.app.types.NotRequired
import dreifa.app.types.StringList
import java.io.File
import java.net.URL

interface ListDirectoryTask : BlockingTask<String, StringList>

class ListDirectoryTaskImpl : ListDirectoryTask, BlockingTask<String, StringList> {
    override fun exec(ctx: ExecutionContext, input: String): StringList {
        val data = File(input).listFiles().map { it.name }
        return StringList(data)
    }
}

class ListDirectoryTaskFake : ListDirectoryTask, BlockingTask<String, StringList> {
    override fun exec(ctx: ExecutionContext, input: String): StringList {
        val out = ctx.stdout()
        out.println("ListDirectoryTask:")
        out.println("   params: $input")
        ctx.log(LogMessage.info("listing directory '$input'"))
        return StringList(listOf("fake.txt"))
    }
}


class RegistryTask(private val registry: Registry) : BlockingTask<Unit?, String> {
    override fun exec(ctx: ExecutionContext, input: Unit?): String {
        return registry.get(String::class.java)
    }
}


data class Params(val p1: String, val p2: Int)
class ParamsTask() : BlockingTask<Params, Unit>, UnitBlockingTask<Params> {
    override fun exec(ctx: ExecutionContext, input: Params) {
        ctx.log(LogMessage.info("called with params $input"))
    }
}

enum class Colour { Red, Green, Blue }
class EnumTask() : BlockingTask<Colour, Unit>, UnitBlockingTask<Colour> {
    override fun exec(ctx: ExecutionContext, input: Colour) {
        ctx.log(LogMessage.info("called with params $input"))
    }
}

data class ParamsWithDefault(val p1: String, val p2: Int = 99, val p3: String = "foo")
class ParamsWithDefaultTask() : BlockingTask<ParamsWithDefault, ParamsWithDefault> {
    override fun exec(ctx: ExecutionContext, input: ParamsWithDefault): ParamsWithDefault {
        ctx.log(LogMessage.info("called with params $input"))
        return input
    }
}

class MapTask() : BlockingTask<Map<String, Any>, Unit>, UnitBlockingTask<Map<String, Any>> {
    override fun exec(ctx: ExecutionContext, input: Map<String, Any>) {
        ctx.log(LogMessage.info("called with params $input"))
    }
}

class NoParamTask() : BlockingTask<Nothing?, Unit>, UnitBlockingTask<Nothing?> {
    override fun exec(ctx: ExecutionContext, input: Nothing?) {
        ctx.log(LogMessage.info("called with params $input"))
    }
}

class NotRequiredParamTask() : BlockingTask<NotRequired, Unit>, UnitBlockingTask<NotRequired> {
    override fun exec(ctx: ExecutionContext, input: NotRequired) {
        ctx.log(LogMessage.info("called with params $input"))
    }
}

class FileTask : BlockingTask<File, Int> {
    override fun exec(ctx: ExecutionContext, input: File): Int {
        ctx.log(LogMessage.info("Loading file $input"))
        return input.readBytes().size
    }
}


class URLTask : BlockingTask<URL, String> {
    override fun exec(ctx: ExecutionContext, input: URL): String {
        ctx.log(LogMessage.info("Loading url $input"))
        return input.toExternalForm()
    }
}


data class ParamsWithFile(val file: File, val files: List<File>)
class ParamsWithFileTask() : BlockingTask<ParamsWithFile, ParamsWithFile> {
    override fun exec(ctx: ExecutionContext, input: ParamsWithFile): ParamsWithFile {
        ctx.log(LogMessage.info("called with params $input"))
        return input
    }
}


// testing of sealed classes
sealed class DatabaseConfig

data class PostgresConfig(val postgres: String) : DatabaseConfig()
data class OracleConfig(val oracle: String) : DatabaseConfig()

class DatabaseTask() : BlockingTask<DatabaseConfig, DatabaseConfig> {
    override fun exec(ctx: ExecutionContext, input: DatabaseConfig): DatabaseConfig {
        ctx.log(LogMessage.info("called with params $input"))
        return input
    }
}

// emulates a task that reads some status information. after a period
// of time that status will change, e.g. a system might go from "starting" to "running" status
class StatusChangeTask<I, O>(private val before: O, private val after: O, private val delay: Long = 1000) :
    BlockingTask<I, O> {

    private val startTime = System.currentTimeMillis()
    override fun exec(ctx: ExecutionContext, input: I): O {
        return if (System.currentTimeMillis() < (startTime + delay)) before else after
    }

}

class TaskRegistrationsExample : SimpleTaskRegistrations(
    listOf(
        TaskRegistration(MultiplyTask::class),
        TaskRegistration(HelloWorldTask::class, SimpleTask::class)
    )
)


@Suppress("UNUSED_PARAMETER")
class TaskRegistrationsWithBadConstructor(notUsed : String) : SimpleTaskRegistrations(
    listOf(
        TaskRegistration(MultiplyTask::class),
        TaskRegistration(HelloWorldTask::class, SimpleTask::class)
    )
)
