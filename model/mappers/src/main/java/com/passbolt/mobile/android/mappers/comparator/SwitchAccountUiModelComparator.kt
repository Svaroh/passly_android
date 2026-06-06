package net.svaroh.passly.mappers.comparator

import net.svaroh.passly.ui.SwitchAccountUiModel

class SwitchAccountUiModelComparator : Comparator<SwitchAccountUiModel> {
    override fun compare(
        current: SwitchAccountUiModel,
        other: SwitchAccountUiModel,
    ) = order.indexOf(current::class.java).compareTo(order.indexOf(other::class.java))

    private companion object {
        private val order =
            listOf(
                SwitchAccountUiModel.HeaderItem::class.java,
                SwitchAccountUiModel.AccountItem::class.java,
                SwitchAccountUiModel.ManageAccountsItem::class.java,
            )
    }
}
