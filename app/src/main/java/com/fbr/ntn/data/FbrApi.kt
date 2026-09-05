package com.fbr.ntn.data

import retrofit2.http.*

data class CheckNtnRequest(val ntn: String)
data class CheckNtnResponse(val status: String)

data class VerifyPinRequest(val ntn: String, val pin: Int)
data class VerifyPinResponse(val status: String, val ntn: String, val url: String)

data class LoginRequest(val username: String, val password: String, val pin: String, val ntn: String)
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val data: LoginData?
)
data class LoginData(
    val token: String,
    val ntn: String,
    val partyname: String,
    val address: String,
    val saletax: String,
    val mobile: String,
    val user: UserInfo?
)
data class UserInfo(val id: Int, val username: String, val role: String, val status: String)

data class PendingInvoiceDto(
    val sr: Int,
    val inv: Int,
    val date: String,
    val party: String,
    val ntn: String,
    val amount: String,
    val chk: Int,
    val fyear: String,
    val status: String = "pending"
)

data class PendingInvoicesResponse(val success: Boolean, val count: Int, val data: List<PendingInvoiceDto>)

data class InvoiceActionRequest(val sr: Int, val ntn: String)
data class InvoiceActionResponse(val success: Boolean, val message: String = "", val sr: Int = 0, val fbr_response: FbrValidationResponse? = null)
data class FbrValidationResponse(val dated: String = "", val validationResponse: ValidationDetail? = null)
data class ValidationDetail(val statusCode: String = "", val status: String = "", val error: String = "")

data class PostedInvoicesResponse(val success: Boolean, val count: Int, val data: List<PendingInvoiceDto>)

data class PrintInvoiceRequest(val sr: Int, val ntn: String)
data class PrintInvoiceResponse(val success: Boolean, val data: PrintInvoiceData?)
data class PrintInvoiceData(
    val company: PrintCompany?,
    val invoice: PrintInvoiceInfo?,
    val buyer: PrintBuyer?,
    val columns: List<String>,
    val items: List<PrintItem>,
    val summary: PrintSummary?,
    val fbr_token: String = ""
)
data class PrintCompany(val ntn: String, val name: String, val address: String, val province: String, val phone: String, val saletax: String = "")
data class PrintInvoiceInfo(val sr: Int, val inv: Int, val date: String, val token: String)
data class PrintBuyer(val name: String, val ntn: String, val address: String, val province: String, val phone: String)
data class PrintItem(
    @com.google.gson.annotations.SerializedName("#") val num: Int,
    @com.google.gson.annotations.SerializedName("Item Name") val itemName: String,
    @com.google.gson.annotations.SerializedName("Retail Price") val retailPrice: String,
    @com.google.gson.annotations.SerializedName("UOM") val uom: String,
    @com.google.gson.annotations.SerializedName("Qty") val qty: Int,
    @com.google.gson.annotations.SerializedName("Rate") val rate: Int,
    @com.google.gson.annotations.SerializedName("Amount") val amount: String,
    @com.google.gson.annotations.SerializedName("GST Rate") val gstRate: Int,
    @com.google.gson.annotations.SerializedName("GST Amount") val gstAmount: String,
    @com.google.gson.annotations.SerializedName("Total Incl. GST") val totalInclGst: String,
    @com.google.gson.annotations.SerializedName("HS Code") val hsCode: String
)
data class PrintSummary(val totalAmount: Double, val totalTax: Double, val totalFutherTax: Double, val totalDiscount: Double, val totalInclusive: Double)

interface FbrApi {
    @POST("api/check-ntn")
    suspend fun checkNtn(@Body request: CheckNtnRequest): CheckNtnResponse

    @POST("api/verify-pin")
    suspend fun verifyPin(@Body request: VerifyPinRequest): VerifyPinResponse

    @POST("api/mobile/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/invoices/pending")
    suspend fun getPending(@Query("ntn") ntn: String): PendingInvoicesResponse

    @POST("api/invoices/validate")
    suspend fun validate(@Body request: InvoiceActionRequest): InvoiceActionResponse

    @POST("api/invoices/post")
    suspend fun post(@Body request: InvoiceActionRequest): InvoiceActionResponse

    @GET("api/invoices/posted")
    suspend fun getPosted(
        @Query("ntn") ntn: String,
        @Query("sdate") sdate: String,
        @Query("edate") edate: String
    ): PostedInvoicesResponse

    @POST("api/invoices/print")
    suspend fun printInvoice(@Body request: PrintInvoiceRequest): PrintInvoiceResponse
}
