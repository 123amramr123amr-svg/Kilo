package com.personalaiagent

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalaiagent.platform.AppLocale
import com.personalaiagent.platform.PrefsLocaleMirror
import com.personalaiagent.ui.navigation.AppNavHost
import com.personalaiagent.ui.navigation.MainViewModel
import com.personalaiagent.ui.theme.PersonalAiAgentTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        val tag = PrefsLocaleMirror(PrefsLocaleMirror.preferences(newBase)).read()
        super.attachBaseContext(AppLocale.wrap(newBase, tag))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Locale that the current resources were loaded with; used to know when a
        // language change requires an Activity recreation.
        val attachedLocale = PrefsLocaleMirror(PrefsLocaleMirror.preferences(this)).read()

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(state.isLoading, state.settings.language) {
                if (!state.isLoading && state.settings.language.tag != attachedLocale) {
                    // Mirror first: the recreated Activity reads it in attachBaseContext,
                    // which guarantees one recreation instead of a loop.
                    PrefsLocaleMirror(PrefsLocaleMirror.preferences(this@MainActivity))
                        .write(state.settings.language.tag)
                    recreate()
                }
            }

            if (state.isLoading) {
                // Splash screen stays visible until settings are read.
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
            } else {
                val rtl = state.settings.language.isRtl
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    PersonalAiAgentTheme(themeMode = state.settings.theme) {
                        AppNavHost(settings = state.settings)
                    }
                }
            }
        }
    }
}
