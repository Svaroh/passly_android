package net.svaroh.passly.domain.resources.actions

import net.svaroh.passly.ui.ResourceUiModel

fun interface SecretPropertiesActionsInteractorFactory {
    fun create(resource: ResourceUiModel): SecretPropertiesActionsInteractor
}
