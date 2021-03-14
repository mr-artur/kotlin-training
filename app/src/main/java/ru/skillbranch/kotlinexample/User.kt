package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {
    val userInfo: String
    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString(" ")

    private var phone: String? = null
        set(value) {
            field = value?.replace("[^+\\d]".toRegex(), "")
        }

    private var _login: String? = null
    var login: String
        set(value) {
            _login = value?.toLowerCase()
        }
        get() = _login!!

    private var salt: String = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    constructor(
        firstName: String,
        lastName: String?,
        email: String,
        password: String
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("Secondary mail constructor")
        passwordHash = encrypt(password)
    }

    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ) : this(firstName, lastName, meta = mapOf("auth" to "sms"), rawPhone = rawPhone) {
        println("Secondary phone constructor")
        val code = requestAccessCode()
        sendAccessCodeToUser(phone, code)
    }

    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        salt: String,
        passwordHash: String,
        rawPhone: String?
    ) : this(firstName, lastName, email, rawPhone, mapOf("src" to "csv")) {
        println("Secondary csv constructor")
        this.salt = salt
        this.passwordHash = passwordHash
    }

    init {
        println("First init block, primary constructor was called")

//        check(!firstName.isBlank()) { "FirstName must not be blank" }
//        check(email.isNullOrBlank() || rawPhone.isNullOrBlank()) { "Email or phone must not be blank" }

        rawPhone?.run {
            if (isNotEmpty()) {
                phone = this
                login = phone!!
            }
        }
        email?.run {
            login = this
        }

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()
    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) passwordHash = encrypt(newPass)
        else throw IllegalArgumentException("Incorrect current password")
    }

    private fun encrypt(password: String): String = salt.plus(password.md5())

    fun requestAccessCode(): String {
        return generateAccessCode().also {
            passwordHash = encrypt(it)
            accessCode = it
        }
    }

    private fun generateAccessCode(): String {
        val possible = "ABCDEFGHJIKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    private fun sendAccessCodeToUser(phone: String?, code: String) {
        println("sending code $code to phone $phone")
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray())
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null,
            passwordHash: String? = null,
            salt: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return when {
                passwordHash != null && salt != null -> User(
                    firstName,
                    lastName,
                    email,
                    salt,
                    passwordHash,
                    phone
                )
                !phone.isNullOrBlank() -> User(firstName, lastName, rawPhone = phone)
                !email.isNullOrBlank() and !password.isNullOrBlank() -> User(
                    firstName,
                    lastName,
                    email = email!!,
                    password = password!!
                )
                else -> throw IllegalArgumentException("Email or phone must not be null or blank")
            }
        }

        private fun String.fullNameToPair(): Pair<String, String?> {
            return split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when (size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException(
                            "FullName must contain only first and last name, " +
                                    "split result is ${this@fullNameToPair}"
                        )
                    }
                }
        }
    }
}