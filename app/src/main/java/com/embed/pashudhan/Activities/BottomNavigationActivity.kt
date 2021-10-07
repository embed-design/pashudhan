package com.embed.pashudhan.Activities

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.embed.pashudhan.Fragments.*
import com.embed.pashudhan.Helper
import com.embed.pashudhan.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

class BottomNavigationActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "BottomNavigation==>"
    }

    private val pashuBazarFragment = PashuBazaarFragment()
    private val pashuSamwardhanFragment = PashuSamwardhanFragment()
    private val userProfileFragment = ProfileFragment()
    private val notificationsFragment = NotificationsFragment()
    private val locationFragment = LocationFragment()
    private lateinit var mBottomNavigationMenu: BottomNavigationView
    private lateinit var userProfileButton: ImageButton
    private lateinit var notificationsButton: ImageButton
    private lateinit var locationButton: ImageButton
    private var helper = Helper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.botton_navigation_activity_layout)
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            helper.updateFCMToken(it)
        }
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val locale = sharedPref.getString(getString(R.string.sp_locale), "mr")
            .toString()
        val userLatitude = sharedPref.getString(getString(R.string.sp_userLatitude), "0")
        val userLongitude = sharedPref.getString(getString(R.string.sp_userLongitude), "0")
        helper.changeAppLanguage(this, locale)

        if(userLatitude == "null" || userLongitude == "null") {
            locationFragment.apply {
                show(supportFragmentManager, LocationFragment.TAG)
            }
        }else {
            loadActivity()
        }
    }

    fun loadActivity() {
        var selectedFragment = intent.getStringExtra("fragment").toString()
        if (selectedFragment == "pashuBazaar") {
            replaceFragment(pashuBazarFragment)
        } else if (selectedFragment == "pashuSamwardhan") {
            replaceFragment(pashuSamwardhanFragment)
        } else if (selectedFragment == "profile") {
            replaceFragment(ProfileFragment.newInstance(
                intent.getStringExtra("userID").toString(),
            ))
        } else {
            replaceFragment(pashuBazarFragment)
        }

        mBottomNavigationMenu = findViewById(R.id.bottom_navigation)
        mBottomNavigationMenu.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menuItem_pashuBazaar -> replaceFragment(pashuBazarFragment)
                R.id.menuItem_pashuSamwardhan -> replaceFragment(pashuSamwardhanFragment)
                R.id.menuItem_pashuStory -> {
                    val intent = Intent(this, PashuStoryActivity::class.java)
                    startActivity(intent)
                }
            }
            true
        }
        userProfileButton = findViewById(R.id.profileBtn)
        userProfileButton.setOnClickListener {
//            replaceFragment(userProfileFragment)
            replaceFragment(ProfileFragment.newInstance(
                FirebaseAuth.getInstance().currentUser?.phoneNumber!!,
            ))
        }
        notificationsButton = findViewById(R.id.notificationsBtn)
        notificationsButton.setOnClickListener {
            replaceFragment(notificationsFragment)
        }

        locationButton = findViewById(R.id.locationBtn)
        locationButton.setOnClickListener {
            locationFragment.apply {
                show(supportFragmentManager, LocationFragment.TAG)
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        if (fragment != null) {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.nav_frameLayout, fragment)
            transaction.setReorderingAllowed(true)
            transaction.addToBackStack(fragment.tag)
            transaction.commit()
        }
    }

    override fun onBackPressed() {
        if(supportFragmentManager.backStackEntryCount > 1)  {
            supportFragmentManager.popBackStack()
        }else if (supportFragmentManager.backStackEntryCount == 1){
            finish()
        }
    }

}