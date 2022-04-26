package dreifa.app.tasks.demo.echo

import dreifa.app.tasks.*
import dreifa.app.clock.PlatformTimer
import dreifa.app.helpers.random
import dreifa.app.registry.Registry
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.logging.LogLevel
import dreifa.app.tasks.logging.LogMessage
import dreifa.app.types.StringList
import dreifa.app.types.UniqueId
import java.math.BigDecimal
import java.util.*

enum class Colour {
    Red, Green, Blue;

    companion object {
        fun random(): Colour = Colour.values()[Random().nextInt(2)]
    }
}

data class DemoModel(
    val string: String = String.random(80),
    val int: Int = Random().nextInt(),
    val long: Long = Random().nextLong(),
    val double: Double = Random().nextDouble(),
    val float: Float = Random().nextFloat(),
    val boolean: Boolean = Random().nextBoolean(),
    val colour: Colour = Colour.random(),
    val nested: DemoModel? = null
)

/**
 * Tasks that simply echo the result back. Good for basic testing
 * of communication channels and serialisation
 */

class EchoIntTask : BlockingTask<Int, Int> {
    override fun exec(ctx: ExecutionContext, input: Int): Int {
        return input
    }
}

class EchoLongTask : BlockingTask<Long, Long> {
    override fun exec(ctx: ExecutionContext, input: Long): Long {
        return input
    }
}

class EchoDoubleTask : BlockingTask<Double, Double> {
    override fun exec(ctx: ExecutionContext, input: Double): Double {
        return input
    }
}

class EchoFloatTask : BlockingTask<Float, Float> {
    override fun exec(ctx: ExecutionContext, input: Float): Float {
        return input
    }
}

class EchoBooleanTask : BlockingTask<Boolean, Boolean> {
    override fun exec(ctx: ExecutionContext, input: Boolean): Boolean {
        return input
    }
}

class EchoStringTask : BlockingTask<String, String> {
    override fun exec(ctx: ExecutionContext, input: String): String {
        return input
    }
}

class EchoBigDecimalTask : BlockingTask<BigDecimal, BigDecimal> {
    override fun exec(ctx: ExecutionContext, input: BigDecimal): BigDecimal {
        return input
    }
}

class EchoUUIDTask : BlockingTask<UUID, UUID> {
    override fun exec(ctx: ExecutionContext, input: UUID): UUID {
        return input
    }
}

class EchoStringListTask : BlockingTask<StringList, StringList>, TaskDoc<StringList, StringList> {
    override fun exec(ctx: ExecutionContext, input: StringList): StringList {
        return StringList(input.map { it.uppercase() })
    }

    override fun description(): String = "A simple task for demoing lists. The input list is echoed back in upper-case"

    override fun examples(): List<TaskExample<StringList, StringList>> {
        return TaskExamplesBuilder()
            .example("A list of names")
            .input(StringList(listOf("Alice", "Bob")))
            .output(StringList(listOf("ALICE", "BOB")))
            .done()
            .build()
    }
}

class EchoDemoModelTask : BlockingTask<DemoModel, DemoModel>, TaskDoc<DemoModel, DemoModel> {
    override fun exec(ctx: ExecutionContext, input: DemoModel): DemoModel {
        return input
    }

    override fun description(): String =
        "A simple task for demoing custom models. The input list is echoed back in the output"

    override fun examples(): List<TaskExample<DemoModel, DemoModel>> {
        val model = DemoModel()
        val modelWithNesting = DemoModel(nested = model)

        return TaskExamplesBuilder()
            .example("Random DemoModel")
            .input(model)
            .output(model)
            .done()
            .example("Random DemoModel with nesting")
            .input(modelWithNesting)
            .output(modelWithNesting)
            .done()
            .build()
    }
}

class EchoEnumTask : BlockingTask<Colour, Colour> {
    override fun exec(ctx: ExecutionContext, input: Colour): Colour {
        return input
    }
}

class EchoToStdOutTask : BlockingTask<String, Unit> {
    override fun exec(ctx: ExecutionContext, input: String): Unit {
        ctx.stdout().print(input)
    }
}

class EchoToStdErrTask : BlockingTask<String, Unit> {
    override fun exec(ctx: ExecutionContext, input: String): Unit {
        ctx.stderr().print(input)
    }
}

class EchoToLogTask : BlockingTask<String, Unit> {
    override fun exec(ctx: ExecutionContext, input: String) {
        ctx.log(
            LogMessage(
                openTelemetryContext = ctx.telemetryContext(),
                level = LogLevel.INFO,
                body = input
            )
        )
    }
}

abstract class BaseEchoAsyncTask<I, O>(registry: Registry) : AsyncTask<I, O> {
    private val resultChannelFactory = registry.get(AsyncResultChannelSinkFactory::class.java)

    protected fun submitResultWithDelay(
        ctx: ExecutionContext,
        channelLocator: AsyncResultChannelSinkLocator,
        result: AsyncResultChannelMessage<I>
    ) {
        ctx.executorService().submit<Unit> {
            // 1. Get the results channel
            val resultChannel = resultChannelFactory.create(channelLocator)

            // 2. Simulate a delay
            Thread.sleep(PlatformTimer.clockTick())

            // 3. Write the result and also echo to logging channels
            resultChannel.accept(result)
        }
    }
}

class EchoIntAsyncTask(registry: Registry) : BaseEchoAsyncTask<Int, Int>(registry) {
    override fun exec(
        ctx: ExecutionContext,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: Int
    ) {
        val result = AsyncResultChannelMessage(channelId, Success(input), Int::class.java)
        submitResultWithDelay(ctx, channelLocator, result)
    }
}

class EchoStringAsyncTask(registry: Registry) : BaseEchoAsyncTask<String, String>(registry) {
    override fun exec(
        ctx: ExecutionContext,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: String
    ) {
        val result = AsyncResultChannelMessage(channelId, Success(input), String::class.java)
        submitResultWithDelay(ctx, channelLocator, result)
    }
}

class EchoToConsoleTask : BlockingTask<String, Unit> {

    override fun exec(ctx: ExecutionContext, input: String): Unit {
        ctx.stdout().println(input)
    }
}

// list of all echo tasks
class EchoTasks : SimpleTaskRegistrations(
    listOf(
        TaskRegistration(EchoIntTask::class),
        TaskRegistration(EchoIntAsyncTask::class),
        TaskRegistration(EchoLongTask::class),
        TaskRegistration(EchoBigDecimalTask::class),
        TaskRegistration(EchoFloatTask::class),
        TaskRegistration(EchoDoubleTask::class),
        TaskRegistration(EchoStringTask::class),
        TaskRegistration(EchoStringAsyncTask::class),
        TaskRegistration(EchoBooleanTask::class),
        TaskRegistration(EchoEnumTask::class),
        TaskRegistration(EchoDemoModelTask::class),
        TaskRegistration(EchoUUIDTask::class),
        TaskRegistration(EchoStringListTask::class),
        TaskRegistration(EchoToStdOutTask::class),
        TaskRegistration(EchoToStdErrTask::class),
        TaskRegistration(EchoToLogTask::class),
        TaskRegistration(EchoToConsoleTask::class)
    )
)

