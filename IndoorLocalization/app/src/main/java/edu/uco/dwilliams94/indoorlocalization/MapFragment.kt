package edu.uco.dwilliams94.indoorlocalization

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_map.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView



// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [MapFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [MapFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class MapFragment : Fragment(), SensorEventListener {

    private lateinit var map : MapView

    private lateinit var sensorManager: ILSensorManager

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null

    private lateinit var container: ConstraintLayout

    private lateinit var updateThread : Thread
    private var updateNav = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }


        var ctx = activity!!.applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        val senMan = activity!!.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        sensorManager = ILSensorManager(senMan, null, false)

        val task = Runnable {
            if(map_nav != null)
                map_nav.rotation = sensorManager.compassRotation
            if(turning != null) {
                if (sensorManager.turning)
                    turning.visibility = View.VISIBLE
                else {
                    turning.visibility = View.INVISIBLE
                }
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

    override fun onResume() {
        super.onResume()
        map.onResume()
        sensorManager.onResume()
        updateNav = true

    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        sensorManager.onPause()
        updateNav = false
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view as ConstraintLayout

        map = container.findViewById<MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(true)
        map.setMultiTouchControls(true)

        val mapController = map.controller
        mapController.setZoom(19.5)
        val startPoint = GeoPoint(35.654259, -97.470482)
        mapController.setCenter(startPoint)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false)


    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MapFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MapFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        map_nav.rotation = sensorManager.compassRotation
        System.out.println("TESTESTSTETSETSETSETSETESTSETS")
        System.out.println(sensorManager.compassRotation)
        Log.d("TESTETSETSET","TESTESTETSETESTSETSET")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
