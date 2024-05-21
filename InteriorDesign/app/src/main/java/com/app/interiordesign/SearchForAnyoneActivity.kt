package com.app.interiordesign

import User
import android.app.ActionBar
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.interiordesign.messages.UserItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder

class SearchForAnyoneActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: GroupAdapter<GroupieViewHolder>

    private val userList = mutableListOf<User>()
    private val filteredUserList = mutableListOf<User>()

    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_for_anyone)
        supportActionBar?.hide()


        searchEditText = findViewById(R.id.searchEditText)
        recyclerView = findViewById(R.id.recyclerView)
        val clearButton = findViewById<ImageView>(R.id.clearButton)

        // Initialize Firebase database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("/users")

        // Initialize RecyclerView and adapter
        userAdapter = GroupAdapter<GroupieViewHolder>()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = userAdapter

        // Set up a TextWatcher for the search bar
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                clearButton.visibility = if (charSequence.isNullOrEmpty()) View.GONE else View.VISIBLE
                filterUsers(charSequence.toString())
            }

            override fun afterTextChanged(editable: Editable?) {}
        })

        // Fetch and display users based on roles
        fetchUsersBasedOnRole()

        clearButton.setOnClickListener {
            searchEditText.text.clear()
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(searchEditText.windowToken, 0)
        }
    }

    private fun fetchUsersBasedOnRole() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val currentRoleQuery = databaseReference.child(currentUserId ?: "").child("role")

        currentRoleQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentRole = snapshot.getValue(String::class.java)
                if (currentRole != null) {
                    // Query users based on their roles
                    val query = databaseReference.orderByChild("role").equalTo(getOppositeRole(currentRole))

                    query.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(usersSnapshot: DataSnapshot) {
                            for (userSnapshot in usersSnapshot.children) {
                                val user = userSnapshot.getValue(User::class.java)
                                if (user != null) {
                                    userList.add(user)
                                    filteredUserList.add(user)
                                    // Add the user to the GroupAdapter
                                    userAdapter.add(UserItem(user, this@SearchForAnyoneActivity))
                                }
                            }
                            userAdapter.setOnItemClickListener { item, _ ->
                                showAddContactDialog(item as? UserItem)
                            }

                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle database error
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }

    private fun showAddContactDialog(userItem: UserItem?) {
        val dialog = Dialog(this@SearchForAnyoneActivity, R.style.dialog)
        dialog.setContentView(R.layout.dialog_add_contact)
        val window = dialog.window
        window?.setLayout(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT)
        dialog.findViewById<Button>(R.id.yes).setOnClickListener {
            userItem?.let { item ->
                addToContacts(item.user.uid)
            }
            dialog.dismiss()
        }
        dialog.findViewById<Button>(R.id.cancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun addToContacts(userId: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users/${FirebaseAuth.getInstance().currentUser?.uid}")
        val contacts = dbRef.child("contacts")
        contacts.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                val users = p0.value as? ArrayList<String> ?: ArrayList()
                if (users.contains(userId)) {
                    Toast.makeText(this@SearchForAnyoneActivity, "Already in your contacts", Toast.LENGTH_SHORT).show()
                } else {
                    users.add(userId)
                    Toast.makeText(this@SearchForAnyoneActivity, "Add successful", Toast.LENGTH_SHORT).show()
                }
                contacts.setValue(users)
            }
        })
    }



    private fun filterUsers(query: String) {
        userAdapter.clear() // Clear previous items
        for (user in userList) {
            if (user.name.contains(query, ignoreCase = true)) {
                userAdapter.add(UserItem(user, this@SearchForAnyoneActivity))
            }
        }
    }

    private fun getOppositeRole(role: String): String {
        return if (role == "Customer") {
            "Dealer"
        } else {
            "Customer"
        }
    }
}
