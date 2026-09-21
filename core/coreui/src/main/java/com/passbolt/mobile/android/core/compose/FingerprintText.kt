package com.passbolt.mobile.android.core.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.passbolt.mobile.android.core.formatter.FingerprintFormatter

@Composable
fun FingerprintText(
    fingerprint: String,
    fingerprintFormatter: FingerprintFormatter,
    modifier: Modifier = Modifier,
) {
    Text(
        text =
            fingerprintFormatter
                .formatWithRawFallback(fingerprint, appendMiddleSpacing = true)
                .uppercase(),
        style = FingerprintTextStyle,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun FingerprintTextLightPreview() {
    PassboltTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            FingerprintText(
                fingerprint = "03F60E958F4CB29DBE4BE4EB3BD91E325CC7D42C",
                fingerprintFormatter = FingerprintFormatter(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FingerprintTextDarkPreview() {
    PassboltTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            FingerprintText(
                fingerprint = "03F60E958F4CB29DBE4BE4EB3BD91E325CC7D42C",
                fingerprintFormatter = FingerprintFormatter(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FingerprintTextRawFallbackPreview() {
    PassboltTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            FingerprintText(
                fingerprint = "not-a-valid-40-char-fingerprint",
                fingerprintFormatter = FingerprintFormatter(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
