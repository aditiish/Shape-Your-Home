package com.app.interiordesign.nearby

import User
import android.Manifest
import android.app.ActionBar
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.app.interiordesign.messages.MessageMenuActivity
import com.app.interiordesign.views.BigImageDialog
import com.app.interiordesign.R
import com.app.interiordesign.databinding.ActivityGetLocationBinding
import com.app.interiordesign.databinding.UserRowNewMessageBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item

class GetLocationActivity : AppCompatActivity() {

    private val permissions = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private val REQUEST_CODE = 1001
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var currentUser: User? = null
    private var locationUpdateState = false
    private lateinit var binding: ActivityGetLocationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        supportActionBar?.hide()


        if (checkLocationPermissions()) {
            requestLocationPermissions()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createLocationRequest()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                val lastLocation = p0.lastLocation
                updateCurrentLocation(lastLocation!!.latitude, lastLocation.longitude)
            }
        }

        startLocationUpdates()

        binding.swiperefresh.setColorSchemeColors(
            ContextCompat.getColor(
                this,
                R.color.colorAccent
            )
        )

        supportActionBar?.title = "Pull down to find users nearby"
        Glide.with(this).asGif()
            .load("https://media1.tenor.com/images/d6cd5151c04765d1992edfde14483068/tenor.gif?itemid=5662595")
            .apply(RequestOptions.circleCropTransform())
            .into(binding.loading)

        fetchUsers()

        binding.swiperefresh.setOnRefreshListener {
            fetchUsers()
            binding.loading.visibility = View.GONE
        }
    }

//    private fun setupUI() {
//        binding.swiperefresh.setOnRefreshListener {
//            fetchUsers()
//            binding.loading.visibility = View.GONE
//        }

//        binding.recyclerviewNewmessage.layoutManager = LinearLayoutManager(this)
//    }

    private fun updateCurrentLocation(latitude: Double, longitude: Double) {
        val uid = FirebaseAuth.getInstance().uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        ref.child("lat").setValue(latitude)
        ref.child("lon").setValue(longitude)
    }

    private fun checkLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.return_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item?.itemId) {
            R.id.menu_back -> {
                navigateToMessageMenu()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun navigateToMessageMenu() {
        val intent = Intent(this, MessageMenuActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left)
    }

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
        if (checkLocationPermissions()) {
            return
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
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
            if (grantResults.size == 2 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
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

    private fun fetchUsers() {
        binding.swiperefresh.isRefreshing = true
        val uid = FirebaseAuth.getInstance().uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("/users")
        val ref2 = FirebaseDatabase.getInstance().getReference("/users/$uid")
        ref2.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                currentUser = dataSnapshot.getValue(User::class.java)
            }
        })

        var myLon = 0.0
        var myLat = 0.0
        if (currentUser != null) {
            myLon = currentUser!!.lon
            myLat = currentUser!!.lat
        }

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val adapter = GroupAdapter<GroupieViewHolder>()
                dataSnapshot.children.forEach {
                    it.getValue(User::class.java)?.let { user ->
                        if (user.uid != uid) {
                            val distance = getDistance(myLat, myLon, user.lat, user.lon)
                            if (distance < 1.0) {
                                adapter.add(UserItem(user, this@GetLocationActivity))
                            }
                        }
                    }
                }

                adapter.setOnItemClickListener { item, _ ->
                    showAddContactDialog(item as? UserItem)
                }

                binding.recyclerviewNewmessage.adapter = adapter
                binding.swiperefresh.isRefreshing = false
            }
        })
    }

    // distance calculate algorithm
    fun getDistance(myLat: Double, myLong: Double, hisLat: Double, hisLong: Double) : Double {
        var lat1 :Double = myLat * Math.PI/180.0
        var lon1: Double = myLong * Math.PI/180.0
        var lat2: Double = hisLat * Math.PI/180.0
        var lon2: Double = hisLong * Math.PI/180.0

        val a = 6378137.0 // WGS84 major axis
        val b = 6356752.3142 // WGS84 semi-major axis
        val f = (a - b) / a
        val aSqMinusBSqOverBSq = (a * a - b * b) / (b * b)

        val L = lon2 - lon1
        var A = 0.0
        val U1 = Math.atan((1.0 - f) * Math.tan(lat1))
        val U2 = Math.atan((1.0 - f) * Math.tan(lat2))

        val cosU1 = Math.cos(U1)
        val cosU2 = Math.cos(U2)
        val sinU1 = Math.sin(U1)
        val sinU2 = Math.sin(U2)
        val cosU1cosU2 = cosU1 * cosU2
        val sinU1sinU2 = sinU1 * sinU2

        var sigma = 0.0
        var deltaSigma = 0.0
        var cosSqAlpha = 0.0
        var cos2SM = 0.0
        var cosSigma = 0.0
        var sinSigma = 0.0
        var cosLambda = 0.0
        var sinLambda = 0.0

        var lambda = L // initial guess
        for (iter in 0 until 20) {
            val lambdaOrig = lambda
            cosLambda = Math.cos(lambda)
            sinLambda = Math.sin(lambda)
            val t1 = cosU2 * sinLambda
            val t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda
            val sinSqSigma = t1 * t1 + t2 * t2
            sinSigma = Math.sqrt(sinSqSigma)
            cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda
            sigma = Math.atan2(sinSigma, cosSigma)
            val sinAlpha = if (sinSigma == 0.0)
                0.0
            else
                cosU1cosU2 * sinLambda / sinSigma
            cosSqAlpha = 1.0 - sinAlpha * sinAlpha
            cos2SM = if (cosSqAlpha == 0.0)
                0.0
            else
                cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha

            val uSquared = cosSqAlpha * aSqMinusBSqOverBSq
            A = 1 + uSquared / 16384.0 * (4096.0 + uSquared * (-768 + uSquared * (320.0 - 175.0 * uSquared)))
            val B = uSquared / 1024.0 * (256.0 + uSquared * (-128.0 + uSquared * (74.0 - 47.0 * uSquared)))
            val C = f / 16.0 * cosSqAlpha * (4.0 + f * (4.0 - 3.0 * cosSqAlpha))
            val cos2SMSq = cos2SM * cos2SM
            deltaSigma = B * sinSigma * (cos2SM + B / 4.0 * (cosSigma * (-1.0 + 2.0 * cos2SMSq) - B / 6.0 * cos2SM *
                            (-3.0 + 4.0 * sinSigma * sinSigma) * (-3.0 + 4.0 * cos2SMSq)))
            lambda = L + (1.0 - C) * f * sinAlpha *
                    (sigma + C * sinSigma * (cos2SM + C * cosSigma * (-1.0 + 2.0 * cos2SM * cos2SM)))
            val delta = (lambda - lambdaOrig) / lambda
            if (Math.abs(delta) < 1.0e-12) {
                break
            }
        }
        return b * A * (sigma - deltaSigma)
    }


    private fun showAddContactDialog(userItem: UserItem?) {
        val dialog = Dialog(this@GetLocationActivity, R.style.dialog)
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
                    Toast.makeText(this@GetLocationActivity, "Already in your contacts", Toast.LENGTH_SHORT).show()
                } else {
                    users.add(userId)
                    Toast.makeText(this@GetLocationActivity, "Add successful", Toast.LENGTH_SHORT).show()
                }
                contacts.setValue(users)
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
                val activity = context as? Activity
                activity?.let {
                    BigImageDialog.newInstance(user.profileImageUrl).show(
                        it.fragmentManager,
                        ""
                    )
                }
            }
        }
    }

    override fun getLayout(): Int {
        return R.layout.user_row_new_message
    }
}
