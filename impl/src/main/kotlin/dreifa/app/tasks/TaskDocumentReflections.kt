package dreifa.app.tasks

class TaskDocumentReflections(private val t: Task) {

    fun description(): String {
        if (t is TaskDoc<*, *>) {
            return t.description()
        }
        return ""
    }

}