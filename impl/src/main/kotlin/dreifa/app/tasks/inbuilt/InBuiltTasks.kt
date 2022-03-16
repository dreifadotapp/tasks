package dreifa.app.tasks.inbuilt

import dreifa.app.tasks.SimpleTaskRegistrations
import dreifa.app.tasks.TaskRegistration
import dreifa.app.tasks.inbuilt.fileBundle.FBQueryTaskImpl
import dreifa.app.tasks.inbuilt.fileBundle.FBRetrieveTaskImpl
import dreifa.app.tasks.inbuilt.fileBundle.FBStoreTaskImpl

class InBuiltTasks : SimpleTaskRegistrations(
    listOf(
        // FileBundle
        TaskRegistration(FBQueryTaskImpl::class),
        TaskRegistration(FBRetrieveTaskImpl::class),
        TaskRegistration(FBStoreTaskImpl::class),

        // Networking
        TaskRegistration(DeterminePrivateIpAddressTaskImpl::class),
        TaskRegistration(DeterminePublishedIpAddressTaskImpl::class)
    )
)