package edu.uco.dwilliams94.indoorlocalization

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_test.*
import java.io.File


class TestActivity : AppCompatActivity() {

    private val TAG = "AndroidCameraApi"
    private val takePictureButton: Button? = null
    private val textureView: TextureView? = null
    private val ORIENTATIONS = SparseIntArray()
    private val cameraId: String? = null
    protected var cameraDevice: CameraDevice? = null
    protected var cameraCaptureSessions: CameraCaptureSession? = null
    protected var captureRequest: CaptureRequest? = null
    protected var captureRequestBuilder: CaptureRequest.Builder? = null
    private val imageDimension: Size? = null
    private val imageReader: ImageReader? = null
    private val file: File? = null
    private val REQUEST_CAMERA_PERMISSION = 200
    private val mFlashSupported: Boolean = false
    private val mBackgroundHandler: Handler? = null
    private val mBackgroundThread: HandlerThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);

        texture.surfaceTextureListener
    }

}
