package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.IllegalArgumentException

object UserHolder {
    private val users = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User {
        if (users.containsKey(email.toLowerCase())) {
            throw IllegalArgumentException("A user with this email already exists")
        }
        return User.makeUser(fullName, email, password)
            .also { users[email.toLowerCase()] = it }
    }

    fun registerUserByPhone(
        fullName: String,
        rawPhone: String
    ): User {
        if (users.containsKey(rawPhone)) {
            throw IllegalArgumentException("A user with this phone already exists")
        }
        if (!rawPhone.isPhoneNumberValid()) {
            throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
        }
        return User.makeUser(fullName, null, null, rawPhone)
            .also { users[rawPhone] = it }
    }

    fun loginUser(login: String, password: String): String? {
        return users[login.trim().toLowerCase()]?.run {
            if (checkPassword(password)) this.userInfo
            else null
        }
    }

    fun requestAccessCode(login: String) {
        users[login]?.requestAccessCode()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() = users.clear()

    fun importUsers(usersList: List<String>): List<String> {
        println("UserHolder.importUsers - usersList: $usersList")
        return usersList.map { parseUser(it).userInfo }
            .toList()
    }

    fun parseUser(str: String): User {
        val delimiter = ";"
        val props = str.split(delimiter).map { if (it.isNotEmpty()) it else "" }
        if (props.size != 5) {
            throw IllegalArgumentException("Incorrect number of user properties. Needed 5, but was ${props.size}")
        }
        return User.makeUser(
            props[0]!!,
            if (props[1].isNotEmpty()) props[1] else null,
            null,
            props[3],
            props[4],
            props[2]
        )
    }

    private fun String.isPhoneNumberValid(): Boolean {
        return (first() == '+') and containsDigits(11)
    }

    private fun String.containsDigits(expectedNumber: Int): Boolean {
        var digitsCount = 0
        for (char in this) {
            if (char.isDigit()) {
                digitsCount++
            }
        }
        return digitsCount == expectedNumber
    }
}