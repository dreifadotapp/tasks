package dreifa.app.tasks

/**
 * Add documentation information to a Task.
 */
interface TaskDocument<I, O> {
    fun description(): String
    fun examples(): List<TaskExample<I, O>>

}

interface TaskExampleData<T> {
    fun data(): T
    fun dataDescription(): String
    fun provisioningState(): Map<String, Map<String, Any>>
    fun provisioningStateDescription(): String
}

interface TaskExample<I, O> {
    fun description(): String
    fun input(): TaskExampleData<I>
    fun output(): TaskExampleData<O>
}

class DefaultTaskDocument<I, O>(private val description: String,
                                private val examples: List<TaskExample<I, O>>) : TaskDocument<I, O> {

    override fun description(): String {
        return description
    }

    override fun examples(): List<TaskExample<I, O>> {
        return examples
    }
}

class DefaultTaskExampleData<T>(private val data: T,
                                private val dataDescription: String = "",
                                private val provisioningState: Map<String, Map<String, Any>> = emptyMap(),
                                private val provisioningStateDescription: String = "") : TaskExampleData<T> {

    override fun data(): T {
        return data
    }

    override fun dataDescription(): String {
        return dataDescription
    }

    override fun provisioningState(): Map<String, Map<String, Any>> {
        return provisioningState
    }

    override fun provisioningStateDescription(): String {
        return provisioningStateDescription
    }
}

class UnitTaskExampleData(private val provisioningState: Map<String, Map<String, Any>> = emptyMap(),
                          private val provisioningStateDescription: String = "") : TaskExampleData<Unit> {

    override fun data() {
    }

    override fun dataDescription(): String {
        return ""
    }

    override fun provisioningState(): Map<String, Map<String, Any>> {
        return provisioningState
    }

    override fun provisioningStateDescription(): String {
        return provisioningStateDescription
    }
}


class DefaultTaskExample<I, O>(private val description: String,
                               private val input: TaskExampleData<I>,
                               private val outout: TaskExampleData<O>
) : TaskExample<I, O> {
    override fun input(): TaskExampleData<I> {
        return input
    }

    override fun output(): TaskExampleData<O> {
        return outout
    }

    override fun description(): String {
        return description
    }

}


class UnitTaskExample<I>(private val description: String,
                         private val input: TaskExampleData<I>,
                         private val outout: UnitTaskExampleData
) : TaskExample<I, Unit> {
    override fun input(): TaskExampleData<I> {
        return input
    }

    override fun output(): TaskExampleData<Unit> {
        return outout
    }

    override fun description(): String {
        return description
    }

}