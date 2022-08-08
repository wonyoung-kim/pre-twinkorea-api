package io.amona.twinkorea.request

data class OfferRequest(
    val offerId: Long? = null,
    val landId: Long? = null,
    val cellIds: String? = null,
    val buyerId: Long? = null,
    val sellerId: Long? = null,
    val price: Long? = null,
    val name: String? = null,
)
