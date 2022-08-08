package io.amona.twinkorea.request

data class IpWhiteListRequest(
    val ip: String,
    val description: String? = null,
)