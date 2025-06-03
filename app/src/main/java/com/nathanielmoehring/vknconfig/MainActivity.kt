package com.nathanielmoehring.vknconfig

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class MainActivity : AppCompatActivity() {
    private val TAG = "VknMainActivity"
    // Using a dedicated thread for rendering is still common for native graphics
    private var renderThread: Thread? = null
    private var D_renderingActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NativeBridge.nativeInit() // Initialize VknApp early
        setContent {
            MaterialTheme { // Or your app's theme
                Surface(modifier = Modifier.fillMaxSize()) {
                    VulkanTesterScreen()
                }
            }
        }
    }

    /**
     * A native method that is implemented by the 'vknconfig' native library,
     * which is packaged with this application.
     */
    //external fun stringFromJNI(): String

    companion object {
        // Used to load the 'vknconfig' library on application startup.
        init {
            System.loadLibrary("vknconfig")
        }
    }

    @Composable
    fun VulkanTesterScreen() {
        val context = LocalContext.current
        var surfaceViewInstance: SurfaceView? by remember { mutableStateOf(null) }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                // AndroidView is used to embed traditional Android Views in Compose
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            surfaceViewInstance = this
                            holder.addCallback(object : SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: SurfaceHolder) {
                                    Log.i(TAG, "Surface created.")
                                    NativeBridge.nativeSetSurface(holder.surface)
                                    startRenderingLoop()
                                }

                                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                                    Log.i(TAG, "Surface changed: ${width}x$height")
                                    // Vulkan handles resizes via swapchain recreation
                                }

                                override fun surfaceDestroyed(holder: SurfaceHolder) {
                                    Log.i(TAG, "Surface destroyed.")
                                    stopRenderingLoop()
                                    NativeBridge.nativeSetSurface(null)
                                    NativeBridge.nativeSurfaceDestroyed()
                                }
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Button(
                onClick = {
                    Toast.makeText(context, "Running tests... Check Logcat.", Toast.LENGTH_SHORT).show()
                    // Using a coroutine for the test execution
                    (context as? ComponentActivity)?.lifecycleScope?.launch {
                        val testResult = withContext(Dispatchers.Default) { // Run on a background thread
                            NativeBridge.nativeRunTests()
                        }
                        if (testResult == 0) {
                            Toast.makeText(context, "Tests PASSED!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Tests FAILED! Check Logcat.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text("Run VknConfig Tests")
            }
        }

        // Handle lifecycle events for pausing/resuming rendering
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        Log.i(TAG, "Lifecycle: ON_RESUME")
                        // Surface might be created before onResume or after.
                        // Rendering loop is started in surfaceCreated.
                        // If surface already exists and is valid, ensure rendering starts.
                        surfaceViewInstance?.holder?.surface?.let {
                            if (it.isValid) {
                                Log.i(TAG, "Surface valid onResume, ensuring render loop starts if not already.")
                                NativeBridge.nativeSetSurface(it) // Re-set surface just in case
                                startRenderingLoop()
                            }
                        }
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        Log.i(TAG, "Lifecycle: ON_PAUSE")
                        stopRenderingLoop()
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        Log.i(TAG, "Lifecycle: ON_DESTROY")
                        // stopRenderingLoop() // Should be called by onPause
                        NativeBridge.nativeCleanup()
                    }
                    else -> {} // Ignore other events
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    private fun startRenderingLoop() {
        if (D_renderingActive || renderThread?.isAlive == true) {
            Log.i(TAG, "Render loop already active or thread alive.")
            return
        }

        D_renderingActive = true
        renderThread = Thread {
            Log.i(TAG, "Render thread started.")
            try {
                while (D_renderingActive && !Thread.currentThread().isInterrupted) {
                    // The native side (VknApp::cycleEngine) should handle cases
                    // where the surface might not be ready or minimized.
                    // The D_renderingActive flag is the primary control from the Activity.
                    NativeBridge.nativeRender()

                    // Minimal sleep to yield if not vsync paced by Vulkan.
                    // Adjust or remove if Vulkan's presentation engine handles pacing.
                    Thread.sleep(1)
                }
            } catch (e: InterruptedException) {
                Log.i(TAG, "Render thread interrupted.")
                Thread.currentThread().interrupt() // Preserve interrupt status
            } finally {
                D_renderingActive = false // Ensure flag is reset if loop exits
                Log.i(TAG, "Render thread finished.")
            }
        }.apply {
            name = "VulkanRenderThread" // Good practice to name threads
            start()
        }
    }

    private fun stopRenderingLoop() {
        if (!D_renderingActive && renderThread?.isAlive != true) {
            Log.i(TAG, "Render loop already stopped or thread not alive.")
            return
        }
        Log.i(TAG, "Stopping render loop...")
        D_renderingActive = false
        renderThread?.interrupt() // Signal the thread to stop
        try {
            renderThread?.join(500) // Wait for the thread to finish
            if (renderThread?.isAlive == true) {
                Log.w(TAG, "Render thread did not finish in time after interrupt and join.")
            } else {
                Log.i(TAG, "Render thread joined successfully.")
            }
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted while joining render thread", e)
            Thread.currentThread().interrupt() // Preserve interrupt status
        }
        renderThread = null
    }
}
