package net.svaroh.passly.ui

import com.google.gson.Gson
import net.svaroh.passly.jsonmodel.JSON_MODEL_GSON
import net.svaroh.passly.jsonmodel.JsonModel
import net.svaroh.passly.jsonmodel.jsonpathops.JsonPathsOps
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class RootRelativeJsonPathIconDelegate(
    private val jsonPath: String,
) : ReadWriteProperty<JsonModel, MetadataIconModel?>,
    KoinComponent {
    private val gson: Gson by inject(named(JSON_MODEL_GSON))
    private val jsonPathsOps: JsonPathsOps by inject()

    override fun getValue(
        thisRef: JsonModel,
        property: KProperty<*>,
    ): MetadataIconModel? =
        jsonPathsOps.readOrNull(thisRef) { "$.$jsonPath" }?.let {
            gson.fromJson(it, MetadataIconModel::class.java)
        }

    override fun setValue(
        thisRef: JsonModel,
        property: KProperty<*>,
        value: MetadataIconModel?,
    ) {
        if (value == null) {
            // if icon is null remove the property from json
            jsonPathsOps.delete(thisRef) { "$.$jsonPath" }
        } else {
            jsonPathsOps.setOrCreate(thisRef, { jsonPath }, gson.toJsonTree(value))
        }
    }
}
