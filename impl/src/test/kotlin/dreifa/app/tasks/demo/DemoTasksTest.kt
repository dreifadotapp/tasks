package dreifa.app.tasks.demo

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dreifa.app.tasks.TaskDocInput
import org.junit.jupiter.api.Test

class DemoTasksTest {
    @Test
    fun `should build TaskDocs`() {
        val task = ExceptionGeneratingBlockingTask()

        assertThat(task.examples().size, equalTo(2))
        val example1 = task.examples()[0]
        val example2 = task.examples()[1]

        assertThat(example1.description(), equalTo("Creates an exception"))
        assertThat(example1.input(), equalTo(TaskDocInput("DemoException", "must contain `exception`")))
        assert(example1.output() == null)

        assertThat(example2.description(), equalTo("Echoes the input"))
        assertThat(example2.input(), equalTo(TaskDocInput("Hello World")))
        assertThat(example2.output(), equalTo("Hello World"))

    }
}