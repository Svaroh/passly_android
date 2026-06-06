package net.svaroh.passly.feature.autofill.resources.datasetstrategy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import net.svaroh.passly.core.autofill.system.AssistStructureParser
import net.svaroh.passly.core.autofill.system.FillableInputsFinder
import net.svaroh.passly.feature.autofill.autofill.RemoteViewsFactory

class ReturnAutofillDataset(
    private val autofillCallback: AutofillCallback,
    private val appContext: Context,
    private val assistStructureParser: AssistStructureParser,
    private val fillableInputsFinder: FillableInputsFinder,
    private val remoteViewsFactory: RemoteViewsFactory,
) : ReturnAutofillDatasetStrategy {
    override fun returnDataset(
        username: String,
        password: String,
        uri: String?,
    ) {
        val structure = autofillCallback.getAutofillStructure()
        val parsedStructures = assistStructureParser.parse(structure)

        val usernameParsedAssistStructure =
            fillableInputsFinder.findStructureForAutofillFields(
                net.svaroh.passly.core.autofill.system.AutofillField.USERNAME,
                parsedStructures.structures,
            )
        val passwordParsedAssistStructure =
            fillableInputsFinder.findStructureForAutofillFields(
                net.svaroh.passly.core.autofill.system.AutofillField.PASSWORD,
                parsedStructures.structures,
            )

        val fillResponse =
            FillResponse
                .Builder()
                .addDataset(
                    Dataset
                        .Builder()
                        .apply {
                            addDatasetValue(usernameParsedAssistStructure?.id, username)
                            addDatasetValue(passwordParsedAssistStructure?.id, password)
                        }.build(),
                ).build()

        val replyIntent =
            Intent().apply {
                putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, fillResponse)
            }

        autofillCallback.setResultAndFinish(Activity.RESULT_OK, replyIntent)
    }

    private fun Dataset.Builder.addDatasetValue(
        id: AutofillId?,
        valueText: String,
    ) {
        if (id != null) {
            setValue(
                id,
                AutofillValue.forText(valueText),
                remoteViewsFactory.getAutofillFillDropdown(appContext.packageName),
            )
        }
    }
}
