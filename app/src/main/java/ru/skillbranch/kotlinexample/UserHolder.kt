package ru.skillbranch.kotlinexample

import java.lang.IllegalArgumentException

object UserHolder {
    private val users = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User {
        if (users.containsKey(email)) {
            throw IllegalArgumentException("A user with this email already exists")
        }
        return User.makeUser(fullName, email, password)
            .also { users[email] = it }
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
        return users[login.trim()]?.run {
            if (checkPassword(password)) this.userInfo
            else null
        }
    }

    fun requestAccessCode(login:String) {
        users[login]?.requestAccessCode()
    }

    fun clearHolder() = users.clear()

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