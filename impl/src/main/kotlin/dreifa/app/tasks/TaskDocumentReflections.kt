package dreifa.app.tasks

class TaskDocumentReflections(private val t: Task) {

    fun description(): String {
        if (t is TaskDocument<*, *>) {
            return t.description()
        }
        return ""
    }

}