package com.app.interiordesign

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import com.app.interiordesign.account.LoginActivity
import com.app.interiordesign.databinding.ActivityHomeBinding
import com.app.interiordesign.messages.MessageMenuActivity
import com.app.interiordesign.nearby.GetLocationActivity
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityHomeBinding.inflate(layoutInflater)
            setContentView(binding.root)
        supportActionBar?.hide()

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            onOptionsItemSelected(menuItem)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

            binding.cardView1.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }

            binding.cardView2.setOnClickListener {
                val intent = Intent(this, PaintActivity::class.java)
                startActivity(intent)
            }

            binding.cardView3.setOnClickListener {
                val intent = Intent(this, Measurement::class.java)
                startActivity(intent)
            }

            binding.cardView4.setOnClickListener {
                val intent = Intent(this, MessageMenuActivity::class.java)
                startActivity(intent)
            }

            binding.cardView5.setOnClickListener {
                val intent = Intent(this, UpdateProfileActivity::class.java)
                startActivity(intent)
            }
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.drawer_menu_edit_profile -> {
                val intent = Intent(this, UpdateProfileActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.drawer_menu_nav_search_people -> {
                val intent = Intent(this, SearchForAnyoneActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.drawer_menu_get_loc -> {
                val intent = Intent(this, GetLocationActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.drawer_menu_logout -> {
                logoutUser()
                true
            }
            R.id.drawer_menu_exit -> {
                // Show an AlertDialog
                AlertDialog.Builder(this)
                    .setMessage("Are you sure you want to exit?")
                    .setPositiveButton("OK") { dialog, _ ->
                        finish()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        // Handle negative button click (Cancel)
                        dialog.dismiss()
                    }
                    .show()

                true
            }



            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
    }
