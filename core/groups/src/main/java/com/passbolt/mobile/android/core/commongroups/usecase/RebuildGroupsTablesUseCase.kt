package net.svaroh.passly.core.commongroups.usecase

import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import net.svaroh.passly.core.commongroups.usecase.db.AddLocalGroupsUseCase
import net.svaroh.passly.core.commongroups.usecase.db.RemoveLocalGroupsUseCase
import net.svaroh.passly.ui.GroupModelWithUsers

class RebuildGroupsTablesUseCase(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val removeLocalGroupsUseCase: RemoveLocalGroupsUseCase,
    private val addLocalGroupsUseCase: AddLocalGroupsUseCase,
) : AsyncUseCase<RebuildGroupsTablesUseCase.Input, Unit> {
    override suspend fun execute(input: Input) {
        val selectedAccount = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        removeLocalGroupsUseCase.execute(UserIdInput(selectedAccount))
        addLocalGroupsUseCase.execute(AddLocalGroupsUseCase.Input(input.groups))
    }

    class Input(
        val groups: List<GroupModelWithUsers>,
    )
}
