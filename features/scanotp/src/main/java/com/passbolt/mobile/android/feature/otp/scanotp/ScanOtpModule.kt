package net.svaroh.passly.feature.otp.scanotp

import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpViewModel
import net.svaroh.passly.feature.otp.scanotp.parser.OtpQrParser
import net.svaroh.passly.feature.otp.scanotp.parser.OtpQrScanResultsMapper
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf

fun Module.scanOtpModule() {
    factoryOf(::OtpQrParser)
    factoryOf(::OtpQrScanResultsMapper)
    viewModelOf(::ScanOtpViewModel)
}
