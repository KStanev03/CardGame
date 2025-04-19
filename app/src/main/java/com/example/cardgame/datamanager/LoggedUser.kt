package com.example.cardgame.datamanager

import com.example.cardgame.datamanager.user.User

object LoggedUser {
    private var username: String? = null
    private var password: String? = null

    fun login(user: User) {
        this.username = user.username
        this.password = user.password
    }

    fun logout() {
        username = null
        password = null
    }

    fun getUsername(): String? {return username}

    fun getPassword(): String? {return password}

    fun setUsername(username: String) {this.username = username}

    fun setPassword(password: String) {this.password = password}

    private var userId: Int = -1

}
