package net.svaroh.passly.core.tags

import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import net.svaroh.passly.core.tags.usecase.db.AddLocalTagsUseCase
import net.svaroh.passly.ui.ResourceModelWithAttributes

// TODO MOB-3051 do not delete existing when rebuilding
class RebuildTagsTablesUseCase(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val addLocalTagsUseCase: AddLocalTagsUseCase,
) : AsyncUseCase<RebuildTagsTablesUseCase.Input, Unit> {
    override suspend fun execute(input: Input) {
        val selectedAccount = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
//        removeLocalTagsUseCase.execute(UserIdInput(selectedAccount))
        addLocalTagsUseCase.execute(
            AddLocalTagsUseCase.Input(
                input.tags,
                selectedAccount,
            ),
        )
    }

    data class Input(
        val tags: List<ResourceModelWithAttributes>,
    )
}
