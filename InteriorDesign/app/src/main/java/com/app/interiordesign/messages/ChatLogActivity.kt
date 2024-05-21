package com.app.interiordesign.messages

import User
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.app.interiordesign.databinding.*
import com.app.interiordesign.models.ChatMessage
import com.app.interiordesign.utils.DateUtils.getFormattedTimeChatLog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item

import java.util.*

class ChatLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatLogBinding
    private val adapter = GroupAdapter<GroupieViewHolder>()

    private val toUser: User?
        get() = intent.getParcelableExtra(USER_KEY)

    companion object {
        const val USER_KEY = "USER_KEY"
        const val IMAGE_REQUEST_CODE = 123
        val TAG = ChatLogActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.swiperefresh.setColorSchemeColors(ContextCompat.getColor(this, com.app.interiordesign.R.color.colorAccent))

        binding.recyclerviewChatLog.adapter = adapter

        supportActionBar?.title = toUser!!.name

        listenForMessages()

        // send message
        binding.sendButtonChatLog.setOnClickListener {
//            performSendMessage()
            sendMessageBoth(binding.edittextChatLog.text.toString(),"")
        }

        binding.iconImageView.setOnClickListener {
            openGallery()
        }

    }

    private fun listenForMessages() {
        binding.swiperefresh.isEnabled = true
        binding.swiperefresh.isRefreshing = true

        val fromId = FirebaseAuth.getInstance().uid ?: return
        val toId = toUser!!.uid
        val ref = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(TAG, "database error: " + databaseError.message)
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d(TAG, "has children: " + dataSnapshot.hasChildren())
                if (!dataSnapshot.hasChildren()) {
                    binding.swiperefresh.isRefreshing = false
                    binding.swiperefresh.isEnabled = false
                }
            }
        })

        ref.addChildEventListener(object : ChildEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                dataSnapshot.getValue(ChatMessage::class.java)?.let {
                    if (it.fromId == FirebaseAuth.getInstance().uid) {
                        val currentUser = MessageMenuActivity.currentUser ?: return
                        if (it.text.isNotEmpty()) {
                            // Text message
                            adapter.add(ChatFromItem(it.text, currentUser, it.timestamp))
                        } else {
                            // Image message
                            adapter.add(ChatImageFromItem(it.imageUrl, currentUser, it.timestamp))
                        }
                    } else {
                        if (it.text.isNotEmpty()) {
                            // Text message
                            adapter.add(ChatToItem(it.text, toUser!!, it.timestamp))
                        } else {
                            // Image message
                            adapter.add(ChatImageToItem(it.imageUrl, toUser!!, it.timestamp))
                        }
                    }
                }
                binding.recyclerviewChatLog.scrollToPosition(adapter.itemCount - 1)
                binding.swiperefresh.isRefreshing = false
                binding.swiperefresh.isEnabled = false
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
            }
        })
    }

    private fun performSendMessage() {
        val text = binding.edittextChatLog.text.toString()
        if (text.isEmpty()) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val fromId = FirebaseAuth.getInstance().uid ?: return
        val toId = toUser!!.uid

        val reference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()
        val toReference = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        val chatMessage = ChatMessage(reference.key!!, text, fromId, toId, System.currentTimeMillis() / 1000,"")
        reference.setValue(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG, "Saved our chat message: ${reference.key}")
                binding.edittextChatLog.text!!.clear()
                binding.recyclerviewChatLog.smoothScrollToPosition(adapter.itemCount - 1)
            }

        toReference.setValue(chatMessage)

        val latestMessageRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId/$toId")
        latestMessageRef.setValue(chatMessage)

        val latestMessageToRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$toId/$fromId")
        latestMessageToRef.setValue(chatMessage)
    }

    private fun sendMessageBoth(text: String,imageUrl: String) {
//        val text = binding.edittextChatLog.text.toString()
//        val imageUrl: String = ""  // Set this to the actual image URL if sending an image

        if (text.isEmpty() && imageUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val fromId = FirebaseAuth.getInstance().uid ?: return
        val toId = toUser!!.uid

        val reference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()
        val toReference = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        val chatMessage = ChatMessage(reference.key!!, text, fromId, toId, System.currentTimeMillis() / 1000,imageUrl)
        reference.setValue(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG, "Saved our chat message: ${reference.key}")
                binding.edittextChatLog.text!!.clear()
                binding.recyclerviewChatLog.smoothScrollToPosition(adapter.itemCount - 1)
            }

        toReference.setValue(chatMessage)

        val latestMessageRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId/$toId")
        latestMessageRef.setValue(chatMessage)

        val latestMessageToRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$toId/$fromId")
        latestMessageToRef.setValue(chatMessage)
    }


    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }

    // Handle the result from the gallery intent
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri = data.data
            if (selectedImageUri != null) {
                sendImage(selectedImageUri)
            }
        }
    }

    private fun performSendMessage1(imageUrl: String? = null) {
        if ( imageUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val fromId = FirebaseAuth.getInstance().uid ?: return
        val toId = toUser!!.uid

        val reference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()
        val toReference = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        val timestamp = System.currentTimeMillis() / 1000
        val chatMessage = ChatMessage(reference.key!!,"" , fromId, toId, timestamp,imageUrl)
        reference.setValue(chatMessage)
            .addOnSuccessListener {
                binding.edittextChatLog.text!!.clear()
                binding.recyclerviewChatLog.smoothScrollToPosition(adapter.itemCount - 1)
            }

        toReference.setValue(chatMessage)

        val latestMessageRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId/$toId")
        latestMessageRef.setValue(chatMessage)

        val latestMessageToRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$toId/$fromId")
        latestMessageToRef.setValue(chatMessage)
    }

    private fun sendImage(imageUri: Uri) {
        // Placeholder method to upload image to Firebase Storage and get the download URL
        uploadImageAndGetUrl(imageUri) { imageUrl ->
            // Once image is uploaded, call performSendMessage with the image URL
//            performSendMessage1( imageUrl)
            sendMessageBoth("",imageUrl)
        }
    }

    private fun uploadImageAndGetUrl(imageUri: Uri, callback: (String) -> Unit) {
        // Implement the logic to upload image to Firebase Storage and get the download URL
        // Example:
         val storageRef = FirebaseStorage.getInstance().getReference("/images/${UUID.randomUUID()}")
         storageRef.putFile(imageUri)
             .addOnSuccessListener { taskSnapshot ->
                 storageRef.downloadUrl.addOnSuccessListener { uri ->
                     // Get the download URL and pass it to the callback
                     callback(uri.toString())
                 }
             }
    }
}

class ChatFromItem(val text: String, val user: User, val timestamp: Long) : Item<GroupieViewHolder>() {

    private lateinit var binding: ChatFromRowBinding

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        binding = ChatFromRowBinding.bind(viewHolder.itemView)
        binding.textviewFromRow.text = text
        binding.fromMsgTime.text = getFormattedTimeChatLog(timestamp)

        val targetImageView = binding.imageviewChatFromRow

        if (!user.profileImageUrl.isNullOrEmpty()) {
            val requestOptions = RequestOptions().placeholder(com.app.interiordesign.R.drawable.no_image2)

            Glide.with(targetImageView.context)
                .load(user.profileImageUrl)
                .thumbnail(0.1f)
                .apply(requestOptions)
                .into(targetImageView)
        }
    }

    override fun getLayout(): Int {
        return com.app.interiordesign.R.layout.chat_from_row
    }
}

class ChatToItem(val text: String, val user: User, val timestamp: Long) : Item<GroupieViewHolder>() {

    private lateinit var binding: ChatToRowBinding

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        binding = ChatToRowBinding.bind(viewHolder.itemView)
        binding.textviewToRow.text = text
        binding.toMsgTime.text = getFormattedTimeChatLog(timestamp)

        val targetImageView = binding.imageviewChatToRow

        if (!user.profileImageUrl.isNullOrEmpty()) {
            val requestOptions = RequestOptions().placeholder(com.app.interiordesign.R.drawable.no_image2)

            Glide.with(targetImageView.context)
                .load(user.profileImageUrl)
                .thumbnail(0.1f)
                .apply(requestOptions)
                .into(targetImageView)
        }
    }

    override fun getLayout(): Int {
        return com.app.interiordesign.R.layout.chat_to_row
    }
}
class ChatImageFromItem(private val imageUrl: String, private val user: User, private val timestamp: Long) : Item<GroupieViewHolder>() {

    private lateinit var binding: ChatImageFromRowBinding

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        binding = ChatImageFromRowBinding.bind(viewHolder.itemView)
        binding.apply {
            // Load user's profile image
            Glide.with(imageviewChatFromRow.context)
                .load(user.profileImageUrl)
                .thumbnail(0.1f)
                .into(imageviewChatFromRow)

            // Show ProgressBar while loading
            progressImageFrom.visibility = View.VISIBLE

            // Load image sent by the user
            Glide.with(imageviewImageFrom.context)
                .load(imageUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Hide ProgressBar on load failure
                        progressImageFrom.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Hide ProgressBar on successful load
                        progressImageFrom.visibility = View.GONE
                        return false
                    }
                })
                .into(imageviewImageFrom)

            // Set timestamp
            imageMsgTimeFrom.text = getFormattedTimeChatLog(timestamp)

            imageviewImageFrom.setOnClickListener {
                openImageInGallery(imageUrl, it.context)
            }
        }
    }

    override fun getLayout(): Int {
        return com.app.interiordesign.R.layout.chat_image_from_row
    }
}

class ChatImageToItem(private val imageUrl: String, private val user: User, private val timestamp: Long) : Item<GroupieViewHolder>() {

    private lateinit var binding: ChatImageToRowBinding

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        binding = ChatImageToRowBinding.bind(viewHolder.itemView)
        binding.apply {
            // Load user's profile image
            Glide.with(imageviewChatToRow.context)
                .load(user.profileImageUrl)
                .thumbnail(0.1f)
                .into(imageviewChatToRow)

            // Show ProgressBar while loading
            progressImageTo.visibility = View.VISIBLE

            // Load image sent to the user
            Glide.with(imageviewImageTo.context)
                .load(imageUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Hide ProgressBar on load failure
                        progressImageTo.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Hide ProgressBar on successful load
                        progressImageTo.visibility = View.GONE
                        return false
                    }
                })
                .into(imageviewImageTo)

            // Set timestamp
            imageMsgTimeTo.text = getFormattedTimeChatLog(timestamp)

            imageviewImageTo.setOnClickListener {
                openImageInGallery(imageUrl, it.context)
            }
        }
    }

    override fun getLayout(): Int {
        return com.app.interiordesign.R.layout.chat_image_to_row
    }
}


    fun openImageInGallery(imageUrl: String, context: Context) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(Uri.parse(imageUrl), "image/*")
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
    }
