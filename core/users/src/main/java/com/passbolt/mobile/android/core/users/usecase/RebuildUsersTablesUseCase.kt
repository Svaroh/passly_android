package net.svaroh.passly.core.users.usecase

import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.core.accounts.usecase.selectedaccount.GetSelectedAccountUseCase
import net.svaroh.passly.core.users.usecase.db.AddLocalUsersUseCase
import net.svaroh.passly.core.users.usecase.db.RemoveLocalUsersUseCase
import net.svaroh.passly.ui.UserModel

class RebuildUsersTablesUseCase(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val removeLocalUsersUseCase: RemoveLocalUsersUseCase,
    private val addLocalUsersUseCase: AddLocalUsersUseCase,
) : AsyncUseCase<RebuildUsersTablesUseCase.Input, Unit> {
    override suspend fun execute(input: Input) {
        val selectedAccount = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        removeLocalUsersUseCase.execute(UserIdInput(selectedAccount))
        addLocalUsersUseCase.execute(AddLocalUsersUseCase.Input(input.users))
    }

    class Input(
        val users: List<UserModel>,
    )
}
