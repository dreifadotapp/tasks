package dreifa.app.tasks.client

import dreifa.app.opentelemetry.OpenTelemetryContext
import dreifa.app.tasks.logging.LoggingChannelLocator

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
    fun telemetryContext () : OpenTelemetryContext

}
