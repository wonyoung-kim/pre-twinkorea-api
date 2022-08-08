package io.amona.twinkorea.response

data class CreateAdminResponse(
    val otpSecretKey: String,
    val qrBarcodeUrl: String,
)