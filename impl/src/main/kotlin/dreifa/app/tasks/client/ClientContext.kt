package dreifa.app.tasks.client

import dreifa.app.opentelemetry.OpenTelemetryContext
import dreifa.app.tasks.logging.LoggingChannelLocator
import dreifa.app.types.CorrelationContexts

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
interface ClientContext : ClientContextModifier {

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
    fun correlation(): CorrelationContexts
}

interface ClientContextModifier {

    fun withTelemetryContext(telemetryContext: OpenTelemetryContext): ClientContext

    fun withCorrelation(correlation: CorrelationContexts): ClientContext
}

class DefaultClientContextModifier(private val ctx: ClientContext) : ClientContextModifier {
    override fun withTelemetryContext(telemetryContext: OpenTelemetryContext): ClientContext {
        return SimpleClientContext(
            loggingChannelLocator = ctx.logChannelLocator(),
            telemetryContext = telemetryContext,
            correlation = ctx.correlation(),
            principles = ctx.securityPrinciples()
        )
    }

    override fun withCorrelation(correlation: CorrelationContexts): ClientContext {
        return SimpleClientContext(
            loggingChannelLocator = ctx.logChannelLocator(),
            telemetryContext = ctx.telemetryContext(),
            correlation = correlation,
            principles = ctx.securityPrinciples()
        )
    }

}
