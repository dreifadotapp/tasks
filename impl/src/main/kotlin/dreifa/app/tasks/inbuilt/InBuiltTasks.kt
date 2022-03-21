package dreifa.app.tasks.inbuilt

import dreifa.app.tasks.SimpleTaskRegistrations
import dreifa.app.tasks.TaskRegistration
import dreifa.app.tasks.inbuilt.fileBundle.*
import dreifa.app.tasks.inbuilt.providers.*

class InBuiltTasks : SimpleTaskRegistrations(
    listOf(
        // FileBundle
        TaskRegistration(FBQueryTaskImpl::class, FBQueryTask::class),
        TaskRegistration(FBRetrieveTaskImpl::class, FBRetrieveTask::class),
        TaskRegistration(FBStoreTaskImpl::class, FBStoreTask::class),

        // Working with Providers
        TaskRegistration(TPScanJarTaskImpl::class, TPScanJarTask::class),
        TaskRegistration(TPRegisterProviderTaskImpl::class, TPRegisterProviderTask::class),
        TaskRegistration(TPQueryTaskImpl::class, TPQueryTask::class),
        TaskRegistration(TPInfoTaskImpl::class, TPInfoTask::class),

        // Networking
        TaskRegistration(DeterminePrivateIpAddressTaskImpl::class, DeterminePrivateIpAddressTask::class),
        TaskRegistration(DeterminePublishedIpAddressTaskImpl::class, DeterminePublishedIpAddressTask::class)
    )
)