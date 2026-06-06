package net.svaroh.passly.domain.resources.actions

import net.svaroh.passly.ui.ResourceUiModel

fun interface ResourceUpdateActionsInteractorFactory {
    fun create(resource: ResourceUiModel): ResourceUpdateActionsInteractor
}
