package net.svaroh.passly.feature.resourceform.additionalsecrets.password

import net.svaroh.passly.ui.PasswordStrength
import net.svaroh.passly.ui.ResourceFormMode

internal data class PasswordFormState(
    val resourceFormMode: ResourceFormMode? = null,
    val password: String = "",
    val passwordStrength: PasswordStrength = PasswordStrength.Empty,
    val entropy: Double = 0.0,
    val mainUri: String = "",
    val username: String = "",
)
