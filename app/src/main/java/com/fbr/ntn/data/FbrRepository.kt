package com.fbr.ntn.data

import com.fbr.ntn.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class FbrRepository(
    private val sessionStore: SessionStore,
    private val cacheDir: File
) {
    companion object {
        const val API_LOGO_FILE = "api_seller_logo.png"
        private const val _BASE_URL = "https://zeenodi.com/lawyer-crm/crm/public/"
        val BASE_URL: String get() = _BASE_URL
    }

    private val client by lazy {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor(Interceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "ZeenoSmartDi/1.0 Android")
                    .build()
                chain.proceed(req)
            })
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (com.fbr.ntn.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            })
            .build()
    }

    private fun api(url: String = _BASE_URL): FbrApi = Retrofit.Builder()
        .baseUrl(url)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FbrApi::class.java)

    fun validSession(): Session? = runCatching { sessionStore.read() }.getOrNull()
    fun savedUsername(): String? = runCatching { sessionStore.read()?.username }.getOrNull()
    fun clearSession() = runCatching { sessionStore.clear() }
    fun saveSession(session: Session) = runCatching { sessionStore.save(session) }

    suspend fun connectAndLogin(apiUrl: String, username: String, password: String, pin: String, ntn: String = ""): AppResult<Session> = try {
        val safeUrl = if (apiUrl.endsWith("/")) apiUrl else "$apiUrl/"
        val loginRes = api(safeUrl).login(
            LoginRequest(username = username, password = password, pin = pin, ntn = ntn)
        )
        if (loginRes.success && loginRes.data != null) {
            val d = loginRes.data
            val session = Session(
                token = d.token,
                expiresAtEpochSeconds = System.currentTimeMillis() / 1000 + 60 * 60 * 24 * 30,
                ntn = d.ntn,
                displayName = d.partyname,
                apiUrl = safeUrl,
                username = username
            )
            saveSession(session)
            AppResult.Success(session)
        } else {
            AppResult.Error(ErrorKind.UNAUTHORIZED, loginRes.message.ifBlank { "Login failed" })
        }
    } catch (t: Throwable) { mapError(t) }

    suspend fun checkNtn(ntn: String): AppResult<AccountContext> = try {
        val res = api().checkNtn(CheckNtnRequest(ntn))
        if (res.status == "found") {
            AppResult.Success(AccountContext(ntn, "", ""))
        } else {
            AppResult.Error(ErrorKind.NOT_FOUND, "NTN not found on FBR")
        }
    } catch (t: Throwable) { mapError(t) }

    suspend fun verifyPin(ntn: String, pin: String): AppResult<Session> = try {
        val pinRes = api().verifyPin(VerifyPinRequest(ntn, pin.toInt()))
        if (pinRes.status != "verified") {
            AppResult.Error(ErrorKind.UNAUTHORIZED, "PIN verification failed")
        } else {
            val dynamicUrl = pinRes.url
            val safeUrl = if (dynamicUrl.endsWith("/")) dynamicUrl else "$dynamicUrl/"
            val loginRes = api(safeUrl).login(
                LoginRequest(username = pin, password = pin, pin = pin, ntn = ntn)
            )
            if (loginRes.success && loginRes.data != null) {
                val d = loginRes.data
                val session = Session(
                    token = d.token,
                    expiresAtEpochSeconds = System.currentTimeMillis() / 1000 + 60 * 60 * 24 * 30,
                    ntn = d.ntn,
                    displayName = d.partyname,
                    apiUrl = safeUrl,
                    username = pin
                )
                saveSession(session)
                AppResult.Success(session)
            } else {
                AppResult.Error(ErrorKind.UNAUTHORIZED, loginRes.message.ifBlank { "Login failed" })
            }
        }
    } catch (t: Throwable) { mapError(t) }

    suspend fun getPending(): AppResult<List<PendingItem>> = try {
        val session = sessionStore.read() ?: error("No valid session")
        val baseUrl = session.apiUrl ?: _BASE_URL
        val res = api(baseUrl).getPending(session.ntn ?: "")
        if (res.success) {
            val items = res.data.map { dto ->
                PendingItem(
                    id = dto.sr.toString(),
                    number = "INV-${dto.fyear}-${dto.inv}",
                    title = dto.party,
                    client = dto.party,
                    date = dto.date,
                    dueDate = dto.date,
                    status = if (dto.chk == 1) PendingStatus.POSTED else PendingStatus.VALIDATE,
                    period = dto.fyear,
                    dueLabel = if (dto.chk == 1) "Posted" else "Pending",
                    sellerName = session.displayName ?: "",
                    sellerNtn = session.ntn ?: "",
                    buyerNtn = dto.ntn,
                    buyerStrn = "",
                    buyerAddr = "",
                    fbrInvoiceNo = "",
                    items = emptyList()
                )
            }
            AppResult.Success(items)
        } else {
            AppResult.Error(ErrorKind.SERVER, "Failed to load invoices")
        }
    } catch (t: Throwable) { mapError(t) }

    suspend fun getPosted(sdate: String, edate: String): AppResult<List<PendingItem>> = try {
        val session = sessionStore.read() ?: error("No valid session")
        val baseUrl = session.apiUrl ?: _BASE_URL
        val res = api(baseUrl).getPosted(session.ntn ?: "", sdate, edate)
        if (res.success) {
            val items = res.data.map { dto ->
                PendingItem(
                    id = "posted-${dto.sr}",
                    number = "INV-${dto.fyear}-${dto.inv}",
                    title = dto.party,
                    client = dto.party,
                    date = dto.date,
                    dueDate = dto.date,
                    status = PendingStatus.POSTED,
                    period = dto.fyear,
                    dueLabel = "Posted",
                    sellerName = session.displayName ?: "",
                    sellerNtn = session.ntn ?: "",
                    buyerNtn = dto.ntn,
                    fbrInvoiceNo = "",
                    items = emptyList()
                )
            }
            AppResult.Success(items)
        } else {
            AppResult.Error(ErrorKind.SERVER, "Failed to load posted invoices")
        }
    } catch (t: Throwable) { mapError(t) }

    suspend fun validateInvoice(invoiceId: String): AppResult<Pair<Boolean, String?>> = try {
        val session = sessionStore.read() ?: error("No valid session")
        val baseUrl = session.apiUrl ?: _BASE_URL
        val res = api(baseUrl).validate(InvoiceActionRequest(sr = invoiceId.toInt(), ntn = session.ntn ?: ""))
        AppResult.Success(res.success to null)
    } catch (t: Throwable) { mapError(t) }

    suspend fun postInvoice(invoiceId: String): AppResult<Boolean> = try {
        val session = sessionStore.read() ?: error("No valid session")
        val baseUrl = session.apiUrl ?: _BASE_URL
        val res = api(baseUrl).post(InvoiceActionRequest(sr = invoiceId.toInt(), ntn = session.ntn ?: ""))
        AppResult.Success(res.success)
    } catch (t: Throwable) { mapError(t) }

    suspend fun printInvoice(invoiceId: String): AppResult<PrintInvoiceData?> = try {
        val session = sessionStore.read() ?: error("No valid session")
        val baseUrl = session.apiUrl ?: _BASE_URL
        val res = api(baseUrl).printInvoice(PrintInvoiceRequest(sr = invoiceId.toInt(), ntn = session.ntn ?: ""))
        AppResult.Success(res.data)
    } catch (t: Throwable) { mapError(t) }

    private suspend fun cacheApiLogo(url: String) = withContext(Dispatchers.IO) {
        runCatching {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 8000; conn.readTimeout = 8000; conn.instanceFollowRedirects = true
            conn.inputStream.use { input -> File(cacheDir, API_LOGO_FILE).outputStream().use { output -> input.copyTo(output) } }
            conn.disconnect()
        }
    }

    private fun mapError(t: Throwable): AppResult.Error {
        val msg = t.message ?: t.javaClass.simpleName
        return when (t) {
            is java.io.IOException -> AppResult.Error(ErrorKind.NETWORK, "Network error: $msg")
            is retrofit2.HttpException -> when (t.code()) {
                401 -> AppResult.Error(ErrorKind.UNAUTHORIZED)
                404 -> AppResult.Error(ErrorKind.NOT_FOUND)
                else -> AppResult.Error(ErrorKind.SERVER, "Server error ${t.code()}: $msg")
            }
            else -> AppResult.Error(ErrorKind.UNKNOWN, msg)
        }
    }
}
