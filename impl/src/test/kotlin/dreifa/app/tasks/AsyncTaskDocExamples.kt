package dreifa.app.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dreifa.app.tasks.*
import dreifa.app.clock.PlatformTimer
import dreifa.app.registry.Registry
import dreifa.app.tasks.client.SimpleClientContext
import dreifa.app.tasks.client.SimpleTaskClient
import dreifa.app.tasks.demo.CalcSquareAsyncTask
import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.tasks.logging.DefaultLoggingChannelFactory
import dreifa.app.tasks.logging.LoggingChannelLocator
import dreifa.app.tasks.logging.LogLevel
import dreifa.app.types.UniqueId
import org.junit.jupiter.api.Test

/**
Code to match examples in 'tasks.md' file
 */
class AsyncTaskDocExamples {

    @Test
    fun `should call task directly`() {
        // 1. Setup a result sink,
        val resultSinkFactory = DefaultAsyncResultChannelSinkFactory()
        val reg = Registry().store(resultSinkFactory)

        // 2. setup a channel for the results
        val locator = AsyncResultChannelSinkLocator.LOCAL
        val channelId = UniqueId.alphanumeric()

        // 3. setup the task
        val task = CalcSquareAsyncTask(reg)
        val ctx = SimpleExecutionContext()

        // 4. call the task. The result will come back on the results channel
        task.exec(ctx, locator, channelId, 10)

        // 5. run a query over the result channel. In this
        //    CalcSquareAsyncTask is a demo and just has a simple sleep
        val query = resultSinkFactory.channelQuery(locator)

        // 6. assert expected results
        // not yet read
        assertThat(query.hasResult(channelId), equalTo(false))
        // wait long enough
        Thread.sleep(PlatformTimer.testDelayInMs(10))
        // now ready
        assert(query.hasResult(channelId))
        val result = query.result<Int>(channelId)
        assertThat(result, equalTo(Success<Int>(100) as AsyncResult<Int>))
    }

    @Test
    fun `should call task via a task client`() {
        // 1. Create a registry and store a AsyncResultChannelSinkFactory
        val registry = Registry()
        val resultSinkFactory = DefaultAsyncResultChannelSinkFactory()
        registry.store(resultSinkFactory)

        // 2a. register a real task in the TaskFactory (server side)
        val taskFactory = TaskFactory(registry)
        taskFactory.register(CalcSquareAsyncTask::class)
        registry.store(taskFactory)

        // 2b. register LogChannelFactory (server side)
        val logChannelFactory = DefaultLoggingChannelFactory(registry)
        registry.store(logChannelFactory)

        // 3. get a task client (client side)
        val taskClient = SimpleTaskClient(registry)

        // 4. setup a channel for the results
        val locator = AsyncResultChannelSinkLocator.LOCAL
        val channelId = UniqueId.alphanumeric()

        // 5. call the client
        val logChannelLocator = LoggingChannelLocator.inMemory()
        val clientContext = SimpleClientContext(loggingChannelLocator = logChannelLocator)
        taskClient.execAsync(
            clientContext,
            "dreifa.app.tasks.demo.CalcSquareAsyncTask",
            locator,
            channelId,
            10,
            Int::class
        )

        // 6. the first log message is already available, but the second isn't
        val logQuery = logChannelFactory.query(logChannelLocator)
        assert(
            logQuery.messages().hasMessage(LogLevel.INFO, "Starting calculation")
        )
        assert(
            logQuery.messages().doesNotHaveMessage(LogLevel.INFO, "Completed calculation")
        )

        // 7. run a query over the result channel. In this case
        //    CalcSquareAsyncTask is a demo and just has a simple sleep
        val query = resultSinkFactory.channelQuery(locator)

        // 6. assert expected results
        // not yet read
        assertThat(query.hasResult(channelId), equalTo(false))
        // wait long enough
        Thread.sleep(PlatformTimer.clockTick() * 20)
        // now ready
        assert(query.hasResult(channelId))
        val result = query.result<Int>(channelId)
        assertThat(result, equalTo(Success<Int>(100) as AsyncResult<Int>))

        // 4. assert results
//        assert(result.contains("fake.txt"))
//
//        // 5. assert logging output
//        assertThat(
//            clientContext.inMemoryLoggingConsumerContext().stdout(),
//            equalTo(
//                "ListDirectoryTask:\n" +
//                        "   params: .\n"
//            )
//        )
//        assert(
//            clientContext.inMemoryLoggingConsumerContext().messages()
//                .hasMessage(LogLevel.INFO, "listing directory '.'")
//        )
    }
}