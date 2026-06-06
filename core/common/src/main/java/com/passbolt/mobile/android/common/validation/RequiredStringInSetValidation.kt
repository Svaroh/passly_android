package net.svaroh.passly.common.validation

class RequiredStringInSetValidation : (String, Set<String>) -> Boolean {
    override fun invoke(
        field: String,
        allowedValues: Set<String>,
    ) = field in allowedValues
}
