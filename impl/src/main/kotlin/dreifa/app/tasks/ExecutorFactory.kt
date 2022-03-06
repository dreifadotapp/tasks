package dreifa.app.tasks

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Need to wrap ExecutorService - having trouble identifying ExecutorService correctly
 * via reflections correctly with the Registry, and this work around resolves the problem
 */
interface ExecutorFactory {
    fun executorService(): ExecutorService
}

class SingleThreadedExecutor : ExecutorFactory {
    override fun executorService(): ExecutorService {
        return Executors.newSingleThreadExecutor()
    }
}

class FixedThreadPoolExecutor(private val maxThreads: Int = 10) : ExecutorFactory {
    override fun executorService(): ExecutorService {
        return Executors.newFixedThreadPool(maxThreads)
    }
}