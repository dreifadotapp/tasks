package dreifa.app.tasks.inbuilt


import dreifa.app.tasks.BlockingTask
import dreifa.app.tasks.executionContext.ExecutionContext
import dreifa.app.tasks.logging.LogMessage
import dreifa.app.types.NotRequired
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.*

/**
 * Finds the local private IP address using this most reliable method available, starting
 * with the known cloud APIs or picks up a forced setting
 */

interface DeterminePrivateIpAddressTask : BlockingTask<NotRequired, String>

class DeterminePrivateIpAddressTaskImpl : DeterminePrivateIpAddressTask {
    private val taskId = UUID.randomUUID()
    override fun taskId(): UUID {
        return taskId
    }

    override fun exec(ctx: ExecutionContext, input: NotRequired): String {
        if (isForced()) {
            return forced()
        }

        // Am i on a PC or Mac
        if (isLaptop()) {
            return "localhost"
        }

        // AWS
        val aws = doRequest("http://169.254.169.254/latest/meta-data/local-ipv4")
        if (aws.success) {
            ctx.log(LogMessage.info("Found private ip address of ${aws.result} using AWS endpoint"))
            return aws.result
        }

        // Ask the OS
        val linux = doCommand(listOf("hostname", "-I"))
        if (linux.success) {
            ctx.log(LogMessage.info( "Found private ip address of ${linux.result} using 'hostname -I'"))
            return linux.result
        }

        // Ask the JVM
        val localhost = InetAddress.getLocalHost()
        val localIP = localhost.hostAddress.trim()
        ctx.log(LogMessage.info( "Found private ip address of $localIP from JVM"))
        return localIP
    }


    // using plain old java code to minimise 3rd party deps
    private fun doRequest(targetURL: String): RequestResult {
        try {
            val url = URL(targetURL)
            val connection = url.openConnection() as HttpURLConnection
            connection.readTimeout = 1000
            connection.connectTimeout = 1000
            connection.requestMethod = "GET"
            connection.useCaches = false
            connection.doOutput = false

            val input = connection.inputStream
            val rd = BufferedReader(InputStreamReader(input))

            val response = StringBuilder() // or StringBuffer if Java version 5+
            rd.lines().forEach {
                if (response.isNotEmpty()) response.append("\n")
                response.append(it)
            }

            rd.close()
            return RequestResult(response.toString().trim(), true)
        } catch (ex: Exception) {
            return RequestResult("", false)
        }
    }

    private fun doCommand(cmd: List<String>): RequestResult {
        val pb = ProcessBuilder()
        pb.command(cmd)
        val process = pb.start()
        val sb = StringBuilder()

        val reader = BufferedReader(InputStreamReader(process.inputStream))

        reader.lines().forEach {
            if (sb.isNotEmpty()) sb.append("\n")
            sb.append(it)
        }

        val exitCode = process.waitFor()
        return RequestResult(sb.toString().trim(), exitCode == 0)
    }

    private fun isLaptop(): Boolean {

        val osName = System.getProperties().getProperty("os.name").toLowerCase()
        if (osName.contains("mac")) return true
        if (osName.contains("win")) return true

        // todo - add tests for desktop builds of linux

        return false
    }

    private fun isForced(): Boolean {
        return System.getenv().containsKey("DREIFA_FORCE_PRIVATE_IP")
    }

    private fun forced(): String {
        return System.getenv("DREIFA_FORCE_PRIVATE_IP")
    }

    data class RequestResult(val result: String, val success: Boolean)
}


fun main() {
    val result = DeterminePrivateIpAddressTaskImpl().exec(input = NotRequired.instance())
    println(result)
}

