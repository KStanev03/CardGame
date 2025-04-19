package com.example.cardgame.login

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Email
import android.text.TextUtils
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.cardgame.R
import com.example.cardgame.datamanager.AppDatabase
import com.example.cardgame.datamanager.user.User
import com.example.cardgame.datamanager.user.UserDAO
import com.example.cardgame.datamanager.user.UserInfo
import com.example.cardgame.datamanager.user.UserInfoDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern


class Register : AppCompatActivity() {
    private lateinit var userDAO: UserDAO
    private lateinit var userInfoDAO: UserInfoDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val submitg = findViewById<Button>(R.id.btSubmit)
        val db = AppDatabase.getInstance(applicationContext)
        userDAO = db.userDAO()
        userInfoDAO = db.userInfoDAO()

        submitg.setOnClickListener {
            insert()
        }
        val loginText = findViewById<Button>(R.id.loginRegister)

        loginText.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }


    private fun isPasswordStrong(password: String): Boolean {

        val passwordPattern = Pattern.compile(
            "^(?=.*[0-9])" +       // at least 1 number
                    "(?=.*[a-z])" +         // at least 1 lower case letter
                    "(?=.*[A-Z])" +         // at least 1 upper case letter
                    "(?=.*[!@#$%^&*()_+])" + // at least 1 special character
                    "(?=\\S+$).{8,}$"       // at least 8 characters, no whitespace
        )
        return passwordPattern.matcher(password).matches()
    }

    private fun isEmailValid(email: String): Boolean {
        val emailPattern = Pattern.compile(
            "^[A-Za-z0-9_-]+@[a-z-]+\\.[a-z]+$"
        )
        return emailPattern.matcher(email).matches()
    }

    private fun insert() {
        val email = findViewById<EditText>(R.id.tEmailRegister).text.toString()
        val username = findViewById<EditText>(R.id.tUsernameRegister).text.toString()
        val password = findViewById<EditText>(R.id.tPassRegister).text.toString()
        val confirmPassword = findViewById<EditText>(R.id.tPassConfirmRegister).text.toString()

        // Validate all fields
        if (email.isBlank() || username.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if passwords match
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isPasswordStrong(password)) {
            Toast.makeText(
                this,
                "Password must be at least 8 characters long and contain:\n" +
                        "- Uppercase letter\n" +
                        "- Lowercase letter\n" +
                        "- Number\n" +
                        "- Special character (!@#$%^&*()_+)",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!isEmailValid(email)) {
            Toast.makeText(
                this,
                "Please enter a valid email address!\n",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Check if username or email already exists
                val existingUserByUsername = userDAO.findByUsername(username)
                if (existingUserByUsername != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@Register,
                            "Username is already taken",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val existingUserByEmail = userDAO.findByEmail(email)
                if (existingUserByEmail != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@Register,
                            "Email is already registered",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                // Insert the user with their actual username (not hardcoded)
                userDAO.insert(User(0, username = username, password = password, email = email))

                val newUser = userDAO.findByUsername(username)

                newUser?.let {
                    val userInfo = UserInfo(
                        profileId = 0, // or omit if it's auto-generated
                        userId = it.uid,
                        displayName = it.username,
                        avatar = "panda",
                        points = 0,
                        money = 0,
                        highScore = 0
                    )
                    userInfoDAO.insert(userInfo)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Register,
                        "User registered successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                val id = userDAO.findByUsername(username)



                withContext(Dispatchers.Main) {
                    val intent = Intent(this@Register, Login::class.java)
                    startActivity(intent)
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Register,
                        "Failed to register user: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}