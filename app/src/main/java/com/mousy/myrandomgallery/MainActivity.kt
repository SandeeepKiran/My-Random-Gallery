package com.mousy.myrandomgallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mousy.myrandomgallery.data.media.MediaPermissions
import com.mousy.myrandomgallery.ui.GalleryApp
import com.mousy.myrandomgallery.ui.theme.MyRandomGalleryTheme
import com.mousy.myrandomgallery.util.AndroidVersionGate
import com.mousy.myrandomgallery.viewmodel.GalleryViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GalleryViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.any { it }) {
            viewModel.onPermissionsGranted()
        }
    }

    /** DEVICE-ONLY: Photo Picker for partial access on Android 13+. */
    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) {
        viewModel.onPermissionsGranted()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestMediaAccessIfNeeded()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            MyRandomGalleryTheme(
                themeMode = settings.themeMode,
                amoled = settings.amoled,
                accent = settings.accent,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    GalleryApp(
                        viewModel = viewModel,
                        onRequestOrientation = { orientation ->
                            // DEVICE-ONLY: Multi-Video landscape lock, or sensor for viewer video
                            requestedOrientation = orientation
                        },
                    )
                }
            }
        }
    }

    private fun requestMediaAccessIfNeeded() {
        if (MediaPermissions.hasReadAccess(this)) {
            viewModel.onPermissionsGranted()
            return
        }

        if (AndroidVersionGate.isAndroid16Plus && MediaPermissions.shouldOfferPhotoPicker(this)) {
            photoPickerLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageAndVideo,
                ),
            )
            return
        }

        permissionLauncher.launch(MediaPermissions.permissionsToRequest())
    }
}
