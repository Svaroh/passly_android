package net.svaroh.passly.jsonmodel

interface JsonModel {
    var json: String?

    fun parsed(parse: (String?) -> ParsedJson): ParsedJson = parse(json)

    fun store(parsed: ParsedJson) {
        json = parsed.sourceJson
    }
}
