package net.svaroh.passly.feature.main.mainscreen

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import net.svaroh.passly.core.security.runtimeauth.RuntimeAuthenticatedFlag
import org.koin.android.ext.android.inject

// NOTE: When changing name or package read core/navigation/README.md
class MainActivity : AppCompatActivity() {
    private val runtimeAuthenticatedFlag: RuntimeAuthenticatedFlag by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        runtimeAuthenticatedFlag.require(this)
        setContent { MainScreen() }
    }
}
