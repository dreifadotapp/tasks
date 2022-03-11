package dreifa.app.tasks

/**
 * Add documentation information to a Task.
 */
interface TaskDoc<I, O> {
    fun description(): String
    fun examples(): List<TaskExample<I, O>>
}

data class TaskDocInput<T> (val example: T, val description: String? = null)


interface TaskExample<I, O> {
    fun description(): String
    fun input(): TaskDocInput<I>
    fun output(): O?
}

class DefaultTaskDoc<I, O>(
    private val description: String,
    private val examples: List<TaskExample<I, O>>
) : TaskDoc<I, O> {

    override fun description(): String {
        return description
    }

    override fun examples(): List<TaskExample<I, O>> {
        return examples
    }
}



class DefaultTaskExample<I, O>(
    private val description: String,
    private val input: TaskDocInput<I>,
    private val output: O? = null
) : TaskExample<I, O> {
    override fun input(): TaskDocInput<I> {
        return input
    }

    override fun description(): String {
        return description
    }

    override fun output(): O? {
        return output
    }
}
