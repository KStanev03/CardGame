package com.example.cardgame

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.cardgame.datamanager.AppDatabase
import com.example.cardgame.datamanager.user.User
import com.example.cardgame.datamanager.user.UserDAO
import com.example.cardgame.datamanager.user.UserInfo
import com.example.cardgame.datamanager.user.UserInfoDAO
import com.example.cardgame.login.Login
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// Add these imports to ProfileActivity.kt
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cardgame.LocationHelper
import com.example.cardgame.LocationResult
import com.google.android.material.button.MaterialButton

class Profile : AppCompatActivity() {
    private lateinit var userInfoDAO: UserInfoDAO
    private lateinit var userDAO: UserDAO
    private var userId: Int = -1
    private lateinit var profileImageView: CircleImageView

    private lateinit var locationTextView: TextView
    private lateinit var updateLocationButton: MaterialButton
    private lateinit var locationHelper: LocationHelper

    // UI elements
    private lateinit var emailEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var displayNameEditText: EditText
    private lateinit var pointsTextView: TextView
    private lateinit var moneyTextView: TextView
    private lateinit var highScoreTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        emailEditText = findViewById(R.id.tEmailProfile)
        usernameEditText = findViewById(R.id.tUsernameProfile)
        displayNameEditText = findViewById(R.id.tDisplayNameProfile)
        pointsTextView = findViewById(R.id.tvPoints)
        moneyTextView = findViewById(R.id.tvMoney)
        highScoreTextView = findViewById(R.id.tvHighScore)

        val updateButton = findViewById<Button>(R.id.btUpdateProfile)
        val deleteButton = findViewById<Button>(R.id.btDeleteProfile)
        profileImageView = findViewById(R.id.imageView)
        val editAvatarButton = findViewById<ImageView>(R.id.editAvatar)

        // Setup avatar selection
        editAvatarButton.setOnClickListener {
            showAvatarSelectionDialog()
        }

        // Initialize database
        val db = AppDatabase.getInstance(applicationContext)
        userInfoDAO = db.userInfoDAO()
        userDAO = db.userDAO()

        // Get userId from intent
        userId = intent.getIntExtra("USER_ID", -1)

        if (userId == -1) {
            Toast.makeText(this, "Невалидно потребителско ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load user data
        lifecycleScope.launch {
            loadUserData()
        }

        // Set up button click listeners
        updateButton.setOnClickListener {
            showUpdateConfirmationDialog()
        }

        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
        locationTextView = findViewById(R.id.tvLocation) // You'll need to add this to the layout
        updateLocationButton = findViewById(R.id.btnUpdateLocation) // You'll need to add this to the layout
        locationHelper = LocationHelper(this)

        updateLocationButton.setOnClickListener {
            requestLocationPermissions()
        }
    }

    private suspend fun loadUserData() {
        val user = withContext(Dispatchers.IO) { userDAO.findById(userId) }
        val userInfo = withContext(Dispatchers.IO) { userInfoDAO.getUserInfoByUserId(userId) }

        withContext(Dispatchers.Main) {
            user?.let {
                emailEditText.setText(it.email)
                usernameEditText.setText(it.username)
            }

            userInfo?.let {
                displayNameEditText.setText(it.displayName ?: user?.username)
                pointsTextView.text = it.points.toString()
                moneyTextView.text = it.money.toString()
                highScoreTextView.text = it.highScore.toString()
                locationTextView.text = it.location ?: "Локацията не е зададена"

                // Load avatar
                try {
                    val resourceId = resources.getIdentifier(it.avatar, "drawable", packageName)
                    if (resourceId != 0) {
                        profileImageView.setImageResource(resourceId)
                    } else {
                        // Fallback to default avatar if resource not found
                        profileImageView.setImageResource(R.drawable.panda)
                    }
                } catch (e: Exception) {
                    profileImageView.setImageResource(R.drawable.panda)
                }
            }
        }
    }

    private fun showAvatarSelectionDialog() {
        // List of available avatar animals (these should match your drawable resource names)
        val avatarOptions = listOf("panda", "duck", "bear", "pig", "frog", "sheep", "monkey", "bober", "koala", "tiger", "elephant", "lion")

        // Create a grid layout for the avatars
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Избери аватар")

        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.avatar_selection_grid, null)
        val gridView = dialogView.findViewById<GridView>(R.id.avatarGridView)

        val adapter = AvatarAdapter(this, avatarOptions)
        gridView.adapter = adapter

        builder.setView(dialogView)
        builder.setNegativeButton("Откажи") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()

        gridView.setOnItemClickListener { _, _, position, _ ->
            val selectedAvatar = avatarOptions[position]
            updateUserAvatar(selectedAvatar)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateUserAvatar(avatarName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            // Get current user info
            val userInfo = userInfoDAO.getUserInfoByUserId(userId)

            // Update the avatar in the database
            userInfo?.let {
                val updatedInfo = UserInfo(
                    profileId = it.profileId,
                    userId = it.userId,
                    displayName = it.displayName,
                    avatar = avatarName,
                    points = it.points,
                    money = it.money,
                    highScore = it.highScore
                )
                userInfoDAO.update(updatedInfo)
            }

            // Update the UI on the main thread
            withContext(Dispatchers.Main) {
                // Update the image view with the new avatar
                val resourceId = resources.getIdentifier(avatarName, "drawable", packageName)
                profileImageView.setImageResource(resourceId)
                Toast.makeText(this@Profile, "Аватарът е актоализиран успешно", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUpdateConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Актоализация на профил")
            .setMessage("Сигурен ли си с актуализацията?")
            .setPositiveButton("Актоализация") { _, _ ->
                lifecycleScope.launch {
                    updateUserProfile()
                }
            }
            .setNegativeButton("Отказ", null)
            .show()
    }

    private suspend fun updateUserProfile() {
        val displayName = displayNameEditText.text.toString()

        if (displayName.isBlank()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@Profile, "Показваното име е задължително", Toast.LENGTH_SHORT).show()
            }
            return
        }

        withContext(Dispatchers.IO) {
            // Get current user info
            val userInfo = userInfoDAO.getUserInfoByUserId(userId)

            userInfo?.let {
                val updatedInfo = UserInfo(
                    profileId = it.profileId,
                    userId = it.userId,
                    displayName = displayName,
                    avatar = it.avatar,
                    points = it.points,
                    money = it.money,
                    highScore = it.highScore,
                    location = it.location
                )
                userInfoDAO.update(updatedInfo)
            }
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(this@Profile, "Профилът актуализиран успешно", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Изтрий Профил")
            .setMessage("Сигурен ли си, че искаш да изтриеш профила?")
            .setPositiveButton("Изтрий") { _, _ ->
                lifecycleScope.launch {
                    deleteUserAccount()
                }
            }
            .setNegativeButton("Отказ", null)
            .show()
    }

    private suspend fun deleteUserAccount() {
        withContext(Dispatchers.IO) {
            // UserInfo will be automatically deleted due to CASCADE relationship
            userDAO.delete(userDAO.findById(userId))
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(this@Profile, "Профилът е изтрит успешно", Toast.LENGTH_SHORT).show()

            val intent = Intent(this@Profile, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Permissions granted, get location
                lifecycleScope.launch {
                    updateLocationDisplay()
                }
            }
            else -> {
                // Permissions denied
                Toast.makeText(
                    this@Profile,
                    "Разрешенията за достъп до локация са необходими за да се намери сегашната",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun requestLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                lifecycleScope.launch {
                    updateLocationDisplay()
                }
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Show explanation to user about why location is needed
                Toast.makeText(
                    this,
                    "Разрешенията за достъп до локация са необходими за да се намери сегашната",
                    Toast.LENGTH_LONG
                ).show()
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            else -> {
                // Request permission
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private suspend fun updateLocationDisplay() {
        // Show a loading indicator
        withContext(Dispatchers.Main) {
            locationTextView.text = "Търсиене на местоположение..."
            updateLocationButton.isEnabled = false
        }

        val locationResult = locationHelper.getCurrentLocation()

        withContext(Dispatchers.Main) {
            updateLocationButton.isEnabled = true

            when (locationResult) {
                is LocationResult.Success -> {
                    Log.d("ProfileActivity", "Location success: ${locationResult.locationName}")
                    locationTextView.text = locationResult.locationName
                    // Update the database with the new location
                    updateUserLocation(locationResult.locationName)
                    Toast.makeText(
                        this@Profile,
                            "Местоположението е актуализирано успешно!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is LocationResult.Error -> {
                    Log.e("ProfileActivity", "Location error: ${locationResult.message}")
                    Toast.makeText(
                        this@Profile,
                        "Error getting location: ${locationResult.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is LocationResult.PermissionRequired -> {
                    requestLocationPermissions()
                }
            }
        }
    }

    private suspend fun updateUserLocation(location: String) {
        withContext(Dispatchers.IO) {
            // Get current user info
            val userInfo = userInfoDAO.getUserInfoByUserId(userId)

            userInfo?.let {
                val updatedInfo = UserInfo(
                    profileId = it.profileId,
                    userId = it.userId,
                    displayName = it.displayName,
                    avatar = it.avatar,
                    points = it.points,
                    money = it.money,
                    highScore = it.highScore,
                    location = location
                )
                userInfoDAO.update(updatedInfo)
            }
        }
    }
}

/**
 * Adapter for displaying avatar options in a grid
 */
class AvatarAdapter(
    private val context: Context,
    private val avatarList: List<String>
) : BaseAdapter() {

    override fun getCount(): Int = avatarList.size

    override fun getItem(position: Int): Any = avatarList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView: ImageView = if (convertView == null) {
            ImageView(context).apply {
                layoutParams = ViewGroup.LayoutParams(150, 150)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(8, 8, 8, 8)
            }
        } else {
            convertView as ImageView
        }

        val resourceId = context.resources.getIdentifier(avatarList[position], "drawable", context.packageName)
        imageView.setImageResource(resourceId)
        return imageView
    }
}