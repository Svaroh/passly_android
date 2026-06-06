package net.svaroh.passly.feature.home.screen.snackbar

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import net.svaroh.passly.core.localization.R as LocalizationR

@Composable
fun AutofillConflictSnackbarEffect(
    snackbarHostState: SnackbarHostState,
    isAutofillConflictDetected: Boolean,
    onActionClick: () -> Unit,
) {
    val message = stringResource(LocalizationR.string.autofill_conflict_message)
    val actionLabel = stringResource(LocalizationR.string.settings)
    val currentOnActionClick by rememberUpdatedState(onActionClick)

    LaunchedEffect(isAutofillConflictDetected) {
        if (!isAutofillConflictDetected) {
            return@LaunchedEffect
        }

        val result =
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = false,
                duration = SnackbarDuration.Long,
            )

        if (result == SnackbarResult.ActionPerformed) {
            currentOnActionClick()
        }
    }
}
