//package com.app.chatonlynearby.views
//
//import android.app.Activity
//import android.view.LayoutInflater
//import android.view.ViewGroup
//import androidx.recyclerview.widget.RecyclerView
//import com.app.chatonlynearby.databinding.LatestMessageRowBinding
//import com.app.interiordesign.messages.MessageListFragment
//import com.app.interiordesign.models.ChatMessage
//import com.app.chatonlynearby.models.User
//import com.app.interiordesign.utils.DateUtils
//import com.bumptech.glide.Glide
//import com.bumptech.glide.request.RequestOptions
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.*
//
//class LatestMessageRow(private val binding: LatestMessageRowBinding, private val context: MessageListFragment) :
//    RecyclerView.ViewHolder(binding.root) {
//
//    var chatPartnerUser: User? = null
//
//    fun bind(chatMessage: ChatMessage) {
//        binding.latestMessageTextview.text = chatMessage.text
//
//        val chatPartnerId: String =
//            if (chatMessage.fromId == FirebaseAuth.getInstance().uid) chatMessage.toId else chatMessage.fromId
//
//        val ref = FirebaseDatabase.getInstance().getReference("/users/$chatPartnerId")
//        ref.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onCancelled(p0: DatabaseError) {}
//
//            override fun onDataChange(p0: DataSnapshot) {
//                chatPartnerUser = p0.getValue(User::class.java)
//                binding.usernameTextviewLatestMessage.text = chatPartnerUser?.name
//                binding.latestMsgTime.text = DateUtils.getFormattedTime(chatMessage.timestamp)
//
//                if (!chatPartnerUser?.profileImageUrl.isNullOrEmpty()) {
//                    val requestOptions = RequestOptions().placeholder(com.app.chatonlynearby.R.drawable.no_image2)
//
//                    Glide.with(binding.imageviewLatestMessage.context)
//                        .load(chatPartnerUser?.profileImageUrl)
//                        .apply(requestOptions)
//                        .into(binding.imageviewLatestMessage)
//
//                    binding.imageviewLatestMessage.setOnClickListener {
//                        BigImageDialog.newInstance(chatPartnerUser?.profileImageUrl!!).show(
//                            (context as Activity).fragmentManager,
//                            ""
//                        )
//                    }
//                }
//            }
//        })
//    }
//
//    companion object {
//        fun create(parent: ViewGroup, context: MessageListFragment): LatestMessageRow {
//            val inflater = LayoutInflater.from(parent.context)
//            val binding = LatestMessageRowBinding.inflate(inflater, parent, false)
//            return LatestMessageRow(binding, context)
//        }
//    }
//}
package com.app.interiordesign.views

import User
import android.app.Activity
import com.app.interiordesign.databinding.LatestMessageRowBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.app.interiordesign.messages.MessageListFragment
import com.app.interiordesign.models.ChatMessage
import com.app.interiordesign.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item

class LatestMessageRow(val chatMessage: ChatMessage, val context: MessageListFragment) : Item<GroupieViewHolder>() {

    private lateinit var binding: LatestMessageRowBinding
    var chatPartnerUser: User? = null

    override fun getLayout(): Int {
        return com.app.interiordesign.R.layout.latest_message_row
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        binding = LatestMessageRowBinding.bind(viewHolder.itemView)
        bindViews()
    }

    private fun bindViews() {
        binding.latestMessageTextview.text = chatMessage.text

        val chatPartnerId: String = if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {
            chatMessage.toId
        } else {
            chatMessage.fromId
        }

        val ref = FirebaseDatabase.getInstance().getReference("/users/$chatPartnerId")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(p0: DataSnapshot) {
                chatPartnerUser = p0.getValue(User::class.java)
                updateUI()
            }
        })
    }

    private fun updateUI() {
        binding.usernameTextviewLatestMessage.text = chatPartnerUser?.name
        binding.latestMsgTime.text = DateUtils.getFormattedTime(chatMessage.timestamp)

        if (!chatPartnerUser?.profileImageUrl.isNullOrEmpty()) {
            val requestOptions = RequestOptions().placeholder(com.app.interiordesign.R.drawable.no_image2)

            Glide.with(binding.imageviewLatestMessage.context)
                .load(chatPartnerUser?.profileImageUrl)
                .apply(requestOptions)
                .into(binding.imageviewLatestMessage)

            binding.imageviewLatestMessage.setOnClickListener {
                BigImageDialog.newInstance(chatPartnerUser?.profileImageUrl!!).show((context as Activity).fragmentManager, "")
            }
        }
    }
}
