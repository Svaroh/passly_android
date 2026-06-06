package net.svaroh.passly.jsonmodel

interface CachedJsonModel : JsonModel {
    var parsedCache: ParsedJson?

    override fun parsed(parse: (String?) -> ParsedJson): ParsedJson {
        parsedCache?.let { if (it.sourceJson === json) return it }
        return parse(json).also { parsedCache = it }
    }

    override fun store(parsed: ParsedJson) {
        json = parsed.sourceJson
        parsedCache = parsed
    }
}
