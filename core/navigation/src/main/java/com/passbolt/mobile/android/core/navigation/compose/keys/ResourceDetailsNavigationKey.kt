package net.svaroh.passly.core.navigation.compose.keys

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import net.svaroh.passly.ui.ResourceUiModel

sealed interface ResourceDetailsNavigationKey : NavKey {
    @Serializable
    data class ResourceDetails(
        // Pass ResourceUiModel instead of resourceId to load initial resource details screen
        // even if refresh in progress and Resources table currently empty
        val resourceModel: ResourceUiModel,
    ) : ResourceDetailsNavigationKey
}
