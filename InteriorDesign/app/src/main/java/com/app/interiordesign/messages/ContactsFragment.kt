package com.app.interiordesign.messages

import User
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.app.interiordesign.R
import com.app.interiordesign.databinding.FragmentNewMessageBinding
import com.app.interiordesign.databinding.UserRowNewMessageBinding
import com.app.interiordesign.views.BigImageDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import java.util.HashSet

class ContactsFragment : Fragment() {

    companion object {
        const val USER_KEY = "USER_KEY"
        private val TAG = ContactsFragment::class.java.simpleName
    }

    private lateinit var binding: FragmentNewMessageBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNewMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        binding.swiperefresh.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.colorAccent))
        fetchUsers()
        binding.swiperefresh.setOnRefreshListener {
            fetchUsers()
        }
    }

    private fun fetchUsers() {
        binding.swiperefresh.isRefreshing = true
        val uid = FirebaseAuth.getInstance().uid
        val friend = HashSet<String>()

        val ref2 = FirebaseDatabase.getInstance().getReference("/users/$uid/contacts")
        ref2.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {}

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach {
                    val friId = it.getValue()
                    friend.add(friId.toString())
                    Log.d(TAG, friId.toString())
                }
            }
        })

        val ref = FirebaseDatabase.getInstance().getReference("/users")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {}

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val adapter = GroupAdapter<GroupieViewHolder>()
                dataSnapshot.children.forEach {
                    it.getValue(User::class.java)?.let { user ->
                        if (user.uid != uid && friend.contains(user.uid)) {
                            adapter.add(UserItem(user, requireContext()))
                        }
                    }
                }

                adapter.setOnItemClickListener { item, view ->
                    val userItem = item as UserItem
                    val intent = Intent(view.context, ChatLogActivity::class.java)
                    intent.putExtra(USER_KEY, userItem.user)
                    startActivity(intent)
                }

                binding.recyclerviewNewmessage.adapter = adapter
                binding.swiperefresh.isRefreshing = false
            }

        })
    }
}

class UserItem(val user: User, val context: Context) : Item<GroupieViewHolder>() {

    private lateinit var binding: UserRowNewMessageBinding

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        binding = UserRowNewMessageBinding.bind(viewHolder.itemView)
        binding.usernameTextviewNewMessage.text = user.name

        if (!user.profileImageUrl.isNullOrEmpty()) {
            val requestOptions = RequestOptions().placeholder(R.drawable.no_image2)

            Glide.with(binding.imageviewNewMessage.context)
                .load(user.profileImageUrl)
                .apply(requestOptions)
                .into(binding.imageviewNewMessage)

            binding.userId.text = user.uid

            binding.imageviewNewMessage.setOnClickListener {
                BigImageDialog.newInstance(user.profileImageUrl).show((context as Activity).fragmentManager, "")
            }
        }
    }

    override fun getLayout(): Int {
        return R.layout.user_row_new_message
    }
}
