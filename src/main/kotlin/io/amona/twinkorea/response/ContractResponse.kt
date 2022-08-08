package io.amona.twinkorea.response

data class MultiCellContractResponse(
    val status: String,
    val message: String,
    val result: MutableList<ContractResponse>
)

data class ContractResponse(
    val cellId: Long,
    val message: String,
    val timeRemaining: Long,
)

data class MyAccountDataEncResponse(
    val signature: String,
    val encodedPrice: String,
)

