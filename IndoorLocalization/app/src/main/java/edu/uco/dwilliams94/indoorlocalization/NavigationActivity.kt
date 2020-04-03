package edu.uco.dwilliams94.indoorlocalization

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.activity_navigation.*


private const val IMMERSIVE_FLAG_TIMEOUT = 500L

class NavigationActivity : SensorEventListener, AppCompatActivity() {

    private lateinit var sensorManager: ILSensorManager

    var state = "MAP"

    private lateinit var camFragment: ConstraintLayout
    private lateinit var mapFragment: ConstraintLayout
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)
        camFragment = findViewById(R.id.camera_fragment)
        mapFragment = findViewById(R.id.map_fragment)

        show_camera_button.setOnClickListener {
            showCamera()
        }

        back_button.setOnClickListener {
            showMap()
        }

        sensorManager = ILSensorManager(getSystemService(Context.SENSOR_SERVICE) as SensorManager, this, false)
    }

    fun showMap(){
        state = "MAP"
        var camLayoutParams = camFragment.layoutParams
        camLayoutParams.height = 0
        camLayoutParams.width = 0
        camFragment.layoutParams = camLayoutParams

        var mapLayoutParams = mapFragment.layoutParams
        mapLayoutParams.height = ConstraintLayout.LayoutParams.MATCH_PARENT
        mapLayoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT
        mapFragment.layoutParams = mapLayoutParams

        back_button.hide()
        show_camera_button.show()
    }

    fun showCamera(){
        state = "CAMERA"

        var mapLayoutParams = mapFragment.layoutParams
        mapLayoutParams.height = 0
        mapLayoutParams.width = 0
        mapFragment.layoutParams = mapLayoutParams

        var layoutParams = camFragment.layoutParams
        layoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT
        layoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT
        camFragment.layoutParams = layoutParams

        show_camera_button.hide()
        back_button.show()
    }

    override fun onResume() {
        super.onResume()
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        camFragment.postDelayed({
            camFragment.systemUiVisibility = FLAGS_FULLSCREEN
        }, IMMERSIVE_FLAG_TIMEOUT)

        if(state == "MAP"){
            showMap()
        }else if(state == "CAMERA"){
            showCamera()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        val rotation = sensorManager.compassRotation

    }

}
