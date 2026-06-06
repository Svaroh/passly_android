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

package net.svaroh.passly

import net.svaroh.passly.mappers.AccountModelMapper
import net.svaroh.passly.mappers.CreateResourceMapper
import net.svaroh.passly.mappers.EntropyViewMapper
import net.svaroh.passly.mappers.FolderModelMapper
import net.svaroh.passly.mappers.GroupsModelMapper
import net.svaroh.passly.mappers.HomeDisplayViewMapper
import net.svaroh.passly.mappers.MetadataMapper
import net.svaroh.passly.mappers.OtpModelMapper
import net.svaroh.passly.mappers.PasswordExpiryMapper
import net.svaroh.passly.mappers.PasswordPoliciesMapper
import net.svaroh.passly.mappers.PermissionsModelMapper
import net.svaroh.passly.mappers.RbacMapper
import net.svaroh.passly.mappers.ResourceFormMapper
import net.svaroh.passly.mappers.ResourceModelMapper
import net.svaroh.passly.mappers.ResourcePickerMapper
import net.svaroh.passly.mappers.ResourceTypesModelMapper
import net.svaroh.passly.mappers.SharePermissionsModelMapper
import net.svaroh.passly.mappers.SignInMapper
import net.svaroh.passly.mappers.SignOutMapper
import net.svaroh.passly.mappers.SwitchAccountModelMapper
import net.svaroh.passly.mappers.TagsModelMapper
import net.svaroh.passly.mappers.TransferMapper
import net.svaroh.passly.mappers.UserProfileMapper
import net.svaroh.passly.mappers.UsersModelMapper
import net.svaroh.passly.mappers.comparator.SwitchAccountUiModelComparator
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val mappersModule =
    module {
        singleOf(::TransferMapper)
        singleOf(::AccountModelMapper)
        singleOf(::SignInMapper)
        singleOf(::ResourceModelMapper)
        singleOf(::SignOutMapper)
        singleOf(::CreateResourceMapper)
        singleOf(::ResourceTypesModelMapper)
        singleOf(::UsersModelMapper)
        singleOf(::SwitchAccountUiModelComparator)
        singleOf(::SwitchAccountModelMapper)
        singleOf(::UserProfileMapper)
        singleOf(::HomeDisplayViewMapper)
        singleOf(::FolderModelMapper)
        singleOf(::TagsModelMapper)
        singleOf(::GroupsModelMapper)
        singleOf(::PermissionsModelMapper)
        singleOf(::SharePermissionsModelMapper)
        singleOf(::OtpModelMapper)
        singleOf(::ResourcePickerMapper)
        singleOf(::RbacMapper)
        singleOf(::PasswordExpiryMapper)
        singleOf(::PasswordPoliciesMapper)
        singleOf(::MetadataMapper)
        singleOf(::EntropyViewMapper)
        singleOf(::ResourceFormMapper)
    }
