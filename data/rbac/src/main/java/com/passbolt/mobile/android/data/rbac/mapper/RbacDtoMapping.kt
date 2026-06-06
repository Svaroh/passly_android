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

package net.svaroh.passly.data.rbac.mapper

import net.svaroh.passly.domain.rbac.model.Rbac
import net.svaroh.passly.domain.rbac.model.RbacRule
import net.svaroh.passly.domain.rbac.model.RbacRule.ALLOW
import net.svaroh.passly.domain.rbac.model.RbacRule.DENY
import net.svaroh.passly.domain.rbac.model.RbacRule.UNSUPPORTED_RULE
import net.svaroh.passly.dto.response.RbacPermissionDto

fun List<RbacPermissionDto>.toDomain(): Rbac =
    Rbac(
        passwordCopyRule = findRuleOrDefault(COPY_PASSWORD_RULE, ALLOW),
        passwordPreviewRule = findRuleOrDefault(PREVIEW_PASSWORD_RULE, ALLOW),
        tagsUseRule = findRuleOrDefault(USE_TAGS_RULE, ALLOW),
        foldersUseRule = findRuleOrDefault(USE_FOLDERS_RULE, ALLOW),
        shareViewRule = findRuleOrDefault(VIEW_SHARE_RULE, ALLOW),
    )

private fun List<RbacPermissionDto>.findRuleOrDefault(
    ruleName: String,
    defaultRule: RbacRule,
): RbacRule = find { it.uiAction?.name == ruleName }?.controlFunction.toRbacRule() ?: defaultRule

private fun String?.toRbacRule(): RbacRule? =
    when (this) {
        "Allow" -> ALLOW
        "Deny" -> DENY
        null -> null
        else -> UNSUPPORTED_RULE
    }

private const val PREVIEW_PASSWORD_RULE = "Secrets.preview"
private const val COPY_PASSWORD_RULE = "Secrets.copy"
private const val USE_TAGS_RULE = "Tags.use"
private const val USE_FOLDERS_RULE = "Folders.use"
private const val VIEW_SHARE_RULE = "Share.viewList"
