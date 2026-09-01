package com.passbolt.mobile.android.accountinit

import android.os.Build
import com.passbolt.mobile.android.common.usecase.UserIdInput
import com.passbolt.mobile.android.domain.accounts.usecase.SaveAccountUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.SaveCurrentApiUrlUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.SaveSelectedAccountUseCase
import com.passbolt.mobile.android.domain.accounts.usecase.UpdateAccountDataUseCase
import com.passbolt.mobile.android.domain.auth.usecase.SaveResourcesDatabasePassphraseUseCase
import com.passbolt.mobile.android.domain.preferences.GlobalPreferencesRepository
import com.passbolt.mobile.android.domain.preferences.GlobalPreferencesUpdate
import com.passbolt.mobile.android.domain.privatekey.PrivateKeyRepository
import com.passbolt.mobile.android.domain.privatekey.model.PrivateKey
import com.passbolt.mobile.android.intents.ManagedAccountIntentCreator
import org.koin.core.component.KoinComponent

class AccountInitializer(
    private val saveCurrentApiUrlUseCase: SaveCurrentApiUrlUseCase,
    private val saveResourcesDatabasePassphraseUseCase: SaveResourcesDatabasePassphraseUseCase,
    private val saveSelectedAccountUseCase: SaveSelectedAccountUseCase,
    private val updateAccountDataUseCase: UpdateAccountDataUseCase,
    private val privateKeyRepository: PrivateKeyRepository,
    private val managedAccountIntentCreator: ManagedAccountIntentCreator,
    private val saveAccountUseCase: SaveAccountUseCase,
    private val globalPreferencesRepository: GlobalPreferencesRepository,
) : KoinComponent {
    fun initializeAccount() {
        saveCurrentApiUrlUseCase.execute(
            SaveCurrentApiUrlUseCase.Input(managedAccountIntentCreator.getDomain()),
        )
        saveSelectedAccountUseCase.execute(
            UserIdInput(managedAccountIntentCreator.getUserLocalId()),
        )
        saveAccountUseCase.execute(
            UserIdInput(managedAccountIntentCreator.getUserLocalId()),
        )
        saveResourcesDatabasePassphraseUseCase.execute(
            SaveResourcesDatabasePassphraseUseCase.Input(TEST_DATABASE_PASSWORD),
        )
        updateAccountDataUseCase.execute(
            UpdateAccountDataUseCase.Input(
                userId = managedAccountIntentCreator.getUserLocalId(),
                url = managedAccountIntentCreator.getDomain(),
                firstName = managedAccountIntentCreator.getFirstName(),
                lastName = managedAccountIntentCreator.getLastName(),
                email = managedAccountIntentCreator.getUsername(),
                serverId = managedAccountIntentCreator.getUserServerId(),
            ),
        )
        privateKeyRepository.savePrivateKey(
            managedAccountIntentCreator.getUserLocalId(),
            PrivateKey(managedAccountIntentCreator.getArmoredPrivateKey()),
        )
        globalPreferencesRepository.updateGlobalPreferences(
            GlobalPreferencesUpdate(
                areDebugLogsEnabled = false,
                isHideRootDialogEnabled = false,
                deprecatedOsWarningHiddenForSdk = Build.VERSION.SDK_INT,
            ),
        )
    }

    private companion object {
        private const val TEST_DATABASE_PASSWORD = "TEST_DB_PASS"
    }
}
