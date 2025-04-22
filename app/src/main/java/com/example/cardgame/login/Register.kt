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
            "^(?=.*[0-9])" +
                    "(?=.*[a-z])" +
                    "(?=.*[A-Z])" +
                    "(?=.*[!@#$%^&*()_+])" +
                    "(?=\\S+$).{8,}$"
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

        if (email.isBlank() || username.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            Toast.makeText(this, "Моля попълнете всички полета", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Паролите не съвпадат", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isPasswordStrong(password)) {
            Toast.makeText(
                this,
                "Паролата трябва да бъде поне 8 знака и да съдържа:\n" +
                        "- Главна буква\n" +
                        "- Малка буква\n" +
                        "- Число\n" +
                        "- Специален символ (!@#$%^&*()_+)",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!isEmailValid(email)) {
            Toast.makeText(
                this,
                "Моля въведете валиден имейл адрес!\n",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val existingUserByUsername = userDAO.findByUsername(username)
                if (existingUserByUsername != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@Register,
                            "Потребителското име вече е заето",
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
                            "Имейлът вече е регистриран",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                userDAO.insert(User(0, username = username, password = password, email = email))

                val newUser = userDAO.findByUsername(username)

                newUser?.let {
                    val userInfo = UserInfo(
                        profileId = 0,
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
                        "Потребителят е регистриран успешно!",
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
                        "Неуспешна регистрация: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
