package net.svaroh.passly.tagsdetails

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel

fun Module.resourceTagsModule() {
    viewModel { params ->
        ResourceTagsViewModel(
            coroutineLaunchContext = get(),
            resourceId = params.get(),
            getLocalResourceUseCase = get(),
            getLocalResourceTagsUseCase = get(),
            dataRefreshTrackingFlow = get(),
        )
    }
}
