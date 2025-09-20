package com.smartringpro.mannaheal.ui.activities

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.api.profile.ProfileDataRepository
import com.smartringpro.mannaheal.api.profile.ProfileDataResponse
import com.smartringpro.mannaheal.databinding.ActivityHomeBinding
import com.smartringpro.mannaheal.ui.fragments.AppointmentsFragment
import com.smartringpro.mannaheal.ui.fragments.CareFragment
import com.smartringpro.mannaheal.ui.fragments.DeviceFragment
import com.smartringpro.mannaheal.ui.fragments.DoctorsFragment
import com.smartringpro.mannaheal.ui.fragments.FamilyMembersFragment
import com.smartringpro.mannaheal.ui.fragments.HealthDataFragment
import com.smartringpro.mannaheal.ui.fragments.HomeFragment
import com.smartringpro.mannaheal.ui.fragments.ProfileFragment
import com.smartringpro.mannaheal.ui.fragments.ReferToFriendFragment
import com.smartringpro.mannaheal.ui.fragments.SpecialistsFragment
import com.smartringpro.mannaheal.ui.fragments.SymptomsFragment
import com.smartringpro.mannaheal.ui.fragments.VitalsFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit

class HomeActivity : AppCompatActivity() {
    private val BACKGROUND_LOCATION_REQUEST_CODE = 1001
    private lateinit var binding: ActivityHomeBinding

    //    private val deviceViewModel: DeviceViewModel by viewModels()
    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineLocationGranted || coarseLocationGranted) {
                setAppPref("location", "1")
                // Foreground location granted
                scheduleWorkerForGPS()

                // Now check if background location is needed and if it's Android 10+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {

                    // Prompt the user manually, ideally through a dialog
                    showBackgroundLocationPermissionDialog()
                }

            } else {
                setAppPref("location", "0")
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
            }
            checkBluetoothReadyAndStartService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        deviceViewModel.isDeviceConnected.observe(this, Observer { isConnected ->
//            if (!isConnected) {
//                resetDeviceData()
//            }
//        })

//        Log.i("Home Activity", "onCreate Connection status: ${deviceViewModel.isDeviceConnected}")
//        Log.i("Home Activity", "onCreate Connection check: $Extra_name")

//        val isConnected = ConnectionPreferences.getConnectionState(this)

//        val name = ConnectionPreferences.getDeviceName(this)

        setSupportActionBar(binding.toolbar)

        setupDrawer()

        val drawerIcon = findViewById<ImageButton>(R.id.drawerIcon)
        drawerIcon.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    openFragment(HomeFragment(), "Health")
                    true
                }

                R.id.specialists -> {
                    openFragment(SpecialistsFragment(), "Specialists")
                    true
                }

                R.id.appointments -> {
                    openFragment(AppointmentsFragment(), "Appointments")
//                    openFragment(BookSlotFragment(), "Slots")
                    true
                }

                R.id.device -> {
                    openFragment(DeviceFragment(), "Equipment")
                    true
                }

                R.id.activity -> {
                    openFragment(CareFragment(), "Caring Mode")
                    true
                }

                else -> false
            }
        }

//        if (!name.isNullOrEmpty() && name != "Extra_Name" && !isConnected) {
//            binding.bottomNavigationView.selectedItemId = R.id.device
//            binding.sideNavigationView.setCheckedItem(R.id.nav_settings)
//        } else if (savedInstanceState == null) {
//            openFragment(HomeFragment(), "Health")
//
////            openFragment(SpecialistsFragment(),"Book")
//        }
        openFragment(HomeFragment(), "Health")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle the back button event here
                // For example, finish the activity:
                backPressed()
            }
        })
    }


    private fun checkBluetoothReadyAndStartService() {

//        val isConnected = ConnectionPreferences.getConnectionState(this)
//
//        val name = ConnectionPreferences.getDeviceName(this)
//        val macAddress = ConnectionPreferences.getMacAddress(this)
//        if (isConnected || macAddress == null || name == null) {
//            return
//        }
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        // Step 1: Check if Bluetooth is supported
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            return
        }

        // Step 2: Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        // Step 3: Check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    1001
                )
                return
            }
        }

//        val intent = Intent(this, BackgroundService::class.java).apply {
//            putExtra("Extra_MacAddress", macAddress)
//            putExtra("Extra_name", name)
//        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(intent) // API 26+
//        } else {
//            startService(intent) // API < 26
//        }
    }

    private fun requestLocationPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            checkBluetoothReadyAndStartService()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showBackgroundLocationPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Background Location Required")
            .setMessage("This app needs background location permission to function properly even when the app is not in use.")
            .setPositiveButton("Allow") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    BACKGROUND_LOCATION_REQUEST_CODE
                )
            }
            .setNegativeButton("Cancel") { _, _ -> setAppPref("location", "0") }
            .show()
    }

    private fun scheduleWorkerForGPS() {

//        LocationHelper.fetchAndSaveLocation(this, isBackground = false)
//
//        val locationWorkRequest = PeriodicWorkRequestBuilder<LocationWorker>(15, TimeUnit.MINUTES)
//            .setConstraints(
//                Constraints.Builder()
//                    .setRequiredNetworkType(NetworkType.CONNECTED)
//                    .setRequiresBatteryNotLow(true)
//                    .build()
//            )
//            .build()
//
//        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
//            "LocationWork",
//            ExistingPeriodicWorkPolicy.KEEP,
//            locationWorkRequest
//        )
    }

    private fun setAppPref(key: String, value: String) {

        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

//    fun resetDeviceData() {
//        Log.i("Home Activity", "Reset BLE data")
//        val serviceIntent = Intent(this, BackgroundService::class.java)
//        stopService(serviceIntent)
//    }

    private fun loadUserDetails(userNameTextView: TextView, userNumberTextView: TextView) {
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("AppPreferences", MODE_PRIVATE)

        // Retrieve the saved name and mobile number from SharedPreferences
        val name = sharedPreferences.getString("user", "Unknown User")
        val mobileNumber = sharedPreferences.getString("mobileNumber", "Not Available")
        val userId = sharedPreferences.getInt("id", -1)
        if (userId != -1) {
            fetchUserProfileData(userId)
        } else {
            requestLocationPermissions()
        }

        // Set the retrieved details in the TextViews
        userNameTextView.text = name
        userNumberTextView.text = mobileNumber
    }

    private fun fetchUserProfileData(userId: Int) {
        val repository = ProfileDataRepository()
        val call = repository.getUserProfileData(userId)

        call.enqueue(object : Callback<ProfileDataResponse> {
            override fun onResponse(
                call: Call<ProfileDataResponse>,
                response: Response<ProfileDataResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val profileData = response.body()?.data
                    if (profileData != null) {
                        val sharedPreferences =
                            getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putString(
                            "user_gender",
                            profileData.gender
                        ) // Save gender as a string
                        editor.apply()
                        val profileImageUrl = profileData.patient_image_url
                        val headerView = binding.sideNavigationView.getHeaderView(0)
                        val profileImageView = headerView.findViewById<ImageView>(R.id.profileImage)

                        if (!profileImageUrl.isNullOrEmpty()) {
                            Glide.with(this@HomeActivity)
                                .load(profileImageUrl)
                                .circleCrop()
                                .into(profileImageView)
                        } else {
                            profileImageView.setImageResource(R.drawable.baseline_account_circle_24)
                        }
                        requestLocationPermissions()
                        val checkProfile = sharedPreferences.getBoolean("checkProfile", false)

                        if (!checkProfile && profileData.gender == null && profileData.dob == null && profileData.height == null) {
                            editor.putBoolean("checkProfile", true)
                            editor.apply()
                            val intent =
                                Intent(this@HomeActivity, RegisterProfileActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        }
                        Log.d("ProfileFragment", "User Data: $profileData")
                    } else {
                        Toast.makeText(this@HomeActivity, "No user data found", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Log.e("ProfileFragment", "API Error: ${response.errorBody()?.string()}")
                    Toast.makeText(
                        this@HomeActivity,
                        "Failed to fetch profile data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ProfileDataResponse>, t: Throwable) {
                Log.e("ProfileFragment", "API Failure: ${t.message}")
                Toast.makeText(this@HomeActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun clearUserData() {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.remove("accessToken")
        editor.remove("user")
        editor.remove("email")
        editor.remove("roleCode")
        editor.remove("mobileNumber")
        editor.remove("id")
        editor.remove("isLoggedIn")

        editor.apply()

        Log.d("Logout", "User data cleared from SharedPreferences.")
    }

    private fun resetBottomNavSelection() {
        binding.bottomNavigationView.menu.setGroupCheckable(0, true, false)
        for (i in 0 until binding.bottomNavigationView.menu.size()) {
            binding.bottomNavigationView.menu.getItem(i).isChecked = false
        }
        binding.bottomNavigationView.menu.setGroupCheckable(0, true, true)
    }

    // Setup DrawerToggle for Toolbar and side drawer icon click
    private fun setupDrawer() {
        val headerView = binding.sideNavigationView.getHeaderView(0) // Access the header layout
        val closeIcon = headerView.findViewById<ImageView>(R.id.closeIcon)
        val userNameTextView = headerView.findViewById<TextView>(R.id.userName)
        val userNumberTextView = headerView.findViewById<TextView>(R.id.userNumber)
        closeIcon.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START) // Close the drawer
        }

        loadUserDetails(userNameTextView, userNumberTextView)

        binding.sideNavigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_family_members -> openFragment(
                    FamilyMembersFragment(),
                    "Family members",
                    true
                )

                R.id.nav_settings -> {
                    val sharedPreferences =
                        this.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putInt("isAppointmentSummary", 1)
                    editor.apply()
                    openFragment(AppointmentsFragment(), "Appointments")
                }

                R.id.nav_about -> {
                    openFragment(ProfileFragment(), "Profile", true)
                    resetBottomNavSelection()
                }

                R.id.nav_refer_friend -> {
                    openFragment(ReferToFriendFragment(), "Refer a friend", true)
                }

                R.id.nav_vitals -> {
                    openFragment(VitalsFragment(), "Vitals", true)
                }

                R.id.nav_logout -> {
                    clearUserData()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    // Helper function to replace fragments
    fun openFragment(fragment: Fragment, title: String, addToBackStack: Boolean = false) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null) // Add to the back stack
        }

        transaction.commit()

        binding.toolbarTitle.text = title

        if (isRootFragment(fragment)) {
            supportActionBar?.setDisplayHomeAsUpEnabled(false) // Hide back button for root fragments
            binding.drawerIcon.visibility = View.VISIBLE // Show menu icon
        } else {
            supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back button for sub-fragments
            binding.drawerIcon.visibility = View.GONE // Hide menu icon
            supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_arrow_back_ios_24)
        }
    }

    fun openHealthDataFragment(type: String, title: String) {
        val fragment = HealthDataFragment().apply {
            arguments = Bundle().apply {
                putString("type", type) // Pass the type to customize the data
            }
        }
        openFragment(fragment, title, true) // Use the existing openFragment method
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun backPressed() {

        // Check if there are fragments in the back stack
        if (supportFragmentManager.backStackEntryCount > 0) {
            // Pop the fragment from the back stack
            supportFragmentManager.popBackStack()

            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                val currentFragment =
                    supportFragmentManager.findFragmentById(R.id.fragmentContainer)

                updateTitleBasedOnFragment(currentFragment)

                if (isRootFragment(currentFragment)) {
                    supportActionBar?.setDisplayHomeAsUpEnabled(false) // Hide back button for root fragments
                    binding.drawerIcon.visibility = View.VISIBLE // Show menu icon
                    selectBottomNavItem(currentFragment)
                } else {
                    supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back button for sub-fragments
                    binding.drawerIcon.visibility = View.GONE // Hide menu icon
                    supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_arrow_back_ios_24)
                }

            }, 100)
        } else {
            showExitConfirmationDialog()
        }
    }

    private fun showExitConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Are you sure you want to exit?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                binding.toolbarTitle.text = "Health"
                finish()
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    // This method will update the title based on the current fragment
    private fun updateTitleBasedOnFragment(fragment: Fragment?) {
        val sharedPreferences = this.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val fragmentTitle = sharedPreferences.getString("FragmentTitle", "")
        when (fragment) {
            is HomeFragment -> binding.toolbarTitle.text = "Health"
            is SpecialistsFragment -> binding.toolbarTitle.text = "Specialists"
            is CareFragment -> binding.toolbarTitle.text = "Caring Mode"
            is DeviceFragment -> binding.toolbarTitle.text = "Equipment"
            is AppointmentsFragment -> binding.toolbarTitle.text = "Appointments"
            is FamilyMembersFragment -> binding.toolbarTitle.text = "Family members"
            is SymptomsFragment -> binding.toolbarTitle.text = "Book an Appointment"
            is DoctorsFragment -> binding.toolbarTitle.text = fragmentTitle
            else -> binding.toolbarTitle.text = "Unknown"
        }
    }

    private fun isRootFragment(fragment: Fragment?): Boolean {
        return fragment is HomeFragment ||
                fragment is SpecialistsFragment ||
//                fragment is ActivitiesFragment ||
                fragment is DeviceFragment ||
                fragment is AppointmentsFragment
    }

    private fun selectBottomNavItem(fragment: Fragment?) {
        when (fragment) {
            is HomeFragment -> binding.bottomNavigationView.selectedItemId = R.id.home
            is SpecialistsFragment -> binding.bottomNavigationView.selectedItemId = R.id.specialists
            is CareFragment -> binding.bottomNavigationView.selectedItemId = R.id.activity
            is DeviceFragment -> binding.bottomNavigationView.selectedItemId = R.id.device
            is AppointmentsFragment -> binding.bottomNavigationView.selectedItemId =
                R.id.appointments
        }
    }
}

