package net.svaroh.passly.jsonmodel.delegates

import net.svaroh.passly.jsonmodel.JsonModel
import org.koin.core.component.KoinComponent
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class NullableStringDelegate :
    ReadWriteProperty<JsonModel, String?>,
    KoinComponent {
    override fun getValue(
        thisRef: JsonModel,
        property: KProperty<*>,
    ): String? = thisRef.json

    override fun setValue(
        thisRef: JsonModel,
        property: KProperty<*>,
        value: String?,
    ) {
        thisRef.json = value
    }
}
