package dreifa.app.tasks

import kotlin.reflect.KClass

data class TaskRegistration(
    val task: KClass<out Task>,
    val asTask: KClass<out Task> = task
)

interface TaskRegistrations : Iterable<TaskRegistration> {
    companion object {
        fun fromClazzName(clazzName: String): TaskRegistrations {
            val clazz = safeClassForName(clazzName)

            // try wih no params constructor
            clazz.constructors.forEach {
                if (it.parameters.isEmpty()) {
                    try {
                        val taskRegistrations = it.call()
                        if (taskRegistrations is TaskRegistrations) return taskRegistrations
                        throw RuntimeException("Must implement TaskRegistrations")
                    } catch (ex: Exception) {
                        throw TaskException("Problem instantiating `$clazzName`. Original error ${ex.message}")
                    }
                }
            }
            throw TaskException("Problem instantiating `$clazzName`. Couldn't find a no args constructor")


        }

        private fun safeClassForName(clazzName: String): KClass<out Any> {
            try {
                return Class.forName(clazzName).kotlin

            } catch (cnfe: ClassNotFoundException) {
                throw TaskException("Problem instantiating `$clazzName`. ClassNotFoundException")

            } catch (ex: Exception) {
                throw TaskException("Problem instantiating `$clazzName`. Original error ${ex.message}")
            }
        }
    }
}

open class SimpleTaskRegistrations(private val registrations: List<TaskRegistration>) : TaskRegistrations {
    override fun iterator(): Iterator<TaskRegistration> {
        return registrations.iterator()
    }
}
