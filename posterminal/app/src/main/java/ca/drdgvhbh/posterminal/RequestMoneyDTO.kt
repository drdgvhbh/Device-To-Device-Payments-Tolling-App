package ca.drdgvhbh.posterminal

import java.math.BigInteger

data class RequestMoneyDTO(
    val tokenAddress: String,
    val requestAmount: BigInteger,
    val requesterAddress: String
)