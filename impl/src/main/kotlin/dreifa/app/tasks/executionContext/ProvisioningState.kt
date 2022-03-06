package dreifa.app.tasks.executionContext

import dreifa.app.helpers.random
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.HashMap

// marker interface - TBD
interface PlatformContext

/**
 * The state that lives for the lifetime of the provisioning.
 *
 */
interface ProvisioningState {
    /**
     * The unique id
     */
    fun provisioningId(): String

    /**
     * Retrieve any platform information, like connections and regions
     */
    fun platformContext(name: String): PlatformContext

    fun storePlatformContext(name: String, platformContext: PlatformContext): ProvisioningState

    /**
     * A tag to uniquely identity any resources. It is typically a short string similar
     * to an airline booking reference. It will get built into name and identifiers as necessary.
     */
    fun tag(): String

    /**
     * Provisioning is a multistage process, for example the first stage might create a cloud server. There is often
     * a need to take the outputs from a stage and pass them onto the next stage, for example an AWS provisoing stage
     * may need to pass on information like the public IP address of the server.
     *
     * Downstream stage simply look for a key value to be set under the  key / value
     *
     * This problem has some subtleties. There is the distinction between a generic property (for example any server
     * provisioning stage should capture the public ip address) to the a platform specific output.
     */
    fun outputs(stage: String): Map<String, Any>

    fun stages(): List<String>


    fun storeOutput(stage: String, output: Map<String, Any>): ProvisioningState

    /**
     * Modify the tag
     */
    fun withTag(tag: String): ProvisioningState


}

class DefaultProvisioningState(
    private val id: String = UUID.randomUUID().toString(),
    private val platforms: Map<String, PlatformContext> = emptyMap(),
    private val tag: String = String.random(length = 6)
) : ProvisioningState {


    val platformLookup = HashMap(platforms)
    private val outputs = HashMap<String, Map<String, Any>>()


    override fun storePlatformContext(name: String, platformContext: PlatformContext): ProvisioningState {
        platformLookup[name] = platformContext
        return this
    }

    override fun provisioningId(): String {
        return id
    }

    override fun platformContext(name: String): PlatformContext {
        return platformLookup[name]!!
    }

    override fun tag(): String {
        return tag
    }

    override fun outputs(stage: String): Map<String, Any> {
        return outputs.getOrDefault(stage, emptyMap())
    }

    override fun stages(): List<String> {
        return outputs.keys.sorted()
    }

    override fun storeOutput(stage: String, output: Map<String, Any>): ProvisioningState {
        outputs[stage] = output
        return this
    }

//    override fun withTag(tag: String): ProvisioningState {
//        TODO("Not yet implemented")
//    }

    override fun withTag(tag: String): ProvisioningState {
        return DefaultProvisioningState(
            id = this.id,
            platforms = HashMap(this.platformLookup),
            tag = tag
        )
    }
//
    //fun withTag(tag: Tag): ProvisioningState {
    //    return withTag(tag.value())
    //}

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("provisioningId = ${provisioningId()}\n")
        sb.append("tag = ${tag()}\n")
        sb.append("stages:")
        stages().forEach {
            sb.append("  $it \n")
            outputs(it).entries.forEach { entry ->
                sb.append("    ${entry.key} = ${entry.value}\n")
            }
        }
        sb.append("platforms:")
        platforms.forEach {
            sb.append("  ${it.key}:\n")
            sb.append("  ${it.value}\n\n")
        }

        return sb.toString()
    }
}

