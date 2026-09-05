package com.fbr.ntn.model

data class AccountContext(
    val ntn: String,
    val displayName: String,
    val maskedEmail: String
)

data class LineItem(
    val description: String,
    val hsCode: String,
    val uom: String,
    val quantity: Double,
    val rate: Double,
    val taxRate: Double
) {
    val valueExcl: Double get() = quantity * rate
    val tax: Double get() = valueExcl * taxRate / 100.0
    val total: Double get() = valueExcl + tax
}

data class PendingItem(
    val id: String,
    val number: String,
    val title: String,
    val client: String,
    val date: String,
    val dueDate: String,
    val status: PendingStatus,
    val period: String,
    val dueLabel: String,
    val amountFromApi: Double = 0.0,
    val sellerName: String = "",
    val sellerLogoUrl: String = "",
    val sellerNtn: String = "",
    val sellerStrn: String = "",
    val sellerAddr: String = "",
    val sellerContact: String = "",
    val buyerNtn: String = "",
    val buyerStrn: String = "",
    val buyerAddr: String = "",
    val buyerRegType: String = "Registered",
    val paymentTerms: String = "",
    val saleType: String = "",
    val fbrInvoiceNo: String = "",
    val validationCode: String = "",
    val scenario: String = "",
    val columns: List<String> = emptyList(),
    val items: List<LineItem> = emptyList()
) {
    val subtotal: Double get() = if (items.isNotEmpty()) items.sumOf { it.valueExcl } else amountFromApi
    val tax: Double get() = if (items.isNotEmpty()) items.sumOf { it.tax } else 0.0
    val furtherTax: Double get() = 0.0
    val amount: Double get() = if (items.isNotEmpty()) subtotal + tax + furtherTax else amountFromApi
}

fun money(value: Double): String {
    val rounded = kotlin.math.round(value * 100) / 100.0
    val parts = rounded.toString().split(".")
    val intPart = parts[0].reversed().chunked(3).joinToString(",").reversed()
    val dec = (parts.getOrElse(1) { "0" } + "00").take(2)
    return "Rs $intPart.$dec"
}

fun qtyFmt(value: Double): String =
    if (value == kotlin.math.floor(value)) value.toLong().toString() else value.toString()

private val ONES = arrayOf(
    "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
    "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
    "seventeen", "eighteen", "nineteen"
)
private val TENS = arrayOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

private fun underThousand(n: Long): String {
    val parts = mutableListOf<String>()
    val h = n / 100
    val rest = n % 100
    if (h > 0) parts += "${ONES[h.toInt()]} hundred"
    if (rest > 0) {
        if (rest < 20) parts += ONES[rest.toInt()]
        else parts += TENS[(rest / 10).toInt()] + (if (rest % 10 > 0) "-${ONES[(rest % 10).toInt()]}" else "")
    }
    return parts.joinToString(" ")
}

/** Amount in words, matching the official template style ("Three hundred fifty-three thousand ..."). */
fun amountInWordsPKR(value: Double): String {
    var n = kotlin.math.abs(value).toLong()
    if (n == 0L) return "Zero Pakistani rupees only"
    val parts = mutableListOf<String>()
    val million = n / 1_000_000; n %= 1_000_000
    val thousand = n / 1_000; n %= 1_000
    if (million > 0) parts += "${underThousand(million)} million"
    if (thousand > 0) parts += "${underThousand(thousand)} thousand"
    if (n > 0) parts += underThousand(n)
    return (parts.joinToString(" ").replaceFirstChar { it.uppercase() } + " Pakistani rupees only")
}

enum class PendingStatus(val label: String) {
    VALIDATE("Validate"),
    POSTED("Posted")
}

data class ConnectionConfig(
    val apiUrl: String,
    val username: String,
    val pin: String
)

data class Session(
    val token: String,
    val expiresAtEpochSeconds: Long,
    val ntn: String? = null,
    val displayName: String? = null,
    val maskedEmail: String? = null,
    val apiUrl: String? = null,
    val username: String? = null
)

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Error(val kind: ErrorKind, val message: String? = null) : AppResult<Nothing>
}

enum class ErrorKind { NETWORK, NOT_FOUND, INVALID_OTP, UNAUTHORIZED, SERVER, UNKNOWN }
