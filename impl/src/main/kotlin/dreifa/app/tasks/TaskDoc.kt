package dreifa.app.tasks

/**
 * Add documentation information to a Task.
 */
interface TaskDoc<I, O> {
    /**
     * A short description of the task
     */
    fun description(): String

    /**
     * A list of examples
     */
    fun examples(): List<TaskExample<I, O>>
}

data class TaskDocInput<T>(val example: T, val description: String? = null)


class TaskExample<I, O>(
    private val description: String,
    private val input: TaskDocInput<I>? = null,
    private val output: O? = null
) {
    fun description(): String {
        return description
    }

    fun input(): TaskDocInput<I>? {
        return input
    }

    fun output(): O? {
        return output
    }
}

class TaskExamplesBuilder() {
    private var examples = ArrayList<TaskExampleBuilder>()

    class TaskExampleBuilder(private val rootBuilder: TaskExamplesBuilder, private val description: String) {
        private var inputDescription: String? = null
        private var input: Any? = null
        private var output: Any? = null

        fun input(input: Any): TaskExampleBuilder {
            this.input = input
            return this
        }

        fun inputDescription(inputDescription: String): TaskExampleBuilder {
            this.inputDescription = inputDescription
            return this
        }

        fun output(output: Any): TaskExampleBuilder {
            this.output = output
            return this
        }

        fun done(): TaskExamplesBuilder {
            return rootBuilder
        }

        fun _build(): TaskExample<Any, Any> = TaskExample(
            description,
            TaskDocInput(input!!, inputDescription),
            output
        )
    }


    fun example(description: String): TaskExampleBuilder {
        val exampleBuilder = TaskExampleBuilder(this, description)
        this.examples.add(exampleBuilder)
        return exampleBuilder
    }

    fun <I, O> build(): List<TaskExample<I, O>> {
        return this.examples.map { it._build() } as List<TaskExample<I, O>>
    }

}