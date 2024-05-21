//package com.app.chatonlynearby.account
//
//import android.app.Activity
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.net.Uri
//import android.os.Bundle
//import android.provider.MediaStore
//import android.util.Log
//import android.view.View
//import android.widget.Toast
//import androidx.appcompat.app.ActionBar
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import com.app.interiordesign.messages.MessageMenuActivity
//import com.app.chatonlynearby.models.User
//import com.google.android.gms.location.*
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.FirebaseDatabase
//import com.google.firebase.storage.FirebaseStorage
//import com.squareup.picasso.Picasso
//import java.util.*
//
//class RegisterActivity : AppCompatActivity() {
//
//    private var selectedPhotoUri: Uri? = null
//
//    var myLat: Double = 0.0
//    var myLong: Double = 0.0
//
//
//    companion object {
//        val TAG = RegisterActivity::class.java.simpleName!!
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_register)
//        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
//        supportActionBar?.setCustomView(R.layout.abs_layout)
//        supportActionBar?.elevation = 0.0f
//
//        register_button_register.setOnClickListener {
//            performRegistration()
//        }
//
//        // jump to log in
//        already_have_account_text_view.setOnClickListener {
//            val intent = Intent(this, LoginActivity::class.java)
//            startActivity(intent)
//            overridePendingTransition(R.anim.enter, R.anim.exit)
//        }
//
//        // choose the profile image
//        selectphoto_button_register.setOnClickListener {
//            val intent = Intent(Intent.ACTION_PICK)
//            intent.type = "image/*"
//            startActivityForResult(intent, 0)
//        }
//
//        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
//        }
//
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//
//        createLocationRequest()
//
//        locationCallback = object : LocationCallback() {
//            override fun onLocationResult(p0: LocationResult) {
//                super.onLocationResult(p0)
//
//                var lastLocation = p0.lastLocation
//
//                myLat = lastLocation.latitude
//
//                myLong = lastLocation.longitude
//
//            }
//        }
//
//        startLocationUpdates()
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
//            selectedPhotoUri = data.data ?: return
//            Log.d(TAG, "Photo was selected")
//            // Get and resize profile image
//            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
//            contentResolver.query(selectedPhotoUri!!, filePathColumn, null, null, null)?.use {
//                it.moveToFirst()
//                val columnIndex = it.getColumnIndex(filePathColumn[0])
//                val picturePath = it.getString(columnIndex)
//                // If picture chosen from camera rotate by 270 degrees else
//                if (picturePath.contains("DCIM")) {
//                    Picasso.get().load(selectedPhotoUri).rotate(270f).into(selectphoto_imageview_register)
//                } else {
//                    Picasso.get().load(selectedPhotoUri).into(selectphoto_imageview_register)
//                }
//            }
//            selectphoto_button_register.alpha = 0f
//        }
//    }
//
//    private fun performRegistration() {
//        val email = email_edittext_register.text.toString()
//        val password = password_edittext_register.text.toString()
//        val name = name_edittext_register.text.toString()
//        // valid necessary information
//        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
//            Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
//            return
//        }
//        /*
//            if (selectedPhotoUri == null) {
//                Toast.makeText(this, "Please select a photo", Toast.LENGTH_SHORT).show()
//                return
//            }
//        */
//        already_have_account_text_view.visibility = View.GONE
//        loading_view.visibility = View.VISIBLE
//
//        // Firebase Authentication to create a user with email and password
//        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
//            .addOnCompleteListener {
//                if (!it.isSuccessful) return@addOnCompleteListener
//
//                // else if successful
//                Log.d(TAG, "Successfully created user with uid: ${it.result!!.user.uid}")
//                uploadImageToFirebaseStorage()
//            }
//            .addOnFailureListener {
//                Log.d(TAG, "Failed to create user: ${it.message}")
//                loading_view.visibility = View.GONE
//                already_have_account_text_view.visibility = View.VISIBLE
//                Toast.makeText(this, "${it.message}", Toast.LENGTH_LONG).show()
//            }
//    }
//
//    private fun uploadImageToFirebaseStorage() {
//        if (selectedPhotoUri == null) {
//            // save user without photo
//            saveUserToFirebaseDatabase(null)
//        } else {
//            val filename = UUID.randomUUID().toString()
//            val ref = FirebaseStorage.getInstance().getReference("/images/$filename")
//            ref.putFile(selectedPhotoUri!!)
//                .addOnSuccessListener {
//                    Log.d(TAG, "Successfully uploaded image: ${it.metadata?.path}")
//
//                    @Suppress("NestedLambdaShadowedImplicitParameter")
//                    ref.downloadUrl.addOnSuccessListener {
//                        Log.d(TAG, "File Location: $it")
//                        saveUserToFirebaseDatabase(it.toString())
//                    }
//                }
//                .addOnFailureListener {
//                    Log.d(TAG, "Failed to upload image to storage: ${it.message}")
//                    loading_view.visibility = View.GONE
//                    already_have_account_text_view.visibility = View.VISIBLE
//                }
//        }
//
//    }
//
//    private fun saveUserToFirebaseDatabase(profileImageUrl: String?) {
//        val uid = FirebaseAuth.getInstance().uid ?: return
//        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
//
//        var list = arrayListOf<String>()
//        list.add(uid)
//
//        val user = if (profileImageUrl == null) {
//            User(uid, name_edittext_register.text.toString(), myLong, myLat, null, list)
//        } else {
//            User(uid, name_edittext_register.text.toString(), myLong, myLat, profileImageUrl, list)
//        }
//
//        ref.setValue(user)
//            .addOnSuccessListener {
//                Log.d(TAG, "Finally we saved the user to Firebase Database")
//                val intent = Intent(this, MessageMenuActivity::class.java)
//                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
//                startActivity(intent)
//                overridePendingTransition(R.anim.enter, R.anim.exit)
//            }
//            .addOnFailureListener {
//                Log.d(TAG, "Failed to set value to database: ${it.message}")
//                loading_view.visibility = View.GONE
//                already_have_account_text_view.visibility = View.VISIBLE
//            }
//    }
//
//    private var permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
//    private var REQUEST_CODE = 1001
//    private lateinit var fusedLocationClient : FusedLocationProviderClient
//    private lateinit var locationRequest : LocationRequest
//    private lateinit var locationCallback : LocationCallback
//    private var locationUpdateState = false
//
//    override fun onResume() {
//        super.onResume()
//        if(locationUpdateState) {
//            startLocationUpdates()
//        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//        this.fusedLocationClient.removeLocationUpdates(locationCallback)
//    }
//
//    private fun startLocationUpdates() {
//        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            return
//        }
//        this.fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
//    }
//
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if(requestCode == REQUEST_CODE) {
//            if(grantResults.size == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
//                startLocationUpdates()
//            }
//        }
//    }
//
//    private fun createLocationRequest() {
//        locationRequest = LocationRequest()
//        locationRequest.interval = 10000
//        locationRequest.fastestInterval = 5000
//        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//        var builder = LocationSettingsRequest.Builder()
//            .addLocationRequest(locationRequest)
//        val client = LocationServices.getSettingsClient(this)
//        val task = client.checkLocationSettings(builder.build())
//        task.addOnSuccessListener {
//            locationUpdateState = true
//            startLocationUpdates()
//        }
//    }
//}

package com.app.interiordesign.account

import User
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.app.interiordesign.HomeActivity
import com.app.interiordesign.messages.MessageMenuActivity
import com.app.interiordesign.databinding.ActivityRegisterBinding
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var selectedPhotoUri: Uri? = null
    private var myLat: Double = 0.0
    private var myLong: Double = 0.0
    private lateinit var radioButton:RadioButton
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var locationUpdateState = false

    companion object {
        private val TAG = RegisterActivity::class.java.simpleName!!
        private const val REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        val auth = FirebaseAuth.getInstance()

//        // Check the current user
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is signed in, redirect to HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish() // Close MainActivity so user can't navigate back to it
        }

        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(com.app.interiordesign.R.layout.abs_layout)
        supportActionBar?.elevation = 0.0f

        binding.registerButtonRegister.setOnClickListener {
            performRegistration()
        }

        // Jump to log in
        binding.alreadyHaveAccountTextView.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(com.app.interiordesign.R.anim.enter, com.app.interiordesign.R.anim.exit)
            finish()
        }

        // Choose the profile image
        binding.selectphotoButtonRegister.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createLocationRequest()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                val lastLocation = p0.lastLocation

                myLat = lastLocation!!.latitude

                myLong = lastLocation.longitude
            }
        }

        startLocationUpdates()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            selectedPhotoUri = data.data ?: return
            Log.d(TAG, "Photo was selected")
            // Get and resize profile image
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            contentResolver.query(selectedPhotoUri!!, filePathColumn, null, null, null)?.use {
                it.moveToFirst()
                val columnIndex = it.getColumnIndex(filePathColumn[0])
                val picturePath = it.getString(columnIndex)
                // If the picture is chosen from the camera, rotate by 270 degrees, else
                if (picturePath.contains("DCIM")) {
                    Picasso.get().load(selectedPhotoUri).rotate(270f).into(binding.selectphotoImageviewRegister)
                } else {
                    Picasso.get().load(selectedPhotoUri).into(binding.selectphotoImageviewRegister)
                }
            }
            binding.selectphotoButtonRegister.alpha = 0f
        }
    }

    private fun performRegistration() {
        val email = binding.emailEdittextRegister.text.toString()
        val password = binding.passwordEdittextRegister.text.toString()
        val name = binding.nameEdittextRegister.text.toString()

        // Valid necessary information
        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
            return
        }
        showLoadingView()
        binding.alreadyHaveAccountTextView.visibility = View.GONE
        binding.loadingView.visibility = View.VISIBLE

        // Firebase Authentication to create a user with email and password
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful){
                    hideLoadingView()
                    return@addOnCompleteListener
                }

                Log.d(TAG, "Successfully created user with uid: ${it.result!!.user!!.uid}")
                uploadImageToFirebaseStorage()
            }
            .addOnFailureListener {
                Log.d(TAG, "Failed to create user: ${it.message}")
                hideLoadingView()
                Toast.makeText(this, "${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun uploadImageToFirebaseStorage() {
        if (selectedPhotoUri == null) {
            // Save user without a photo
            saveUserToFirebaseDatabase(null)
        } else {
            val filename = UUID.randomUUID().toString()
            val ref = FirebaseStorage.getInstance().getReference("/images/$filename")
            ref.putFile(selectedPhotoUri!!)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully uploaded image: ${it.metadata?.path}")

                    @Suppress("NestedLambdaShadowedImplicitParameter")
                    ref.downloadUrl.addOnSuccessListener {
                        Log.d(TAG, "File Location: $it")
                        saveUserToFirebaseDatabase(it.toString())
                    }
                }
                .addOnFailureListener {
                    Log.d(TAG, "Failed to upload image to storage: ${it.message}")
                    hideLoadingView()
                }
        }
    }

    private fun saveUserToFirebaseDatabase(profileImageUrl: String?) {
        val uid = FirebaseAuth.getInstance().uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        val radioId = binding.regRadioGroup.checkedRadioButtonId
        radioButton = findViewById<RadioButton>(radioId)

        val list = arrayListOf<String>()
        list.add(uid)

        val user = if (profileImageUrl == null) {
            User(uid, binding.nameEdittextRegister.text.toString(), myLong, myLat, null, list,radioButton.text.toString())
        } else {
            User(uid, binding.nameEdittextRegister.text.toString(), myLong, myLat, profileImageUrl, list,radioButton.text.toString())
        }

        ref.setValue(user)
            .addOnSuccessListener {
                Log.d(TAG, "Finally, we saved the user to Firebase Database")
                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                overridePendingTransition(com.app.interiordesign.R.anim.enter, com.app.interiordesign.R.anim.exit)
            }
            .addOnFailureListener {
                Log.d(TAG, "Failed to set value to the database: ${it.message}")
                binding.loadingView.visibility = View.GONE
                binding.alreadyHaveAccountTextView.visibility = View.VISIBLE
            }
    }
    fun checkButton(view: View) {
        val radioId = binding.regRadioGroup.checkedRadioButtonId
        radioButton = view.findViewById<RadioButton>(radioId)
    }


    private var permissions = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onResume() {
        super.onResume()
        if (locationUpdateState) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults.size == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED
            ) {
                startLocationUpdates()
            }
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
    }

    private fun showLoadingView() {
        binding.alreadyHaveAccountTextView.visibility = View.GONE
        binding.loadingView.visibility = View.VISIBLE
    }

    private fun hideLoadingView() {
        binding.loadingView.visibility = View.GONE
        binding.alreadyHaveAccountTextView.visibility = View.VISIBLE
    }
}
