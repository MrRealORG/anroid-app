package com.fbr.ntn.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class CheckNtnRequest(val ntn: String)
data class CheckNtnResponse(val ntn: String, val accountName: String, val maskedMobile: String, val mobileToken: String)
data class SendOtpRequest(val mobileToken: String)
data class SendOtpResponse(val challengeId: String, val resendAfterSeconds: Int)
data class VerifyOtpRequest(val challengeId: String, val code: String)
data class VerifyOtpResponse(val verificationToken: String)
data class LoginLinkRequest(val verificationToken: String)
data class LoginLinkResponse(val url: String)
data class PendingItemDto(val id: String, val title: String, val status: String, val period: String, val dueLabel: String)
data class PendingListResponse(val items: List<PendingItemDto>)

/** API placeholders are intentionally centralized here. Replace paths/DTO fields when contracts arrive. */
interface FbrApi {
    @POST("v1/ntn/check") suspend fun checkNtn(@Body request: CheckNtnRequest): CheckNtnResponse
    @POST("v1/auth/otp/send") suspend fun sendOtp(@Body request: SendOtpRequest): SendOtpResponse
    @POST("v1/auth/otp/verify") suspend fun verifyOtp(@Body request: VerifyOtpRequest): VerifyOtpResponse
    @POST("v1/auth/login-link") suspend fun getLoginLink(@Body request: LoginLinkRequest): LoginLinkResponse
    @GET("v1/pending") suspend fun getPending(@Header("Authorization") authorization: String): PendingListResponse
}
