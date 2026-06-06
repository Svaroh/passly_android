package net.svaroh.passly.gopenpgp.exception

class GopenPgpExceptionParser {
    fun parseGopenPgpException(exception: Exception): OpenPgpFailure {
        // TODO decide if detecting error types based on String error from Go library needs implementation
        return OpenPgpFailure.Generic(OpenPgpError(exception.message.orEmpty()))
    }
}
