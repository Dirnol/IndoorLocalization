package edu.uco.dwilliams94.indoorlocalization


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageButton
import androidx.camera.core.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*

private const val PHOTO_EXTENSION = ".jpg"

/**
 * Camera [Fragment] subclass.
 */
class CameraFragment : Fragment() {

    private lateinit var container: ConstraintLayout
    private lateinit var viewFinder: TextureView
    private lateinit var outputDirectory: File
    private lateinit var broadcastManager: LocalBroadcastManager

    private var displayId = -1
    private var lensFacing = CameraX.LensFacing.BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null

    var handler = Handler()
    lateinit var runnable: Runnable
    val delay = 10*1000L

    private lateinit var sensorManager: ILSensorManager

    private lateinit var updateThread : Thread
    private var updateNav = true

    private var wasTurning = false

    override fun onResume() {

        sensorManager.onResume()
        updateNav = true

        runnable = Runnable {
            capture()
            handler.postDelayed(runnable,delay)
        }

        handler.postDelayed( runnable, delay);

        super.onResume()
    }

    override fun onPause() {
        sensorManager.onPause()
        updateNav = false
        handler.removeCallbacks(runnable)
        super.onPause()
    }


    /** Internal reference of the [DisplayManager] */
    private lateinit var displayManager: DisplayManager

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraFragment.displayId) {
                Log.d("ROTATION", "Rotation changed: ${view.display.rotation}")
                preview?.setTargetRotation(view.display.rotation)
                imageCapture?.setTargetRotation(view.display.rotation)
            }
        } ?: Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mark this as a retain fragment, so the lifecycle does not get restarted on config change
        retainInstance = true

        val senMan = activity!!.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager = ILSensorManager(senMan, null, false)

        val task = Runnable {
            if (sensorManager.turning)
                wasTurning = true
            else {
                if(wasTurning) {
                    Log.d("FINISHED TURN","TAKING PICTURE")
                    capture()
                }
                wasTurning = false
            }
        }

        updateThread = object : Thread() {
            override fun run() {
                try {
                    while (updateNav) {
                        sleep(1)
                        activity!!.runOnUiThread(task)
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        }
        updateThread.start()

    }

    private fun capture(){

        Log.d("CAPTURE", "TAKING PICTURE!!!")

        // Remove previous UI if any
        container.findViewById<ConstraintLayout>(R.id.camera_ui_container)?.let {
            container.removeView(it)
        }

        // Inflate a new view containing all UI for controlling the camera
        val controls = View.inflate(requireContext(), R.layout.camera_ui_container, container)

        // Get a stable reference of the modifiable image capture use case
        imageCapture?.let { imageCapture ->

            // Create output file to hold the image
            val photoFile = createFile(outputDirectory, "yyyy-MM-dd'T'HH:mm:ss", PHOTO_EXTENSION)

            evaluateLocation(editHost.text.toString(), editPort.text.toString().toInt(),
                photoFile)

            // Setup image capture metadata
            val metadata = ImageCapture.Metadata().apply {
                // Mirror image when using the front camera
                isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
            }

            // Setup image capture listener which is triggered after photo has been taken
            imageCapture.takePicture(photoFile, imageSavedListener, metadata)

            // We can only change the foreground Drawable using API level 23+ API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                // Display flash animation to indicate that photo was captured
                container.postDelayed({
                    container.foreground = ColorDrawable(Color.WHITE)
                    container.postDelayed(
                        { container.foreground = null }, ANIMATION_FAST_MILLIS
                    )
                }, ANIMATION_SLOW_MILLIS)
            }
        }
    }

    fun evaluateLocation(host: String, port: Int, imageFile: File) {

        val task = Runnable {
            try {
                val socket = Socket(host, port)
                socket.sendBufferSize = 1024
                val bos = BufferedOutputStream(socket.getOutputStream())
                val image = FileInputStream(imageFile)
                val bytes = ByteArray(1024)
                var len = image.read(bytes)
                while (len != -1) {
                    bos.write(bytes, 0, len)
                    len = image.read(bytes)
                }
                bos.flush()
                if (!socket.isOutputShutdown)
                    socket.shutdownOutput()
                val resultStream = socket.getInputStream()
                val result = StringBuilder()
                len = resultStream.read()
                while (len != -1) {
                    result.append(len.toChar())
                    len = resultStream.read()
                }
                bos.close()
                socket.close()
                textLocation.text = "Location: " + result.toString()
            } catch (e: Exception) {
                System.out.println(e)
            }
        }

        updateThread = object : Thread() {
            override fun run() {
                try {
                    activity!!.runOnUiThread(task)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        }
        updateThread.start()
    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes */
    @SuppressLint("RestrictedApi")
    private fun updateCameraUi() {

        // Remove previous UI if any
        container.findViewById<ConstraintLayout>(R.id.camera_ui_container)?.let {
            container.removeView(it)
        }

        // Inflate a new view containing all UI for controlling the camera
        val controls = View.inflate(requireContext(), R.layout.camera_ui_container, container)

        // Listener for button used to capture photo
        controls.findViewById<ImageButton>(R.id.fab_camera).setOnClickListener {
            // Get a stable reference of the modifiable image capture use case
            imageCapture?.let { imageCapture ->

                // Create output file to hold the image
                val photoFile = createFile(outputDirectory, "yyyy-MM-dd'T'HH:mm:ss", PHOTO_EXTENSION)

                // Setup image capture metadata
                val metadata = ImageCapture.Metadata().apply {
                    // Mirror image when using the front camera
                    isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
                }

                // Setup image capture listener which is triggered after photo has been taken
                imageCapture.takePicture(photoFile, imageSavedListener, metadata)

                // We can only change the foreground Drawable using API level 23+ API
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    // Display flash animation to indicate that photo was captured
                    container.postDelayed({
                        container.foreground = ColorDrawable(Color.WHITE)
                        container.postDelayed(
                            { container.foreground = null }, ANIMATION_FAST_MILLIS)
                    }, ANIMATION_SLOW_MILLIS)
                }
            }
        }
    }

    /** Define callback that will be triggered after a photo has been taken and saved to disk */
    private val imageSavedListener = object : ImageCapture.OnImageSavedListener {

        override fun onError(
            useCaseError: ImageCapture.UseCaseError,
            message: String,
            exc: Throwable?
        ) {
            Log.e("FAILURE", "Photo capture failed: $message")
            exc?.printStackTrace()
        }

        override fun onImageSaved(photoFile: File) {
            Log.d("SUCCESS", "Photo capture succeeded: ${photoFile.absolutePath}")


            // If the folder selected is an external media directory, this is unnecessary
            // but otherwise other apps will not be able to access our images unless we
            // scan them using [MediaScannerConnection]
            val mimeType = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(photoFile.extension)
            MediaScannerConnection.scanFile(
                context, arrayOf(photoFile.absolutePath), arrayOf(mimeType), null)
        }
    }

    /** Helper function used to create a timestamped file */
    private fun createFile(baseFolder: File, format: String, extension: String) =
        File(baseFolder, SimpleDateFormat(format, Locale.US)
            .format(System.currentTimeMillis()) + extension)

    override fun onDestroyView() {
        super.onDestroyView()

        // Unregister the broadcast receivers and listeners
        displayManager.unregisterDisplayListener(displayListener)
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view as ConstraintLayout
        viewFinder = container.findViewById(R.id.view_finder)
        broadcastManager = LocalBroadcastManager.getInstance(view.context)

        // Every time the orientation of device changes, recompute layout
        displayManager = viewFinder.context
            .getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)

        // Determine the output directory
        outputDirectory = MainActivity.getOutputDirectory(requireContext())

        // Wait for the views to be properly laid out
        viewFinder.post {
            // Keep track of the display in which this view is attached
            displayId = viewFinder.display.displayId

            bindCameraUseCases()
            updateCameraUi()

        }
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)
        Log.d("SCREEN_METRICS", "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        // Set up the view finder use case to display camera preview
        val viewFinderConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            // We request aspect ratio but no resolution to let CameraX optimize our use cases
            setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        // Use the auto-fit preview builder to automatically handle size and orientation changes
        preview = AutoFitPreviewBuilder.build(viewFinderConfig, viewFinder)

        // Set up the capture use case to allow users to take photos
        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setLensFacing(lensFacing)
            setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            // We request aspect ratio but no resolution to match preview config but letting
            // CameraX optimize for whatever specific resolution best fits requested capture mode
            setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        imageCapture = ImageCapture(imageCaptureConfig)

        // Apply declared configs to CameraX using the same lifecycle owner
        CameraX.bindToLifecycle(
            viewLifecycleOwner, preview, imageCapture)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }


}
