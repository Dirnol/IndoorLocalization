package edu.uco.dwilliams94.indoorlocalization

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView

class ILSensorManager(senManager: SensorManager, mainActivity: Activity?, isDebug: Boolean) : SensorEventListener {

    private  var sensorManager = senManager
    private var sensor: Sensor? = null

    private var main: Activity? = mainActivity

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var updateHandler: Handler

    private val turnList = ArrayList<Float>()
    var turning = false

    private var debug = isDebug

    var average = 0.0f

    var compassRotation = 0.0f

    init {
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

        // Rotation matrix based on current readings from accelerometer and magnetometer.
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)

        // Express the updated rotation matrix as three orientation angles.
        val orientationAngles = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        updateHandler = Handler(Looper.getMainLooper())
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }

    fun onResume(){
        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                this,
                magneticField,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        updateHandler.post(updateOrientationAngles)
    }

    fun onPause(){

        // Don't receive any more updates from either sensor.
        sensorManager.unregisterListener(this)
        updateHandler.removeCallbacks(updateOrientationAngles)
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }

        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        /*var rotMatString = ""

        for(i in orientationAngles){
            rotMatString += i
            rotMatString += " "
        }

        tv.setText(rotMatString)*/

        turnList.add(orientationAngles[0])

        val angle = -orientationAngles[0]

        if(angle > 0){
            compassRotation = 90.0f + Math.toDegrees(2.0 * Math.PI - angle).toFloat()
        }else{
            compassRotation = 90.0f + Math.abs(Math.toDegrees(angle.toDouble()).toFloat())
        }

        if(debug) {
            var nav_icon: ImageView = main!!.findViewById(R.id.nav_icon) as ImageView

            if(angle > 0){
                nav_icon.rotation = 90.0f + Math.toDegrees(2.0 * Math.PI - angle).toFloat()
            }else{
                nav_icon.rotation = 90.0f + Math.abs(Math.toDegrees(angle.toDouble()).toFloat())
            }


            var tv: TextView = main!!.findViewById(R.id.test) as TextView
            tv.setText(orientationAngles[0].toString())
        }

    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    private val  updateOrientationAngles = object : Runnable {
        // Update rotation matrix, which is needed to update orientation angles.
        override fun run() {

            // "mRotationMatrix" now has up-to-date information.

            var turnString = ""
            average = 0.0f
            var max = -10.0f
            var min = 10.0f
            var range = 0.0f
            for(i in turnList){
                turnString = turnString.plus(i).plus(" \n")
                average += i
                if(i > max)
                    max = i
                if (i < min)
                    min = i
            }
            average /= turnList.size
            range = max - min
            turning = range > 0.2

            if(debug) {

                var tv: TextView = main!!.findViewById(R.id.testAvg)
                tv.text = "Average: " + average.toString()

                var tvMax: TextView = main!!.findViewById(R.id.testMax)
                tvMax.text = "Max: " + max.toString()

                var tvMin: TextView = main!!.findViewById(R.id.testMin)
                tvMin.text = "Min: " + min.toString()

                var tvRange: TextView = main!!.findViewById(R.id.testRange)
                tvRange.text = "Range: " + range.toString()


                var tv2: TextView = main!!.findViewById(R.id.testTop)
                tv2.text = "Turning: " + turning.toString()

                /*var tv2: TextView = main!!.findViewById(R.id.testTop)
                if(turning) {
                    tv2.text = "Turning"
                    tv2.isVisible = true
                }else{
                    tv2.isVisible = false
                }*/

            }
            else{

            }

            turnList.clear()
            // "mOrientationAngles" now has up-to-date information.
            updateHandler.postDelayed(this, 1000)
        }
    }

}