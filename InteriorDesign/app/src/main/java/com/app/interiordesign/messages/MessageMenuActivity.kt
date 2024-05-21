package com.app.interiordesign.messages

import User
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.app.interiordesign.SearchForAnyoneActivity
import com.app.interiordesign.UpdateProfileActivity
import com.app.interiordesign.account.RegisterActivity
import com.app.interiordesign.databinding.ActivityLatestMessagesBinding
import com.app.interiordesign.nearby.GetLocationActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MessageMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLatestMessagesBinding

    companion object {
        var currentUser: User? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLatestMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fetchCurrentUser()

        val pageAdapter = PageAdapter(this, supportFragmentManager)
        binding.viewpager.adapter = pageAdapter
        binding.tabs.setupWithViewPager(binding.viewpager)
    }

    // New page add here
    class PageAdapter(private val context: Context, fm: androidx.fragment.app.FragmentManager) :
        FragmentPagerAdapter(fm) {
        override fun getItem(p0: Int): Fragment {
            return if (p0 == 0) {
                MessageListFragment()
            } else {
                ContactsFragment()
            }
        }

        override fun getCount(): Int {
            return 2
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return if (position == 0) {
                context.getString(com.app.interiordesign.
                        R.string.message)
            } else {
                context.getString(com.app.interiordesign.R.string.contacts)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(com.app.interiordesign.R.menu.nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // buttons on the title bar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item?.itemId) {
            com.app.interiordesign.R.id.menu_edit_profile -> {
                val intent = Intent(this, UpdateProfileActivity::class.java)
//                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                overridePendingTransition(com.app.interiordesign.R.anim.enter, com.app.interiordesign.R.anim.exit)
            }
            com.app.interiordesign.R.id.menu_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, RegisterActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                overridePendingTransition(com.app.interiordesign.R.anim.enter, com.app.interiordesign.R.anim.exit)
            }
            com.app.interiordesign.R.id.menu_search_people -> {
                val intent = Intent(this, SearchForAnyoneActivity::class.java)
//                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                overridePendingTransition(com.app.interiordesign.R.anim.enter, com.app.interiordesign.R.anim.exit)
            }


            com.app.interiordesign.R.id.menu_get_loc -> {
                val intent = Intent(this, GetLocationActivity::class.java)
//                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                overridePendingTransition(com.app.interiordesign.R.anim.left_to_right, com.app.interiordesign.R.anim.right_to_left)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    fun fetchCurrentUser() {
        val uid = FirebaseAuth.getInstance().uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                currentUser = dataSnapshot.getValue(User::class.java)
            }
        })
    }
}
