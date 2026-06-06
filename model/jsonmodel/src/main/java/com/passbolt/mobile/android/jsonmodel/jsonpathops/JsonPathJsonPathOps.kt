package net.svaroh.passly.jsonmodel.jsonpathops

import com.google.gson.JsonElement
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import net.svaroh.passly.jsonmodel.JsonModel
import net.svaroh.passly.jsonmodel.ParsedJson

class JsonPathJsonPathOps(
    jsonPathConfig: Configuration,
) : JsonPathsOps {
    private val parseContext = JsonPath.using(jsonPathConfig)

    override fun read(
        model: JsonModel,
        pathProvider: () -> String,
    ): JsonElement = context(model).read(pathProvider())

    override fun readOrNull(
        model: JsonModel,
        pathProvider: () -> String,
    ): JsonElement? =
        try {
            val value = context(model).read<JsonElement>(pathProvider())
            if (value.isJsonNull) null else value
        } catch (e: Exception) {
            null
        }

    override fun setOrCreate(
        model: JsonModel,
        pathProvider: () -> String,
        value: JsonElement,
    ) {
        val document = context(model)
        try {
            document.set(pathProvider(), value)
        } catch (exception: PathNotFoundException) {
            document.put(ROOT_PATH, pathProvider(), value)
        }
        commit(model, document)
    }

    override fun delete(
        model: JsonModel,
        pathProvider: () -> String,
    ) {
        val document = context(model).delete(pathProvider())
        commit(model, document)
    }

    private fun context(model: JsonModel): DocumentContext = model.parsed { ParsedJson(it, parseContext.parse(it)) }.context

    private fun commit(
        model: JsonModel,
        document: DocumentContext,
    ) {
        model.store(ParsedJson(document.jsonString(), document))
    }

    private companion object {
        const val ROOT_PATH = "$"
    }
}
