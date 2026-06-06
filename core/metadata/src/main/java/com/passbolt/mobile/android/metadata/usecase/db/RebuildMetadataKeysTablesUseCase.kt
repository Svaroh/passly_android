package net.svaroh.passly.metadata.usecase.db

import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.core.accounts.usecase.SelectedAccountUseCase
import net.svaroh.passly.ui.ParsedMetadataKeyModel

class RebuildMetadataKeysTablesUseCase(
    private val removeLocalMetadataKeysUseCase: RemoveLocalMetadataKeysUseCase,
    private val addLocalMetadataKeys: AddLocalMetadataKeysUseCase,
) : AsyncUseCase<RebuildMetadataKeysTablesUseCase.Input, Unit>,
    SelectedAccountUseCase {
    override suspend fun execute(input: Input) {
        removeLocalMetadataKeysUseCase.execute(UserIdInput(selectedAccountId))
        addLocalMetadataKeys.execute(AddLocalMetadataKeysUseCase.Input(input.metadataKeys))
    }

    data class Input(
        val metadataKeys: List<ParsedMetadataKeyModel>,
    )
}
