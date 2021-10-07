package com.embed.pashudhan.Fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.embed.pashudhan.Activities.BottomNavigationActivity
import com.embed.pashudhan.Helper
import com.embed.pashudhan.R
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import java.util.*

class LocationFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "LocationFragment==>"
        private val PERMISSION_ID = 42
    }

    private lateinit var locationButton: FloatingActionButton
    private lateinit var locationText: TextView
    private lateinit var rootLayout: LinearLayout
    private lateinit var mUserLatitude: String
    private lateinit var mUserLongitude: String
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mActivity: FragmentActivity
    private lateinit var currentLocation: LatLng
    private lateinit var mSharedPref: SharedPreferences
    private lateinit var newAddressProgressBar: ProgressBar
    private lateinit var mUserUUID: String

    private var helper = Helper()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_location, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(view.context)

        mUserLatitude = mSharedPref.getString(getString(R.string.sp_userLatitude), "0").toString()
        mUserLongitude = mSharedPref.getString(getString(R.string.sp_userLongitude), "0").toString()

        val userCity = mSharedPref.getString(getString(R.string.sp_userDistrict), "0").toString()
        val userState = mSharedPref.getString(getString(R.string.sp_userState), "0").toString()
        val userPostalCode = mSharedPref.getString(getString(R.string.sp_userPinCode), "0").toString()

        mUserUUID = FirebaseAuth.getInstance().currentUser?.phoneNumber!!
        locationButton = view.findViewById(R.id.location_getLocationButton)
        locationText = view.findViewById(R.id.location_getLocationText)
        newAddressProgressBar = view.findViewById(R.id.newAddressLoadingBar)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(view.context)
        if(mUserLatitude == "null" || mUserLatitude == "0" || mUserLongitude == "null" || mUserLongitude == "0") {
            this.isCancelable = false
            locationText.text = getString(R.string.noLocationMessage)
        }else {
            locationText.text = "$userCity, $userState - $userPostalCode"
            currentLocation = LatLng(mUserLatitude.toDouble(), mUserLongitude.toDouble())
        }


        mActivity = requireActivity()
        rootLayout = view.findViewById(R.id.location_bottom_sheet)

        locationButton.setOnClickListener {
            newAddressProgressBar.visibility = View.VISIBLE
            getLastLocation()
        }

    }

    // Get current location
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {

                mFusedLocationClient.lastLocation.addOnCompleteListener(mActivity) { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        currentLocation = LatLng(location.latitude, location.longitude)
                        with(mSharedPref.edit()) {
                            putString(
                                getString(com.embed.pashudhan.R.string.sp_userLatitude),
                                "${location.latitude}"
                            )
                            putString(
                                getString(com.embed.pashudhan.R.string.sp_userLongitude),
                                "${location.longitude}"
                            )
                            apply()
                        }
                        getLocationData(location)
                    }
                }
            } else {
                Toast.makeText(mActivity, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
            newAddressProgressBar.visibility = View.GONE
        }
    }

    private fun getLocationData(location: Location) {
        var geoCoder = Geocoder(mActivity, Locale.getDefault())
        var addresses: List<Address>

        var latitude: Double = location.latitude
        var longitude: Double = location.longitude

        try {
            addresses = geoCoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                var address = addresses.get(0).getAddressLine(0)
                var city = addresses.get(0).locality
                var state = addresses.get(0).adminArea
                var postalCode = addresses.get(0).postalCode

                locationText.text = "$city, $state - $postalCode"
                this.isCancelable = true

                var dataToUpdate = hashMapOf(
                    "location" to arrayListOf(location.latitude.toString(), location.longitude.toString()),
                    "address" to address,
                    "pinCode" to postalCode,
                    "village" to "",
                    "district" to city,
                    "state" to state,
                )

                val PashudhanDB = FirebaseFirestore.getInstance()
                val userRef = PashudhanDB.collection("users").document(mUserUUID)

                userRef
                    .update(dataToUpdate as Map<String, Any>)
                    .addOnSuccessListener {

                        with(mSharedPref.edit()) {
                            putString(
                                getString(R.string.sp_userAddress),
                                address
                            )
                            putString(
                                getString(R.string.sp_userPinCode),
                                postalCode
                            )
                            putString(
                                getString(R.string.sp_userDistrict),
                                city
                            )
                            putString(getString(R.string.sp_userState), state)
                            apply()
                        }
                        newAddressProgressBar.visibility = View.GONE
                        Log.d(TAG, "DocumentSnapshot successfully updated!")
                        var intent = Intent(this.context, BottomNavigationActivity::class.java)
                        startActivity(intent)
                    }
                    .addOnFailureListener { e -> Log.w(TAG, "Error updating document", e) }

            } else {
                helper.showSnackbar(
                    mActivity,
                    rootLayout,
                    getString(R.string.userRegistrationActivity_noLocationFoundMessage),
                    helper.ERROR_STATE
                )
            }
        } catch (e: IOException) {
            helper.showSnackbar(
                mActivity,
                rootLayout,
                getString(R.string.tryAgainMessage),
                helper.ERROR_STATE
            )
        }


    }

    // Get current location, if shifted
    // from previous location
    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mActivity)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()!!
        )
    }

    // If current location could not be located, use last location
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            currentLocation = LatLng(mLastLocation.latitude, mLastLocation.longitude)
        }
    }

    // function to check if GPS is on
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = mActivity.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    // Check if location permissions are
    // granted to the application
    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                mActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                mActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    // Request permissions if not granted before
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            mActivity,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_ID
        )
    }

    // What must happen when permission is granted
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
        }
    }

}