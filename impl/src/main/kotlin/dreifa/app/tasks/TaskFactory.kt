package  dreifa.app.tasks


import dreifa.app.registry.Registry
import java.lang.reflect.InvocationTargetException
import java.util.Iterator
import kotlin.reflect.KClass

class TaskFactory(
    private val registry: Registry = Registry(),
    private val clazzLoader: ClassLoader? = null
) {
    private val lookup = HashMap<String, KClass<out Task>>()

    /**
     * Register using a list of  TaskRegistrations
     */
    fun register(taskRegistrations: TaskRegistrations): TaskFactory {
        taskRegistrations.forEach { register(it) }
        return this
    }

    /**
     * Register using a list of  TaskRegistrations
     */
    fun register(taskRegistrationsClazzName: String): TaskFactory {
        try {
            val clazz = Class.forName(taskRegistrationsClazzName, true, activeClazzLoader());
            val method = clazz.methods.single { it.name == "iterator" }
            val instance: Any = clazz.getDeclaredConstructor().newInstance()
            val iterator = method.invoke(instance) as Iterator<Any>
            while (iterator.hasNext()) {
                val registration = iterator.next()
                if (registration is TaskRegistration) {
                    register(registration)
                }
            }
            return this
        } catch (cnfe: ClassNotFoundException) {
            throw TaskException("taskRegistrations `$taskRegistrationsClazzName` not found." +
                    "\n.Is the name correct?\nIs a JAR classloader needed?")
        }
    }

    /**
     * Register using a TaskRegistration
     */
    fun register(taskRegistration: TaskRegistration): TaskFactory {
        register(taskRegistration.task, taskRegistration.asTask)
        return this
    }

    /**
     * Register the class, taking the class name as the registered name
     * - task = the implementingClass
     * - asTask = the interface (if different)
     */
    fun register(
        task: KClass<out Task>,
        asTask: KClass<out Task> = task
    ): TaskFactory {
        val name = asTask.qualifiedName!!
        if (lookup.containsKey(name)) throw TaskException("`$name` is already registered")
        lookup[name] = task
        return this
    }

    fun list(): List<String> {
        return lookup.keys.sorted()
    }

    /**
     * Create an instance of a Task by fully qualified name. This
     * is the "core" factory method, but in most cases one of the
     * more type safe variants will result in cleaner code.
     *
     */
    fun createInstance(qualifiedName: String): Task {
        if (!lookup.containsKey(qualifiedName)) {
            throw TaskException("Task: `$qualifiedName` is not registered")
        }
        val clazz = lookup[qualifiedName]!!

        // try with Registry
        clazz.constructors.forEach {
            if (it.parameters.size == 1) {
                @Suppress("UNCHECKED_CAST")
                val paramClazz = it.parameters[0].type.classifier as KClass<Any>
                if (paramClazz == Registry::class) {
                    try {
                        return it.call(registry)
                    } catch (itex: InvocationTargetException) {
                        throw TaskException("Problem instantiating `$qualifiedName`. Original error: `${itex.targetException.message}`")
                    } catch (ex: Exception) {
                        throw TaskException("Problem instantiating `$qualifiedName`. Original error: `${ex.message}`")
                    }
                }
            }
        }

        // try wih no params constructor
        clazz.constructors.forEach {
            if (it.parameters.isEmpty()) {
                try {
                    return it.call()
                } catch (ex: Exception) {
                    throw TaskException("Problem instantiating $qualifiedName. Original error ${ex.message}")
                }
            }
        }
        throw TaskException("Couldn't find a suitable constructor for task: `$qualifiedName`")
    }

    fun <I, O> createInstance(taskClazz: KClass<out BlockingTask<I, O>>): BlockingTask<I, O> {
        val taskName: String = taskClazz.qualifiedName!!
        val task = createInstance(taskName)
        if (task is BlockingTask<*, *>) {
            @Suppress("UNCHECKED_CAST")
            return task as BlockingTask<I, O>
        } else {
            throw RuntimeException("${task::class.qualifiedName} is not a BlockingTask")
        }
    }

    fun <I, O> createInstance(taskClazz: KClass<out AsyncTask<I, O>>): AsyncTask<I, O> {
        val taskName: String = taskClazz.qualifiedName!!
        val task = createInstance(taskName)
        if (task is AsyncTask<*, *>) {
            @Suppress("UNCHECKED_CAST")
            return task as AsyncTask<I, O>
        } else {
            throw RuntimeException("${task::class.qualifiedName} is not an AsyncTask")
        }
    }

    fun activeClazzLoader(): ClassLoader {
        return clazzLoader ?: this::class.java.classLoader
    }

}