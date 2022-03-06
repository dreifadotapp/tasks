package dreifa.app.tasks.processManager

import dreifa.app.registry.Registry
import java.util.*


/*
 Some workarounds to the ProcessManagerInternal. Really should fix the ProcessManagerInternal
 which has got more complicated than necessary.
 */

class CaptureOutput {
    private var sb = StringBuilder()

    //@Synchronized
    fun messageSink(m: String) {
        sb.appendln(m)
    }

    //@Synchronized
    override fun toString(): String {
        return sb.toString()
    }
}

class CaptureCompleted {
    @Volatile
    var pm: ProcessManagerInternal.ManagedProcess? = null

    @Volatile
    var code: Int? = null

    fun sink(pm: ProcessManagerInternal.ManagedProcess, code: Int) {
        this.pm = pm
        this.code = code
    }
}

class Captured(
    val stdout: CaptureOutput = CaptureOutput(),
    val stderr: CaptureOutput = CaptureOutput(),
    val onCompleted: CaptureCompleted = CaptureCompleted()
)


data class ProcessRestartInfo(val newId: UUID, val oldId: UUID, val label: String)

class ProcessManager(registry: Registry = Registry()) {
    private val processManager: ProcessManagerInternal = registry.getOrElse(
        ProcessManagerInternal::class.java,
        ProcessManagerInternal()
    )
    private val captures = HashMap<UUID, Captured>()

    fun registerProcess(
        builder: ProcessBuilder,
        id: UUID,
        label: String,
        autoRestart: Boolean = false
    ) {
        autoRestart == autoRestart
        registerProcess(builder.start(), id, label)


    }


    fun restartAll(): List<ProcessRestartInfo> {
        val result = ArrayList<ProcessRestartInfo>()

        return result
    }

    /**
     * Register a Java process to be tracked and managed by the ProcessManager
     */
    private fun registerProcess(
        process: Process,
        id: UUID,
        label: String
    ) {

        val capture = Captured()
        captures[id] = capture

        processManager.register(process = process,
            id = id,
            label = label,
            outputSink = { capture.stdout.messageSink(it) },
            errorSink = { capture.stderr.messageSink(it) },
            onCompletedSink = { a, b -> capture.onCompleted.sink(a, b) })
    }


    /**
     * Get the captured output, if any, for the process
     */
    fun lookupOutput(id: UUID): Captured? {
        return captures[id]
    }

    /**
     * Find the process by Id
     */
    fun findById(id: UUID): ProcessInfo? {
        return processManager.findById(id)
    }

    /**
     * Find the process by label (name)
     */
    fun findByLabel(label: String): ProcessInfo? {
        return processManager.findByLabel(label)
    }

    /**
     * Kill the process as cleanly as possible
     */
    fun kill(id: UUID, forcibly: Boolean = false) {
        val processInfo = processManager.findById(id)
        if (processInfo != null) {
            processManager.kill(processInfo.process, forcibly)
        }
    }

    fun killAll() {
        processManager.killAll()
    }

    fun allProcesses(): List<ProcessInfo> {
        return processManager.allProcesses()
    }
}


