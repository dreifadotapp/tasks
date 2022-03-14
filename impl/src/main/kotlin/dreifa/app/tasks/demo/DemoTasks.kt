package dreifa.app.tasks.demo

import dreifa.app.tasks.*
import dreifa.app.clock.PlatformTimer
import dreifa.app.registry.Registry
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.logging.LogMessage
import dreifa.app.types.UniqueId
import java.io.File
import java.lang.RuntimeException
import java.util.*

/*
 Some prebuilt demo tasks for tests and example
 */

class CalcSquareTask : BaseBlockingTask<Int, Int>(), TaskDoc<Int, Int> {

    override fun exec(ctx: ExecutionContext, input: Int): Int {
        // this is normally the first line - it ensures the task is stored in the context
        val ctxWithTask = ctx.withTaskId(this)
        ctxWithTask.log(LogMessage.info("Calculating square of $input"))
        return input.times(input)
    }

    override fun description(): String = exampleDescription()

    override fun examples(): List<TaskExample<Int, Int>> = examplesFixture()

    companion object {
        /**
         * Pull examples into a fixture so that they can be easily used in Unit Test
         * assertions
         */
        fun examplesFixture(): List<TaskExample<Int, Int>> {
            // make sure all patters are included
            return listOf(
                TaskExample(
                    "2 squared should return 4",
                    TaskDocInput(2, "The number to square"),
                    4
                ),
                TaskExample(
                    "An example with no input description",
                    TaskDocInput(2),
                    4
                ),
                TaskExample(
                    "An example with no output given. 2 should return ???",
                    TaskDocInput(2, "The number to square")
                ),
                TaskExample(
                    "An example with no input given. 4 is the result of ???",
                    null,
                    4
                )
            )
        }

        fun exampleDescription() = "An example task that calculates the square of a whole number"
    }
}


class ExceptionGeneratingBlockingTask : BaseBlockingTask<String, String>(), TaskDoc<String, String> {
    override fun exec(ctx: ExecutionContext, input: String): String {
        if (input.contains("exception", true)) {
            throw RuntimeException(input)
        } else {
            return input
        }
    }

    override fun description(): String =
        "Use this Task to generate exceptions. If the inout contains `exception` (case insensitive) then a " +
                "RuntimeException is generated with the input as the message. Any other input is echoed in the output"

    override fun examples(): List<TaskExample<String, String>> {
        return TaskExamplesBuilder()
            .example("Creates an exception")
            .input("DemoException")
            .inputDescription("must contain `exception`")
            .done()
            .example("Echoes the input")
            .input("Hello World")
            .output("Hello World")
            .done()
            .build()
    }
}


class ExceptionGeneratingAsyncTask(registry: Registry) : BaseAsyncTask<String, String>() {

    private val executors = registry.get(ExecutorFactory::class.java).executorService()

//    fun exec(ctx: ExecutionContext, input: String): Future<String> {
//        val ctx = DefaultExecutionContextModifier(ctx).withTaskId(taskId())
//        ctx.log(LogMessage.info("Message is '$input'"))
//        return executors.submit<String> {
//            if (input.contains("exception", ignoreCase = true)) throw RuntimeException(input)
//            AsyncTask.sleepForTicks(1)
//            input
//        }
//    }

    override fun exec(
        ctx: ExecutionContext,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: String
    ) {
        TODO("Not yet implemented")
    }
}

class FileTask : BaseBlockingTask<File, Int>() {
    override fun exec(ctx: ExecutionContext, input: File): Int {
        val ctx2 = ctx.withTaskId(taskId())
        ctx2.log(LogMessage.info("Loading file $input"))
        return input.readBytes().size
    }
}

class UnitTask : BaseUnitBlockingTask<String>() {
    override fun exec(ctx: ExecutionContext, input: String) {
        val ctx2 = ctx.withTaskId(taskId())
        ctx2.log(LogMessage.info("Params are: $input"))
    }
}

class PrintStreamTask : BaseUnitBlockingTask<String>() {
    override fun exec(ctx: ExecutionContext, input: String) {
        ctx.stdout().print(input)
    }
}

class CalcSquareAsyncTask(registry: Registry) : AsyncTask<Int, Int> {
    private val resultChannelFactory = registry.get(AsyncResultChannelSinkFactory::class.java)
    private val taskId = UUID.randomUUID()
    override fun exec(
        ctx: ExecutionContext,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: Int
    ) {
        ctx.log(LogMessage.info("Starting calculation"))

        // 1. Find the channel
        val resultChannel = resultChannelFactory.create(channelLocator)

        ctx.executorService().submit<Unit> {
            // 2. Generate a result
            val result = AsyncResultChannelMessage(channelId, Success(input * input), Int::class.java)

            // 3. Simulate a delay
            Thread.sleep(PlatformTimer.clockTick())

            // 4. Write the result and also echo to logging channels
            ctx.log(LogMessage.info("Completed calculation"))
            ctx.stdout().print(result)
            resultChannel.accept(result)
        }
    }

    override fun taskId(): UUID = taskId
}


class NoDocsTask : BaseBlockingTask<String, String>() {
    override fun exec(ctx: ExecutionContext, input: String) = input
}

// list of all demo tasks
class DemoTasks : SimpleTaskRegistrations(
    listOf(
        TaskRegistration(CalcSquareTask::class),
        TaskRegistration(CalcSquareAsyncTask::class),
        TaskRegistration(ExceptionGeneratingBlockingTask::class),
        TaskRegistration(FileTask::class),
        TaskRegistration(UnitTask::class),
        TaskRegistration(PrintStreamTask::class),
        TaskRegistration(NoDocsTask::class),
    )
)


