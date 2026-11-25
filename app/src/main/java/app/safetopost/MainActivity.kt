package app.safetopost

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import app.safetopost.ui.SafeToPostTheme
import app.safetopost.ui.ScanScreen

class MainActivity : ComponentActivity() {

    private val viewModel: ScanViewModel by viewModels()
    private val requestMediaLocation = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestMediaLocation.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
        }
        setContent {
            SafeToPostTheme {
                ScanScreen(viewModel)
            }
        }
    }
}
