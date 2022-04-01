package dreifa.app.tasks.client

import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.assertion.assertThat
import dreifa.app.tasks.TaskFactory
import dreifa.app.registry.Registry
import dreifa.app.tasks.demo.CalcSquareTask
import dreifa.app.tasks.demo.DemoTasks
import dreifa.app.tasks.demo.echo.EchoTasks
import dreifa.app.tasks.logging.DefaultLoggingChannelFactory
import dreifa.app.tasks.logging.LoggingChannelLocator
import dreifa.app.tasks.logging.LoggingReaderContext
import dreifa.app.types.NotRequired
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimpleTaskClientTests  {
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
        //assertNoOutput(clientContext)
    }

    @Test
    fun `should pass on task exception`() {
        val clientContext = SimpleClientContext()
        assertThat({
            SimpleTaskClient(registry).execBlocking(
                clientContext,
                "dreifa.app.tasks.demo.ExceptionGeneratingBlockingTask",
                "MyException",
                String::class
            )
        }, throws<RuntimeException>())

        //assertPartialLogMessage(clientContext, "MyException")
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
    fun `should return TaskDoc if implemented`() {
        val clientContext = SimpleClientContext()
        val result = SimpleTaskClient(registry).taskDocs<Int, Int>(
            clientContext,
            "dreifa.app.tasks.demo.CalcSquareTask"
        )

        val expectedDescription = CalcSquareTask.exampleDescription()
        assertThat(result.description(), equalTo(expectedDescription))
        val expectedExamples = CalcSquareTask.examplesFixture()
        assertThat(result.examples(), equalTo(expectedExamples))
    }

    @Test
    fun `should throw exception if TaskDoc not implemented`() {
        val clientContext = SimpleClientContext()
        assertThat(
            {
                SimpleTaskClient(registry).taskDocs<Any, Any>(
                    clientContext,
                    "dreifa.app.tasks.demo.NoDocsTask"
                )
            }, throws<RuntimeException>(
                has(
                    Exception::message,
                    present(equalTo("No TaskDoc for task: dreifa.app.tasks.demo.NoDocsTask"))
                )
            )
        )
    }

    @Test
    fun `should skip round tripping for NotRemotableTask`() {
        // these are a special case - we don't force serialisation
        // for these tasks as it would likely fail.
        val clientContext = SimpleClientContext()
        SimpleTaskClient(registry).execBlocking(
            clientContext,
            "dreifa.app.tasks.demo.CannotRemoteTask",
            NotRequired.instance(),
            File::class
        )
        // nothing to assert - just want the code to complete with exceptions
    }

}