package com.app.interiordesign

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.app.interiordesign.account.RegisterActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import java.util.*

class UpdateProfileActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var updateButton: Button
    private lateinit var loadingView: ProgressBar
    private lateinit var selectPhotoButton: Button
    private lateinit var selectPhotoImageView: CircleImageView
    private lateinit var loadingText: TextView
    private lateinit var originalImageUrl: String

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseStorage: FirebaseStorage = FirebaseStorage.getInstance()
    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()

    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_profile)
        supportActionBar?.hide()

        nameEditText = findViewById(R.id.name_edittext_update)
        emailEditText = findViewById(R.id.email_edittext_update)
        updateButton = findViewById(R.id.update_button)
        loadingView = findViewById(R.id.loading_view)
        loadingText = findViewById(R.id.loadingText)
//        selectPhotoButton = findViewById(R.id.selectphoto_button_update)
        selectPhotoImageView = findViewById(R.id.selectphoto_imageview_update)

        // Check if the user is signed in
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            // If the user is not signed in, start the SignInActivity and finish this activity
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
            return
        }

        // Load current user data
        loadUserData()

        // Set up click listeners
        selectPhotoImageView.setOnClickListener { openGallery() }
        updateButton.setOnClickListener { updateProfile() }
    }

    override fun onResume() {
        super.onResume()
    }

    // Load user data from Firebase
    private fun loadUserData() {
        val user = firebaseAuth.currentUser
        val uid = user?.uid

        // Assuming you have a "User" data class, replace it with your actual data structure
        val userReference = firebaseDatabase.getReference("/users/$uid")

        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").value.toString()
//                val email = snapshot.child("email").value.toString()
                val email = user?.email
                val imageUrl = snapshot.child("profileImageUrl").value.toString()
                originalImageUrl = imageUrl

                nameEditText.setText(name)
                emailEditText.setText(email)
//                if (imageUrl.isNotEmpty()) {
//                    Picasso.get().load(imageUrl).into(selectPhotoImageView)
//                }
                if (imageUrl.isNotEmpty()) {
                    Picasso.get().load(imageUrl)
                        .into(selectPhotoImageView, object : Callback {
                            override fun onSuccess() {
                                // Image loaded successfully
                                loadingView.visibility = View.GONE
                                loadingText.visibility = View.GONE
                                selectPhotoImageView.visibility = View.VISIBLE
                            }

                            override fun onError(e: Exception?) {
                                // Image loading failed
                                loadingView.visibility = View.GONE
                                loadingText.visibility = View.VISIBLE
                                selectPhotoImageView.visibility = View.GONE
                                loadingText.text = "Photo not loading. Please try again later."
                            }
                        })
                } else {
                    // No image URL, hide loader and show default image or handle accordingly
                    loadingView.visibility = View.GONE
                    loadingText.visibility = View.GONE
                    selectPhotoImageView.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    // Open the gallery to select a photo
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 0)
    }

    // Handle the result from the gallery intent
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
                selectedImageUri = data.data ?: return
                // Get and resize profile image
                val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                contentResolver.query(selectedImageUri!!, filePathColumn, null, null, null)?.use {
                    it.moveToFirst()
                    val columnIndex = it.getColumnIndex(filePathColumn[0])
                    val picturePath = it.getString(columnIndex)
                    // If the picture is chosen from the camera, rotate by 270 degrees, else
                    if (picturePath.contains("DCIM")) {
                        Picasso.get().load(selectedImageUri).rotate(270f).into(selectPhotoImageView)
                    } else {
                        Picasso.get().load(selectedImageUri).into(selectPhotoImageView)
                    }
                }
//                selectPhotoButton.alpha = 0f
        }
    }

    // Update the user profile
    private fun updateProfile() {
        val user = firebaseAuth.currentUser
        val uid = user?.uid
        val name = nameEditText.text.toString()

        showLoadingView()

        // Update profile image
        if (selectedImageUri != null) {
            val filename = UUID.randomUUID().toString()
            val imageRef = firebaseStorage.getReference("/images/$filename")
            imageRef.putFile(selectedImageUri!!)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { imageUrl ->
                        // Update user data with the new image URL
                        updateUserProfile(uid, name, imageUrl.toString())
                    }
                }
        } else {
            // Update user data without changing the image
            updateUserProfile(uid, name, originalImageUrl)
        }
    }

    // Update user profile in the Firebase Realtime Database
    private fun updateUserProfile(uid: String?, name: String, imageUrl: String) {
        val userReference = firebaseDatabase.getReference("/users/$uid")
        userReference.child("name").setValue(name)
        userReference.child("profileImageUrl").setValue(imageUrl)
            .addOnCompleteListener { task ->
                // Hide loading view after the update
                hideLoadingView()

                if (task.isSuccessful) {
                    // Show toast on successful update
                    showToast("Profile updated successfully!")

                    // Navigate back or perform any other action after the update
                    finish()
                } else {
                    // Handle update failure if needed
                    showToast("Failed to update profile. Please try again.")
                }
            }

        // Navigate back or perform any other action after the update
        // finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showLoadingView() {
        loadingView.visibility = View.VISIBLE
//        loadingText.visibility = View.GONE
//        selectPhotoImageView.visibility = View.VISIBLE
    }

    private fun hideLoadingView() {
        loadingView.visibility = View.GONE
        loadingText.visibility = View.GONE
        selectPhotoImageView.visibility = View.VISIBLE
    }
}
