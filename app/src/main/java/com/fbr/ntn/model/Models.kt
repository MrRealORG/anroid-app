package com.fbr.ntn.model

data class AccountContext(
    val ntn: String,
    val displayName: String,
    val maskedMobile: String,
    val mobileToken: String
)

data class PendingItem(
    val id: String,
    val title: String,
    val status: PendingStatus,
    val period: String,
    val dueLabel: String
)

enum class PendingStatus(val label: String) { DUE_SOON("Due soon"), PENDING("Pending"), REVIEW("In review") }

data class Session(
    val token: String,
    val expiresAtEpochSeconds: Long,
    val ntn: String? = null,
    val displayName: String? = null,
    val maskedMobile: String? = null
)

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Error(val kind: ErrorKind, val message: String? = null) : AppResult<Nothing>
}

enum class ErrorKind { NETWORK, NOT_FOUND, INVALID_OTP, UNAUTHORIZED, SERVER, UNKNOWN }
