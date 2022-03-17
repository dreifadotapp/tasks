package dreifa.app.tasks.inbuilt

import dreifa.app.tasks.SimpleTaskRegistrations
import dreifa.app.tasks.TaskRegistration
import dreifa.app.tasks.inbuilt.fileBundle.*
import dreifa.app.tasks.inbuilt.providers.TPScanJarTask
import dreifa.app.tasks.inbuilt.providers.TPScanJarTaskImpl

class InBuiltTasks : SimpleTaskRegistrations(
    listOf(
        // FileBundle
        TaskRegistration(FBQueryTaskImpl::class, FBQueryTask::class),
        TaskRegistration(FBRetrieveTaskImpl::class, FBRetrieveTask::class),
        TaskRegistration(FBStoreTaskImpl::class, FBStoreTask::class),

        // Working with Providers
        TaskRegistration(TPScanJarTaskImpl::class, TPScanJarTask::class),

        // Networking
        TaskRegistration(DeterminePrivateIpAddressTaskImpl::class, DeterminePrivateIpAddressTask::class),
        TaskRegistration(DeterminePublishedIpAddressTaskImpl::class, DeterminePublishedIpAddressTask::class)
    )
)