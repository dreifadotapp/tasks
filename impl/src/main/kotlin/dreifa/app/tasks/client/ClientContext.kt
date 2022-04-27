package dreifa.app.tasks.client

import dreifa.app.opentelemetry.OpenTelemetryContext
import dreifa.app.tasks.logging.LoggingChannelLocator
import dreifa.app.types.UniqueId

/**
 * Marker interface for any type of security (authentication & authorisation) protocol
 */
interface SecurityPrinciple

/**
 * Pass a JWT token that can be checked
 */
//class JwtSecurityPrinciple(val jwtToken: String) : SecurityPrinciple

/**
 * Authenticated with just a username and set of roles. We trust an external system
 */
//class UserAndRoles(val userName: String, val roles: Set<String>) : SecurityPrinciple

/**
 * For testing, or environments where security is unimportant
 */
class NotAuthenticatedSecurityPrinciple(val userName: String = "unknown") : SecurityPrinciple

/**
 * The information that any client must provide
 */
interface ClientContext {

    /**
     * One of the security principles
     */
    fun securityPrinciples(): Set<SecurityPrinciple>

    /**
     * How to be sent back log messages
     */
    fun logChannelLocator(): LoggingChannelLocator

    /**
     * Propagate OpenTelemetry
     */
    fun telemetryContext(): OpenTelemetryContext

    /**
     * Optionally pass some correlation information in from the client
     * This is included in the telemetry for logging and so.
     */
    fun correlation(): CorrelationContexts = CorrelationContexts.empty()

}

/**
 * A generic holder for correlation data - there is a unique id and 'type' which will
 * help in determining which system generated the correlation data.
 *
 */
data class CorrelationContext(
    val type: String,
    val id: UniqueId,
    val propagate: Boolean = false
) {
    constructor(type: String, id: String, propagate: Boolean = false) : this(type, UniqueId(id), propagate)

    override fun toString() = "$type-$id"
}

/**
 * Type safe list for simple serialisation
 */
class CorrelationContexts(data: List<CorrelationContext>) : ArrayList<CorrelationContext>(data) {
    companion object {

        fun empty() = CorrelationContexts(emptyList())

        fun listOf(vararg elements: CorrelationContext): CorrelationContexts = CorrelationContexts(elements.asList())

        fun single(type: String, id: UniqueId, propagate: Boolean = false): CorrelationContexts {
            return single(CorrelationContext(type, id, propagate))
        }

        fun single(type: String, id: String, propagate: Boolean = false): CorrelationContexts {
            return single(CorrelationContext(type, id, propagate))
        }

        fun single(correlation: CorrelationContext): CorrelationContexts {
            return listOf(correlation)
        }
    }
}

