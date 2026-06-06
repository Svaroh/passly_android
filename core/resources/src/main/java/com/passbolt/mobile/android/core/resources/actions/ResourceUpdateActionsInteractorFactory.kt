package net.svaroh.passly.core.resources.actions

import net.svaroh.passly.ui.ResourceModel

fun interface ResourceUpdateActionsInteractorFactory {
    fun create(resource: ResourceModel): ResourceUpdateActionsInteractor
}
