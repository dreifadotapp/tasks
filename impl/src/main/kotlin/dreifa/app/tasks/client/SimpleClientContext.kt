package dreifa.app.tasks.client

import dreifa.app.opentelemetry.OpenTelemetryContext
import dreifa.app.tasks.logging.LoggingChannelLocator
import dreifa.app.types.CorrelationContexts

/**
 * Enough for unit test and to communicate with tasks running locally
 */
class SimpleClientContext(
    private val loggingChannelLocator: LoggingChannelLocator = LoggingChannelLocator.inMemory(),
    private val telemetryContext: OpenTelemetryContext = OpenTelemetryContext.root(),
    private val correlation: CorrelationContexts = CorrelationContexts.empty(),
    private val principles: Set<SecurityPrinciple> = setOf(NotAuthenticatedSecurityPrinciple())

) :
    ClientContext {
    override fun securityPrinciples(): Set<SecurityPrinciple> = principles
    override fun logChannelLocator(): LoggingChannelLocator = loggingChannelLocator
    override fun telemetryContext(): OpenTelemetryContext = telemetryContext
    override fun correlation(): CorrelationContexts = correlation
    override fun withTelemetryContext(telemetryContext: OpenTelemetryContext): ClientContext {
        return DefaultClientContextModifier(this).withTelemetryContext(telemetryContext)
    }

    override fun withCorrelation(correlation: CorrelationContexts): ClientContext {
        return DefaultClientContextModifier(this).withCorrelation(correlation)
    }
}
