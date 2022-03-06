package dreifa.app.tasks.processManager


import java.io.BufferedReader
import java.io.Closeable
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.*
import kotlin.collections.HashMap
import kotlin.concurrent.thread

/**
 * Keeps track of running processes. This implementation is for internal use only. Clients
 * should use ProcessManager.
 */
class ProcessManagerInternal constructor(
    private val defaultOutputSink: (String) -> Unit = { msg -> ProcessMonitor.consoleMessageSink(msg) },
    private val defaultErrorSink: (String) -> Unit = { msg -> ProcessMonitor.consoleMessageSink(msg) },
    private val defaultOnCompletedSink: (ManagedProcess, Int) -> Unit = { _, _ -> }

) {

    private val processList = ArrayList<ManagedProcess>()
    private val processMonitors = HashMap<UUID, ProcessMonitor>()

    /**
     * Register a new Process. Can override all the default sinks
     */
    fun register(
        process: Process,
        id: UUID = UUID.randomUUID(),
        label: String = "",
        outputSink: ((String) -> Unit)? = null,
        errorSink: ((String) -> Unit)? = null,
        onCompletedSink: ((ManagedProcess, Int) -> Unit)? = null
    ) {

        val actualOutputSink = outputSink ?: defaultOutputSink
        val actualErrorSink = errorSink ?: defaultErrorSink
        val actualOnCompletedSink = onCompletedSink ?: defaultOnCompletedSink

        val mp = ManagedProcess(process, id, label, actualOutputSink, actualErrorSink, actualOnCompletedSink)
        processList.add(mp)
        processMonitors[id] = mp.processMonitor
    }

    /**
     * Lookup a process using the Java Process class
     */
    fun findByProcess(process: Process): ProcessInfo? {
        val mp = processList.singleOrNull() { it.process == process }
        return if (mp == null) null else mapToProcessInfo(mp)
    }

    /**
     * Lookup a process using its UUID
     */
    fun findById(id: UUID): ProcessInfo? {
        val mp = processList.singleOrNull { it.id == id }
        return if (mp == null) null else mapToProcessInfo(mp)
    }

    /**
     * Lookup a process using its label
     */
    fun findByLabel(label: String): ProcessInfo? {
        val mp = processList.singleOrNull { it.label == label }
        return if (mp == null) null else mapToProcessInfo(mp)
    }

    /**
     * Simply list all running processes
     */
    fun allProcesses(): List<ProcessInfo> {
        return processList.map { mapToProcessInfo(it) }
    }


    private fun mapToProcessInfo(mp: ManagedProcess): ProcessInfo {
        val monitor = processMonitors[mp.id]!!
        val pid = getPidOfProcess(mp.process)
        return ProcessInfo(process = mp.process, id = mp.id, label = mp.label, monitor = monitor, pid = pid)
    }


    /**
     * Kill everything - rather brutal, not for everyday use.
     */
    @Synchronized
    fun killAll() {

        val processListClone = ArrayList<ManagedProcess>(processList);

        for (p in processListClone) {
            println("Forcibly killing $p")
            kill(p.process)
        }
        processList.clear()

    }

    /**
     * Kill the process
     */
    @Synchronized
    fun kill(process: Process, forcibly: Boolean = false) {

        val pid = getPidOfProcess(process)
        if (pid != -1L) {
            pkill(pid)
        }
        if (forcibly) {
            try {
                process.destroyForcibly()
            } catch (ignored: Exception) {
            }
        }

        // remove from our list if its here
        val mp = processList.singleOrNull() { it.process == process }
        if (mp != null) {
            processList.remove(mp)
            val monitor = processMonitors.get(mp.id)
            if (monitor != null) {
                monitor.stopMonitor()
                processMonitors.remove(mp.id)
            }
        }

    }


    @Synchronized
    fun getPidOfProcess(p: Process): Long {
        var pid: Long = -1

        try {
            if (p.javaClass.name == "java.lang.UNIXProcess") {
                val f = p.javaClass.getDeclaredField("pid")
                f.isAccessible = true
                pid = f.getLong(p)
                f.isAccessible = false
            }
        } catch (e: Exception) {
            pid = -1
        }

        return pid
    }

    private fun pkill(pid: Long) {
        try {
            //logger.debug("using pkill on PID $pid")
            val pb = ProcessBuilder(listOf("pkill", "-P", pid.toString()))
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

            pb.waitFor(5, TimeUnit.SECONDS)
            //logger.debug("stdout: " + pb.inputStream.bufferedReader().use { it.readText() })
            //logger.debug("err:" + pb.errorStream.bufferedReader().use { it.readText() })
        } catch (ignored: Exception) {
        }
    }

    /**
     * Keeps track of the process and associated information
     */
    data class ManagedProcess(
        val process: Process,
        val id: UUID = UUID.randomUUID(),
        val label: String = "???",
        val outputSink: (String) -> Unit = { },
        val errorSink: (String) -> Unit = { },
        val processCompletedSink: (ManagedProcess, Int) -> Unit = { mp, exitcode -> println("Managed process $mp exited with code $exitcode") }
    ) {
        val processMonitor: ProcessMonitor

        init {
            processMonitor = ProcessMonitor(this, outputSink, errorSink, processCompletedSink)
        }
    }


}

data class ProcessInfo(
    val process: Process,
    val id: UUID,
    val label: String,
    val monitor: ProcessMonitor,
    val pid: Long
)


/**
 * Kicks of threads to monitor a running process and capture output.
 *
 * TODO The current implementation is not particularly scalable as it will create 3 threads per
 *      monitored process.
 * TODO This should be better integrated with the Java ProcessBuilder class - will potentially
 *      miss the first few milliseconds of output as the monitor is attached AFTER the process has started.
 */
class ProcessMonitor constructor(
    private val managedProcess: ProcessManagerInternal.ManagedProcess,
    private val outputSink: (String) -> Unit = { msg -> consoleMessageSink(msg) },
    private val errorSink: (String) -> Unit = { msg -> consoleMessageSink(msg) },
    private val processCompletedSink: (ProcessManagerInternal.ManagedProcess, Int) -> Unit = { mp, exitcode -> println("Managed process $mp exited with code $exitcode") }
) : Closeable {

    private var exitCode: Int? = null
    private val outputMonitor: Thread
    private val errorMonitor: Thread

    init {
        outputMonitor = monitorOutput()
        errorMonitor = monitorError()
        monitorRunning()
    }

    private fun monitorOutput(): Thread {
        return thread() {
            val br = BufferedReader(InputStreamReader(managedProcess.process.inputStream))
            var line: String? = br.readLine()
            do {
                while (line != null) {

                    outputSink.invoke(line)
                    line = br.readLine()
                }
                Thread.sleep(1000)
            } while (true)
        }
    }

    private fun monitorError(): Thread {
        return thread() {
            val br = BufferedReader(InputStreamReader(managedProcess.process.errorStream))
            var line: String? = br.readLine()
            do {
                while (line != null) {
                    errorSink.invoke(line)
                    line = br.readLine()
                }
                Thread.sleep(1000)
            } while (true)
        }
    }

    private fun monitorRunning() {
        thread() {
            do {
                if (managedProcess.process.isAlive) {
                    Thread.sleep(1000)
                } else {
                    exitCode = managedProcess.process.exitValue()
                    outputSink("${managedProcess.id} has completed with exit code $exitCode")
                    processCompletedSink.invoke(managedProcess, exitCode as Int)

                    stopMonitor()

                    break;
                }

            } while (true)
        }
    }

    private fun isRunning(): Boolean {
        return exitCode == null
    }

    fun waitFor(executors: ExecutorService = Executors.newSingleThreadExecutor()): Future<Int> {
        return executors.submit<Int> {
            while (isRunning()) {
                Thread.sleep(1000)
            }
            exitCode!!
        }
    }

    fun exitCode(): Int {
        return exitCode!!
    }

    fun stopMonitor() {
        //  fixme !
        outputMonitor.stop()
        errorMonitor.stop()
    }

    override fun close() {
        stopMonitor()
    }

    fun finalize() {
        // generally bad practice, but seems the better alternative to
        // leaving threads running as nothing is enforcing use of Closable
        try {
            stopMonitor()
        } catch (ignored: Throwable) {
        }
    }

    /**
     * A default sink (consumer) of messages that just prints to the console
     */
    companion object {
        fun consoleMessageSink(m: String) {
            println(m)
        }
    }
}