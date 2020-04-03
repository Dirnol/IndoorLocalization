package edu.uco.dwilliams94.indoorlocalization

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity(){

    private lateinit var sensorManager: ILSensorManager

    private val MULTIPLE_PERMISSIONS = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this)
            return
        }

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED){
            val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE )
            requestPermissions(permissions, MULTIPLE_PERMISSIONS)
        }

        sensorManager = ILSensorManager(getSystemService(Context.SENSOR_SERVICE) as SensorManager, this, true)

        cameraButton.setOnClickListener{
            val intent = Intent(this, NavigationActivity::class.java)
            startActivity(intent)
        }

        testButton.setOnClickListener{
            val intent = Intent(this, TestActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                .show()
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        }

        recreate()
    }

    override fun onResume() {
        super.onResume()
        sensorManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.onPause()
    }

    companion object {

        /** Use external media if it is available, our app's file directory otherwise */
        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, "IndoorLocalization").apply { mkdirs() } }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }
    }

}

/** Helper to ask camera permission.  */
object CameraPermissionHelper {
    private const val CAMERA_PERMISSION_CODE = 0
    private const val CAMERA_PERMISSION = android.Manifest.permission.CAMERA

    /** Check to see we have the necessary permissions for this app.  */
    fun hasCameraPermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(activity, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    /** Check to see we have the necessary permissions for this app, and ask for them if we don't.  */
    fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSION_CODE)
    }

    /** Check to see if we need to show the rationale for this permission.  */
    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, CAMERA_PERMISSION)
    }

    /** Launch Application Setting to grant permission.  */
    fun launchPermissionSettings(activity: Activity) {
        val intent = Intent()
        intent.action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.data = Uri.fromParts("package", activity.packageName, null)
        activity.startActivity(intent)
    }
}