package net.svaroh.passly.feature.settings.screen

import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.Module

fun Module.settingsModule() {
    viewModelOf(::SettingsViewModel)
}
