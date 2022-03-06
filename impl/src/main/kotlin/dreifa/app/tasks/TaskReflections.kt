package dreifa.app.tasks

import kotlin.reflect.KClass
import kotlin.reflect.full.functions

/**
 * Examine a Task via reflections to extract meta data to drive other
 * layers such as JSON mappings
 */
class TaskReflections(val t: KClass<out Task>) {

    fun paramClass(): KClass<out Any> {
        val execMethod = t.functions.single { it.name == "exec" }
        val type = execMethod.parameters[2].type
        @Suppress("UNCHECKED_CAST")
        return type.classifier as KClass<Any>
    }

    fun resultClass(): KClass<out Any> {
        val execMethod = t.functions.single { it.name == "exec" }
        @Suppress("UNCHECKED_CAST")
        return execMethod.returnType.classifier as KClass<Any>
    }

    fun isParamOptional(): Boolean {
        val execMethod = t.functions.single { it.name == "exec" }
        return (execMethod.parameters[2].type.isMarkedNullable)
    }

}