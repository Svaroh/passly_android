package net.svaroh.passly.common.autofill

fun interface DetectAutofillConflict {
    operator fun invoke(): Boolean
}
