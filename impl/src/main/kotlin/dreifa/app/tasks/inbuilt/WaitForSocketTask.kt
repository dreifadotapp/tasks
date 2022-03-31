package  dreifa.app.tasks.inbuilt

import dreifa.app.tasks.AsyncTask
import dreifa.app.tasks.AsyncResultChannelSinkLocator
import dreifa.app.tasks.SocketAddress
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.types.UniqueId
import java.util.*

/**
 * Checks for an open socket on a given address
 *
 */

interface WaitForSocketTask : AsyncTask<SocketAddress, Long> {}

class WaitForSocketTaskImpl : WaitForSocketTask {
    private val taskId = UUID.randomUUID()
    override fun exec(
        ctx: ExecutionContext,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: SocketAddress
    ) {
        doit(ctx, input)
    }

    override fun taskId(): UUID {
        return taskId
    }

    fun doit(ctx: ExecutionContext, params: SocketAddress): Long {
        val start = System.currentTimeMillis()
        return ctx.executorService().submit<Long> {
            while (!isSocketAlive(params)) {
                Thread.sleep(1000)
            }
            System.currentTimeMillis() - start
        }.get()
    }
}

fun isSocketAlive(params: SocketAddress) = false

class WaitForSocketTaskFake : WaitForSocketTask {
    private val taskId = UUID.randomUUID()
    override fun exec(
        ctx: ExecutionContext,
        channelLocator: AsyncResultChannelSinkLocator,
        channelId: UniqueId,
        input: SocketAddress
    ) {
        TODO("Not yet implemented")
    }

    override fun taskId(): UUID {
        return taskId
    }
}


// basic test harness
fun main() {

    val ctx = SimpleExecutionContext()
    //val address = SocketAddress("localhost", 12345)

    // Can use netcat to create a test server:
    // $ nc -l -p 12345
    // or on a Mac:
    // $ nc -p 12345

    try {
        println("Running WaitAgentReadyCmdImpl")
        //val result = WaitForSocketTaskImpl().exec(ctx, address)
        //println("Success!, that took $result milliseconds")
    } catch (ex: RuntimeException) {
        ex.printStackTrace()
    }

    ctx.executorService().shutdownNow()
}