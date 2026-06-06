/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package net.svaroh.passly.feature.resourceform.additionalsecrets.note

import net.svaroh.passly.common.validation.StringMaxLength
import net.svaroh.passly.common.validation.validation
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.feature.resourceform.additionalsecrets.note.NoteFormIntent.ApplyChanges
import net.svaroh.passly.feature.resourceform.additionalsecrets.note.NoteFormIntent.GoBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.note.NoteFormIntent.NoteTextChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.note.NoteFormIntent.RemoveNote
import net.svaroh.passly.feature.resourceform.additionalsecrets.note.NoteFormSideEffect.ApplyAndGoBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.note.NoteFormSideEffect.NavigateBack
import net.svaroh.passly.ui.ResourceFormMode

internal class NoteFormViewModel(
    mode: ResourceFormMode,
    note: String,
) : SideEffectViewModel<NoteFormState, NoteFormSideEffect>(
        initialState =
            NoteFormState(
                resourceFormMode = mode,
                note = note,
            ),
    ) {
    fun onIntent(intent: NoteFormIntent) {
        when (intent) {
            is NoteTextChanged -> updateViewState { copy(note = intent.note) }
            GoBack -> emitSideEffect(NavigateBack)
            ApplyChanges -> applyChanges()
            RemoveNote -> emitSideEffect(ApplyAndGoBack(null))
        }
    }

    private fun applyChanges() {
        updateViewState { copy(noteValidationErrors = emptyList()) }
        val note = viewState.value.note
        validation {
            of(note) {
                withRules(StringMaxLength(NOTE_MAX_LENGTH)) {
                    onInvalid {
                        updateViewState {
                            copy(
                                noteValidationErrors = noteValidationErrors + NoteValidationError.MaxLengthExceeded(NOTE_MAX_LENGTH),
                            )
                        }
                    }
                }
            }
            onValid { emitSideEffect(ApplyAndGoBack(note)) }
        }
    }

    companion object {
        const val NOTE_MAX_LENGTH = 50_000
    }
}
