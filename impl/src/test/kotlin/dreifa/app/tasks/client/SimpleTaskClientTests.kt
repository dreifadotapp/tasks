package dreifa.app.tasks.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import com.natpryce.hamkrest.isEmptyString
import com.natpryce.hamkrest.throws
import dreifa.app.tasks.TaskFactory
import dreifa.app.registry.Registry
import dreifa.app.tasks.demo.DemoTasks
import dreifa.app.tasks.demo.echo.EchoTasks
import dreifa.app.tasks.logging.DefaultLoggingChannelFactory
import dreifa.app.tasks.logging.LoggingChannelLocator
import dreifa.app.tasks.logging.LoggingReaderContext
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimpleTaskClientTests : BaseTaskClientTest() {

    private val registry = Registry()
    private val taskFactory = TaskFactory().register(DemoTasks()).register(EchoTasks())
    private val logChannelFactory = DefaultLoggingChannelFactory(registry)


    init {
        registry.store(taskFactory).store(logChannelFactory)
    }

    @Test
    fun `should call task and return output`() {
        val clientContext = SimpleClientContext()
        val result = SimpleTaskClient(registry).execBlocking(
            clientContext,
            "dreifa.app.tasks.demo.echo.EchoStringTask",
            "Hello, world",
            String::class
        )

        assertThat(result, equalTo("Hello, world"))
        assertNoOutput(clientContext)
    }

    @Test
    fun `should pass on task exception`() {
        val clientContext = SimpleClientContext()
        assertThat({
            SimpleTaskClient(registry).execBlocking(
                clientContext,
                "dreifa.app.tasks.demo.ExceptionGeneratingBlockingTask",
                "opps",
                String::class
            )
        }, throws<RuntimeException>())

        assertPartialLogMessage(clientContext, "opps")
    }

    @Test
    fun `should return stdout to client`() {
        val locator = LoggingChannelLocator.inMemory()
        val clientContext = SimpleClientContext(loggingChannelLocator = locator)
        SimpleTaskClient(registry).execBlocking(
            clientContext,
            "dreifa.app.tasks.demo.echo.EchoToStdOutTask",
            "Hello, world\n",
            Unit::class
        )

        val readerContext: LoggingReaderContext = logChannelFactory.query(locator)
        assertThat(readerContext.stdout(), equalTo("Hello, world\n"))
        assertThat(readerContext.stderr(), isEmptyString)
        assertThat(readerContext.messages(), isEmpty)
    }

    @Test
    fun `should return stderr to client`() {
        val locator = LoggingChannelLocator.inMemory()
        val clientContext = SimpleClientContext(locator)
        SimpleTaskClient(registry).execBlocking(
            clientContext,
            "dreifa.app.tasks.demo.echo.EchoToStdErrTask",
            "Goodbye, cruel world\n",
            Unit::class
        )

        val readerContext: LoggingReaderContext = logChannelFactory.query(locator)
        assertThat(readerContext.stdout(), isEmptyString)
        assertThat(readerContext.stderr(), equalTo("Goodbye, cruel world\n"))
        assertThat(readerContext.messages(), isEmpty)
    }

    @Test
    fun `should pass on exception to client`() {
        val clientContext = SimpleClientContext()

        try {
            SimpleTaskClient(registry).execBlocking(
                clientContext,
                "dreifa.app.tasks.demo.ExceptionGeneratingBlockingTask",
                "Opps",
                String::class
            )
            fail("should have thrown a RuntimeException")
        } catch (re: RuntimeException) {
            re.printStackTrace()
            assertThat(re.message, equalTo("Opps"))
        }
    }

}