/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

package net.svaroh.passly.initializers

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.startup.Initializer
import net.svaroh.passly.appModule
import net.svaroh.passly.common.commonModule
import net.svaroh.passly.core.accounts.accountsCoreModule
import net.svaroh.passly.core.authenticationcore.authenticationCoreModule
import net.svaroh.passly.core.autofill.autofillModule
import net.svaroh.passly.core.clipboard.clipboardModule
import net.svaroh.passly.core.commonfolders.foldersModule
import net.svaroh.passly.core.commongroups.groupsModule
import net.svaroh.passly.core.coreUiModule
import net.svaroh.passly.core.envinfo.envInfoModule
import net.svaroh.passly.core.fulldatarefresh.fullDataRefreshModule
import net.svaroh.passly.core.idlingresource.idlingResourcesModule
import net.svaroh.passly.core.inappreview.inAppReviewModule
import net.svaroh.passly.core.logger.loggerModule
import net.svaroh.passly.core.mvp.architectureModule
import net.svaroh.passly.core.navigation.navigationModule
import net.svaroh.passly.core.networking.networkingModule
import net.svaroh.passly.core.notifications.notificationsModule
import net.svaroh.passly.core.otpcore.otpCoreModule
import net.svaroh.passly.core.passphrasememorycache.passphraseMemoryCacheModule
import net.svaroh.passly.core.passwordgenerator.passwordGeneratorModule
import net.svaroh.passly.core.policiesModule
import net.svaroh.passly.core.preferences.preferencesModule
import net.svaroh.passly.core.qrscan.barcodeScanModule
import net.svaroh.passly.core.rbacModule
import net.svaroh.passly.core.resources.resourcesModule
import net.svaroh.passly.core.resourcetypes.resourceTypesModule
import net.svaroh.passly.core.secrets.secretsModule
import net.svaroh.passly.core.security.securityModule
import net.svaroh.passly.core.tags.tagsModule
import net.svaroh.passly.core.users.usersModule
import net.svaroh.passly.createFolderModule
import net.svaroh.passly.createresourcemenu.createResourceMenuModule
import net.svaroh.passly.database.databaseModule
import net.svaroh.passly.encryptedstorage.encryptedStorageModule
import net.svaroh.passly.feature.accountdetails.accountDetailsModule
import net.svaroh.passly.feature.authenticationModule
import net.svaroh.passly.feature.autofill.autofillResourcesModule
import net.svaroh.passly.feature.home.homeModule
import net.svaroh.passly.feature.main.mainModule
import net.svaroh.passly.feature.otp.otpModule
import net.svaroh.passly.feature.otp.scanOtpMainModule
import net.svaroh.passly.feature.resourcedetails.resourceDetailsModule
import net.svaroh.passly.feature.resourceform.resourceFormModule
import net.svaroh.passly.feature.settings.settingsModule
import net.svaroh.passly.feature.setup.setupModule
import net.svaroh.passly.feature.startup.startUpModule
import net.svaroh.passly.feature.transferaccounttoanotherdevice.transferAccountToAnotherDeviceModule
import net.svaroh.passly.featureflags.featureFlagsModule
import net.svaroh.passly.folderDetailsModule
import net.svaroh.passly.gopenpgp.openPgpModule
import net.svaroh.passly.groupDetailsModule
import net.svaroh.passly.helpMenuModule
import net.svaroh.passly.jsonmodel.jsonModelModule
import net.svaroh.passly.linksapi.linksApiModule
import net.svaroh.passly.locationDetailsModule
import net.svaroh.passly.logsModule
import net.svaroh.passly.mappersModule
import net.svaroh.passly.metadata.metadataModule
import net.svaroh.passly.otpMoreMenuModule
import net.svaroh.passly.passboltapi.passboltApiModule
import net.svaroh.passly.permissions.permissionsModule
import net.svaroh.passly.pwnedpasswordsapi.pwnedPasswordsApiModule
import net.svaroh.passly.resourceMoreMenuModule
import net.svaroh.passly.resourcePickerModule
import net.svaroh.passly.resourceTagsModule
import net.svaroh.passly.serializers.serializersModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Initializes the dependency injection framework.
 */
@Suppress("unused")
class KoinInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        startKoin {
            androidContext(context)
            modules(appModules)
        }
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()

    companion object {
        @VisibleForTesting
        val appModules =
            listOf(
                appModule,
                openPgpModule,
                setupModule,
                mappersModule,
                architectureModule,
                networkingModule,
                barcodeScanModule,
                passboltApiModule,
                autofillResourcesModule,
                authenticationModule,
                homeModule,
                settingsModule,
                startUpModule,
                resourcesModule,
                featureFlagsModule,
                databaseModule,
                secretsModule,
                resourceDetailsModule,
                securityModule,
                linksApiModule,
                usersModule,
                loggerModule,
                accountDetailsModule,
                foldersModule,
                folderDetailsModule,
                mainModule,
                groupsModule,
                commonModule,
                coreUiModule,
                locationDetailsModule,
                createFolderModule,
                groupDetailsModule,
                resourceTagsModule,
                helpMenuModule,
                logsModule,
                resourceMoreMenuModule,
                fullDataRefreshModule,
                resourceTypesModule,
                notificationsModule,
                autofillModule,
                inAppReviewModule,
                envInfoModule,
                idlingResourcesModule,
                transferAccountToAnotherDeviceModule,
                otpModule,
                otpCoreModule,
                serializersModule,
                resourcePickerModule,
                tagsModule,
                scanOtpMainModule,
                otpMoreMenuModule,
                rbacModule,
                accountsCoreModule,
                policiesModule,
                pwnedPasswordsApiModule,
                passwordGeneratorModule,
                metadataModule,
                encryptedStorageModule,
                authenticationCoreModule,
                preferencesModule,
                passphraseMemoryCacheModule,
                jsonModelModule,
                createResourceMenuModule,
                resourceFormModule,
                permissionsModule,
                navigationModule,
                clipboardModule,
            )
    }
}
