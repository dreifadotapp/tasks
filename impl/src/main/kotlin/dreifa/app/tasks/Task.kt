package dreifa.app.tasks

import dreifa.app.tasks.executionContext.SimpleExecutionContext
import dreifa.app.tasks.executionContext.DefaultExecutionContextModifier
import dreifa.app.tasks.executionContext.ExecutionContext
import java.util.UUID

/**
 * A common marker interface for a Task
 */
interface Task {
    /**
     * A unique ID created for each run of the Task
     */
    fun taskId(): UUID
}

// An marker interface to indicate that the Task is Idempotent, i.e. it is
// safe to call it multiple times.
//
// Ideally any Task is Idempotent
interface IdempotentTask {}


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

abstract class BaseBlockingTask<I, O> : BlockingTask<I, O> {
    private val taskID = UUID.randomUUID()
    override fun taskId(): UUID {
        return taskID
    }

    /**
     * Update the ExecutionContext with the TaskId.
     */
    protected fun ctxWithTaskID(ctx: ExecutionContext): ExecutionContext =
        DefaultExecutionContextModifier(ctx).withTaskId(taskId())
}

abstract class BaseUnitBlockingTask<I> : UnitBlockingTask<I> {
    private val taskID = UUID.randomUUID()
    override fun taskId(): UUID {
        return taskID
    }

    /**
     * Update the ExecutionContext with the TaskId.
     */
    protected fun ctxWithTaskID(ctx: ExecutionContext): ExecutionContext =
        DefaultExecutionContextModifier(ctx).withTaskId(taskId())
}

abstract class BaseAsyncTask<I, O> : AsyncTask<I, O> {
    private val taskID = UUID.randomUUID()
    override fun taskId(): UUID {
        return taskID
    }

    /**
     * Update the ExecutionContext with the TaskId.
     */
    protected fun updatedCtx(ctx: ExecutionContext): ExecutionContext =
        DefaultExecutionContextModifier(ctx).withTaskId(taskId())
}





