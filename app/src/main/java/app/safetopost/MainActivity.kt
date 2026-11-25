package app.safetopost

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import app.safetopost.ui.SafeToPostTheme
import app.safetopost.ui.ScanScreen

class MainActivity : ComponentActivity() {

    private val viewModel: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SafeToPostTheme {
                ScanScreen(viewModel)
            }
        }
    }
}
