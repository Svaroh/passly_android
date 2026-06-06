package net.svaroh.passly.passboltapi

import net.svaroh.passly.passboltapi.auth.authApiModule
import net.svaroh.passly.passboltapi.expiry.passwordExpiryApiModule
import net.svaroh.passly.passboltapi.favourites.favouritesApiModule
import net.svaroh.passly.passboltapi.folders.foldersApiModule
import net.svaroh.passly.passboltapi.groups.groupsApiModule
import net.svaroh.passly.passboltapi.metadata.metadataApiModule
import net.svaroh.passly.passboltapi.mfa.mfaApiModule
import net.svaroh.passly.passboltapi.passwordpolicies.passwordPoliciesApiModule
import net.svaroh.passly.passboltapi.rbac.rbacApiModule
import net.svaroh.passly.passboltapi.registration.mobileTransferApiModule
import net.svaroh.passly.passboltapi.resource.resourceApiModule
import net.svaroh.passly.passboltapi.resourcetypes.resourceTypesApiModule
import net.svaroh.passly.passboltapi.secrets.secretsApiModule
import net.svaroh.passly.passboltapi.settings.settingsApiModule
import net.svaroh.passly.passboltapi.share.shareApiModule
import net.svaroh.passly.passboltapi.users.usersApiModule
import org.koin.dsl.module

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
val passboltApiModule =
    module {
        mobileTransferApiModule()
        authApiModule()
        mfaApiModule()
        secretsApiModule()
        resourceApiModule()
        resourceTypesApiModule()
        foldersApiModule()
        groupsApiModule()
        usersApiModule()
        settingsApiModule()
        shareApiModule()
        favouritesApiModule()
        rbacApiModule()
        passwordExpiryApiModule()
        passwordPoliciesApiModule()
        metadataApiModule()
    }
