package dreifa.app.tasks.client

import dreifa.app.tasks.logging.LoggingChannelLocator

/**
 * Enough for unit test and to communicate with tasks running locally
 */
class SimpleClientContext(private val loggingChannelLocator: LoggingChannelLocator = LoggingChannelLocator.inMemory()) :
    ClientContext {
    private val principle = NotAuthenticatedSecurityPrinciple()
    override fun securityPrinciples(): Set<SecurityPrinciple> = setOf(principle)
    override fun logChannelLocator(): LoggingChannelLocator = loggingChannelLocator
}
