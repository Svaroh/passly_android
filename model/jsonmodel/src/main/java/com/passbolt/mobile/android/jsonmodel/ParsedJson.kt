package net.svaroh.passly.jsonmodel

import com.jayway.jsonpath.DocumentContext

class ParsedJson internal constructor(
    internal val sourceJson: String?,
    internal val context: DocumentContext,
)
