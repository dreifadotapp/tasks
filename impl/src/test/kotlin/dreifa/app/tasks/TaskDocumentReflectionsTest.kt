package dreifa.app.tasks

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dreifa.app.tasks.TaskDocumentReflections
import dreifa.app.tasks.demo.CalcSquareTask

class TaskDocumentReflectionsTest {
    fun `Should read description if available`() {
        val t = CalcSquareTask()
        val reflections = TaskDocumentReflections(t)
        assertThat(reflections.description(), equalTo("An example Task that calculates the square of a number"))
    }
}