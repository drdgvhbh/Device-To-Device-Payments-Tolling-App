package ca.drdgvhbh.posterminal.data

import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.POST
import java.math.BigInteger

data class PayTollBody(
    val amount: String,
    val vehicleId: String,
    val vehicleOwner: String,
    val tollBooth: String
)

data class PaytollResponse(
    val TransactionHash: String
)

interface PayTollAPI {
    @POST("/")
    fun payToll(@Body body: PayTollBody): Single<PaytollResponse>
}