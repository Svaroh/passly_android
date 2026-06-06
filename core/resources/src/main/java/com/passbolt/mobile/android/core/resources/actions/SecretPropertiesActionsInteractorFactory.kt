package net.svaroh.passly.core.resources.actions

import net.svaroh.passly.ui.ResourceModel

fun interface SecretPropertiesActionsInteractorFactory {
    fun create(resource: ResourceModel): SecretPropertiesActionsInteractor
}
