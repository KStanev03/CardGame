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

class Profile : AppCompatActivity() {
    private lateinit var userInfoDAO: UserInfoDAO
    private lateinit var userDAO: UserDAO
    private var userId: Int = -1
    private lateinit var profileImageView: CircleImageView

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
            Toast.makeText(this, "Invalid user ID", Toast.LENGTH_SHORT).show()
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
        builder.setTitle("Choose your avatar")

        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.avatar_selection_grid, null)
        val gridView = dialogView.findViewById<GridView>(R.id.avatarGridView)

        val adapter = AvatarAdapter(this, avatarOptions)
        gridView.adapter = adapter

        builder.setView(dialogView)
        builder.setNegativeButton("Cancel") { dialog, _ ->
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
                Toast.makeText(this@Profile, "Avatar updated successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUpdateConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Update Profile")
            .setMessage("Are you sure you want to update your profile information?")
            .setPositiveButton("Update") { _, _ ->
                lifecycleScope.launch {
                    updateUserProfile()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private suspend fun updateUserProfile() {
        val displayName = displayNameEditText.text.toString()

        if (displayName.isBlank()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@Profile, "Display name cannot be empty", Toast.LENGTH_SHORT).show()
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
                    highScore = it.highScore
                )
                userInfoDAO.update(updatedInfo)
            }
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(this@Profile, "Profile updated successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Profile")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    deleteUserAccount()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private suspend fun deleteUserAccount() {
        withContext(Dispatchers.IO) {
            // UserInfo will be automatically deleted due to CASCADE relationship
            userDAO.delete(userDAO.findById(userId))
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(this@Profile, "Account deleted successfully", Toast.LENGTH_SHORT).show()

            val intent = Intent(this@Profile, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
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