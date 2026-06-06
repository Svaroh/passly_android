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

package net.svaroh.passly.core.autofill.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.common.ResourceDimenProvider
import net.svaroh.passly.core.autofill.system.AutofillField
import net.svaroh.passly.core.autofill.system.AutofillHintsFactory
import net.svaroh.passly.core.navigation.AutofillType
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AccessibilityOperationsProviderTest {
    private val resourceDimenProvider = mock<ResourceDimenProvider>()
    private val autofillHintsFactory = mock<AutofillHintsFactory>()
    private lateinit var provider: AccessibilityOperationsProvider

    @Before
    fun setUp() {
        whenever(autofillHintsFactory.getHintValues(AutofillField.TOTP))
            .thenReturn(arrayOf("otp", "totp", "one-time-code"))
        whenever(autofillHintsFactory.getHintValues(AutofillField.PASSWORD))
            .thenReturn(arrayOf("password", "passphrase", "pass"))
        whenever(autofillHintsFactory.getHintValues(AutofillField.USERNAME))
            .thenReturn(arrayOf("username", "email", "mail", "login"))

        provider = AccessibilityOperationsProvider(resourceDimenProvider, autofillHintsFactory)
    }

    @Test
    fun `getTotpNode finds node matching totp hint text`() {
        val totpNode = createNode(hintText = "Enter your OTP code", isPassword = false)
        val otherNode = createNode(hintText = "Enter username", isPassword = false)

        val result = provider.getTotpNode(listOf(otherNode, totpNode))

        assertThat(result).isEqualTo(totpNode)
    }

    @Test
    fun `getTotpNode finds node matching totp content description`() {
        val totpNode =
            createNode(
                contentDescription = "totp field",
                isPassword = false,
            )
        val otherNode = createNode(hintText = "password", isPassword = false)

        val result = provider.getTotpNode(listOf(otherNode, totpNode))

        assertThat(result).isEqualTo(totpNode)
    }

    @Test
    fun `getTotpNode finds node matching totp view id`() {
        val totpNode =
            createNode(
                viewIdResourceName = "com.app:id/otp_input",
                isPassword = false,
            )
        val otherNode = createNode(hintText = "email", isPassword = false)

        val result = provider.getTotpNode(listOf(otherNode, totpNode))

        assertThat(result).isEqualTo(totpNode)
    }

    @Test
    fun `getTotpNode skips password nodes`() {
        val passwordNode =
            createNode(
                hintText = "Enter one-time-code",
                isPassword = true,
            )
        val otherNode = createNode(hintText = "username", isPassword = false)

        val result = provider.getTotpNode(listOf(passwordNode, otherNode))

        assertThat(result).isNull()
    }

    @Test
    fun `getTotpNode returns null when no match`() {
        val node1 = createNode(hintText = "username", isPassword = false)
        val node2 = createNode(hintText = "password", isPassword = true)

        val result = provider.getTotpNode(listOf(node1, node2))

        assertThat(result).isNull()
    }

    @Test
    fun `getPasswordNode uses keyword list`() {
        val passphraseNode = createNode(hintText = "Enter passphrase", isPassword = false)
        val otherNode = createNode(hintText = "username", isPassword = false)

        val result = provider.getPasswordNode(listOf(otherNode, passphraseNode))

        assertThat(result).isEqualTo(passphraseNode)
    }

    @Test
    fun `getUsernameNode uses keyword list`() {
        val emailNode = createNode(hintText = "Enter your mail address", isPassword = false)
        val otherNode = createNode(hintText = "something", isPassword = false)

        val result = provider.getUsernameNode(listOf(otherNode, emailNode), null)

        assertThat(result).isEqualTo(emailNode)
    }

    @Test
    fun `matchesKeywords is case insensitive`() {
        val uppercaseNode = createNode(hintText = "Enter your OTP CODE here", isPassword = false)

        val result = provider.getTotpNode(listOf(uppercaseNode))

        assertThat(result).isEqualTo(uppercaseNode)
    }

    @Test
    fun `classifyAutofillType returns CREDENTIALS when only username + password found`() {
        val (root, event) =
            createTree(
                createEditTextNode(hintText = "username", windowId = 1),
                createEditTextNode(hintText = "password", windowId = 1, isPassword = true),
            )
        assertThat(provider.classifyAutofillType(root, event)).isEqualTo(AutofillType.CREDENTIALS)
    }

    @Test
    fun `classifyAutofillType returns TOTP when only totp found`() {
        val (root, event) =
            createTree(
                createEditTextNode(hintText = "otp code", windowId = 1),
            )
        assertThat(provider.classifyAutofillType(root, event)).isEqualTo(AutofillType.TOTP)
    }

    @Test
    fun `classifyAutofillType returns CREDENTIALS_AND_TOTP when username + password + totp found`() {
        val (root, event) =
            createTree(
                createEditTextNode(hintText = "username", windowId = 1),
                createEditTextNode(hintText = "password", windowId = 1, isPassword = true),
                createEditTextNode(hintText = "otp code", windowId = 1),
            )
        assertThat(provider.classifyAutofillType(root, event)).isEqualTo(AutofillType.CREDENTIALS_AND_TOTP)
    }

    @Test
    fun `classifyAutofillType returns null when no fields found`() {
        val (root, event) = createTree()
        assertThat(provider.classifyAutofillType(root, event)).isNull()
    }

    private fun createNode(
        hintText: String? = null,
        contentDescription: String? = null,
        viewIdResourceName: String? = null,
        isPassword: Boolean = false,
    ): AccessibilityNodeInfo =
        mock<AccessibilityNodeInfo> {
            on { this.hintText } doReturn hintText
            on { this.contentDescription } doReturn contentDescription
            on { this.viewIdResourceName } doReturn viewIdResourceName
            on { this.isPassword } doReturn isPassword
        }

    private fun createEditTextNode(
        hintText: String? = null,
        windowId: Int = 1,
        isPassword: Boolean = false,
    ): AccessibilityNodeInfo =
        mock<AccessibilityNodeInfo> {
            on { this.hintText } doReturn hintText
            on { this.contentDescription } doReturn null
            on { this.viewIdResourceName } doReturn null
            on { this.className } doReturn "android.widget.EditText"
            on { this.windowId } doReturn windowId
            on { this.isPassword } doReturn isPassword
            on { this.childCount } doReturn 0
        }

    private fun createTree(vararg children: AccessibilityNodeInfo): Pair<AccessibilityNodeInfo, AccessibilityEvent> {
        val root =
            mock<AccessibilityNodeInfo> {
                on { childCount } doReturn children.size
                on { className } doReturn "android.widget.FrameLayout"
            }
        children.forEachIndexed { i, child -> whenever(root.getChild(i)).thenReturn(child) }
        val event = mock<AccessibilityEvent> { on { this.windowId } doReturn 1 }
        return root to event
    }
}
