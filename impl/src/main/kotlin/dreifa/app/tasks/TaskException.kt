package dreifa.app.tasks

class TaskException(override val message: String, override val cause: Throwable? = null) :
    RuntimeException(message, cause)
