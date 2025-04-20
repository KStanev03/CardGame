package com.example.cardgame.datamanager

import com.example.cardgame.datamanager.user.User

object LoggedUser {
    private var username: String? = null
    private var password: String? = null
    private var userId: Int = -1

    fun login(user: User) {
        this.username = user.username
        this.password = user.password
        this.userId = user.uid
    }

    fun logout() {
        username = null
        password = null
        userId = -1
    }

    fun getUsername(): String? {return username}

    fun getPassword(): String? {return password}

    fun getUserId(): Int {return userId}

    fun setUsername(username: String) {this.username = username}

    fun setPassword(password: String) {this.password = password}

    fun setUserId(userId: Int) {this.userId = userId}
}
