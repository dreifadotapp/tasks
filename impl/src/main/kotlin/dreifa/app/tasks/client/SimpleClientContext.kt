package dreifa.app.tasks.client

import dreifa.app.opentelemetry.OpenTelemetryContext
import dreifa.app.tasks.logging.LoggingChannelLocator
import dreifa.app.types.CorrelationContexts

/**
 * Enough for unit test and to communicate with tasks running locally
 */
class SimpleClientContext(
    private val loggingChannelLocator: LoggingChannelLocator = LoggingChannelLocator.inMemory(),
    private val telemetryContext: OpenTelemetryContext = OpenTelemetryContext.root,
    private val correlation: CorrelationContexts = CorrelationContexts.empty()
) :
    ClientContext {
    private val principle = NotAuthenticatedSecurityPrinciple()
    override fun securityPrinciples(): Set<SecurityPrinciple> = setOf(principle)
    override fun logChannelLocator(): LoggingChannelLocator = loggingChannelLocator
    override fun telemetryContext(): OpenTelemetryContext = telemetryContext
    override fun correlation(): CorrelationContexts = correlation
}
