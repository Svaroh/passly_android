package net.svaroh.passly.feature.otp.scanotp

import net.svaroh.passly.core.qrscan.CameraInformationProvider
import net.svaroh.passly.core.qrscan.analyzer.BarcodeScanResult
import net.svaroh.passly.feature.otp.scanotp.compose.ScanOtpViewModel
import net.svaroh.passly.feature.otp.scanotp.parser.OtpQrParser
import net.svaroh.passly.ui.OtpParseResult
import net.svaroh.passly.ui.OtpParseResult.UserResolvableError.ErrorType.NO_BARCODES_IN_RANGE
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.mockito.kotlin.mock

internal val cameraInformationProvider = mock<CameraInformationProvider>()
internal val qrParser = mock<OtpQrParser>()
internal val scanningFlow = MutableStateFlow<BarcodeScanResult>(BarcodeScanResult.NoBarcodeInRange)
internal val parseFlow = MutableStateFlow<OtpParseResult>(OtpParseResult.UserResolvableError(NO_BARCODES_IN_RANGE))

val scanOtpTestModule =
    module {
        single { cameraInformationProvider }
        single { qrParser }
        factoryOf(::ScanOtpViewModel)
    }
