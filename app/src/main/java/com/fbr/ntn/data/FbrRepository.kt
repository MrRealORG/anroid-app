package com.fbr.ntn.data

import com.fbr.ntn.BuildConfig
import com.fbr.ntn.model.*
import kotlinx.coroutines.delay
import java.net.URLEncoder

class FbrRepository(private val api: FbrApi, private val sessionStore: SessionStore) {
    fun validSession(): Session? = runCatching { sessionStore.read() }.getOrNull()

    suspend fun checkNtn(ntn: String): AppResult<AccountContext> = request {
        if (BuildConfig.DEMO_MODE) {
            delay(850)
            AccountContext(ntn, "Ayesha Trading Co.", "03•• ••• 456", "demo-mobile")
        } else api.checkNtn(CheckNtnRequest(ntn)).let { AccountContext(it.ntn, it.accountName, it.maskedMobile, it.mobileToken) }
    }

    suspend fun sendOtp(mobileToken: String): AppResult<Pair<String, Int>> = request {
        if (BuildConfig.DEMO_MODE) { delay(800); "demo-challenge" to 30 }
        else api.sendOtp(SendOtpRequest(mobileToken)).let { it.challengeId to it.resendAfterSeconds }
    }

    suspend fun verifyOtp(challengeId: String, code: String): AppResult<String> = try {
        if (BuildConfig.DEMO_MODE) {
            delay(700)
            if (code == "123456") AppResult.Success("demo-verification") else AppResult.Error(ErrorKind.INVALID_OTP)
        } else AppResult.Success(api.verifyOtp(VerifyOtpRequest(challengeId, code)).verificationToken)
    } catch (t: Throwable) { mapError(t) }

    suspend fun getLoginLink(token: String): AppResult<String> = request {
        if (BuildConfig.DEMO_MODE) {
            delay(450)
            val html = """<html><meta name='viewport' content='width=device-width'><body style='font-family:sans-serif;background:#f4f7ff;padding:32px;color:#101828'><h2>Secure sign in</h2><p>Your identity has been verified. Continue to create your secure app session.</p><button onclick="location.href='fbrntn://auth/complete?token=demo-session'" style='border:0;border-radius:30px;background:#0A5FFF;color:white;padding:16px 24px;font-size:16px'>Continue securely</button></body></html>"""
            "data:text/html;charset=utf-8," + URLEncoder.encode(html, "UTF-8").replace("+", "%20")
        } else api.getLoginLink(LoginLinkRequest(token)).url
    }

    fun completeWebLogin(token: String, account: AccountContext?) {
        sessionStore.save(Session(
            token = token,
            expiresAtEpochSeconds = System.currentTimeMillis() / 1000 + 60 * 60 * 24 * 30,
            ntn = account?.ntn,
            displayName = account?.displayName,
            maskedMobile = account?.maskedMobile
        ))
    }

    suspend fun getPending(): AppResult<List<PendingItem>> = request {
        val session = sessionStore.read() ?: error("No valid session")
        if (BuildConfig.DEMO_MODE) {
            delay(900)
            listOf(
                PendingItem("1", "Income Tax Return", PendingStatus.DUE_SOON, "Tax year 2025", "Due 30 Sep"),
                PendingItem("2", "Sales Tax Declaration", PendingStatus.PENDING, "August 2026", "Awaiting submission"),
                PendingItem("3", "Withholding Statement", PendingStatus.REVIEW, "Q3 2026", "Under review")
            )
        } else api.getPending("Bearer ${session.token}").items.map {
            PendingItem(it.id, it.title, runCatching { PendingStatus.valueOf(it.status.uppercase()) }.getOrDefault(PendingStatus.PENDING), it.period, it.dueLabel)
        }
    }

    private suspend fun <T> request(block: suspend () -> T): AppResult<T> = try { AppResult.Success(block()) } catch (t: Throwable) { mapError(t) }
    private fun mapError(t: Throwable): AppResult.Error = when (t) {
        is java.io.IOException -> AppResult.Error(ErrorKind.NETWORK)
        is retrofit2.HttpException -> when (t.code()) {
            401 -> AppResult.Error(ErrorKind.UNAUTHORIZED)
            404 -> AppResult.Error(ErrorKind.NOT_FOUND)
            else -> AppResult.Error(ErrorKind.SERVER)
        }
        else -> AppResult.Error(ErrorKind.UNKNOWN)
    }
}
