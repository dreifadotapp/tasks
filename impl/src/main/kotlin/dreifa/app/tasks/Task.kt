package dreifa.app.tasks

import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.tasks.executionContext.ExecutionContext

/**
 * A common marker interface for a Task
 */
interface Task {
    /**
     * Every Task should have a name for logging and display in UIs.
     * The convention is that the class names should be clear and generally unambiguous,
     * i.e. no need to qualify by package name, and therefore
     * there is no generally no need to reimplement this method.
     */
    fun taskName(): String {
        return this::class.simpleName!!
    }

    fun taskQualifiedName(): String {
        // rather ugly :(
        return this::class.qualifiedName!!.removeSuffix(this::class.simpleName!!) + taskName()
    }
}

/**
 * A marker interface to indicate that the Task is Idempotent, i.e. it is
 * safe to call it multiple times.
 *
 * Ideally any Task is Idempotent
 **/
interface IdempotentTask


/**
 * A marker to indicate that this Task cannot be remoted, i.e. it can
 * only be run locally, not via a TaskClient.
 *
 * This should be used sparingly.
 */
interface NotRemotableTask

/**
 * A blocking task, i.e. one we can assume will either complete within a reasonable time or just
 * fail with an exception
 */
interface BlockingTask<in I, out O> : Task {
    /**
     * Execute the task.
     */
    fun exec(ctx: ExecutionContext = SimpleExecutionContext(), input: I): O
}

interface UnitBlockingTask<I> : BlockingTask<I, Unit> {
    override fun exec(ctx: ExecutionContext, input: I)
}


