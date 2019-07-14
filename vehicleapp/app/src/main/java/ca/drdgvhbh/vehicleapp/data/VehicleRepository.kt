package ca.drdgvhbh.vehicleapp.data

import com.google.gson.Gson
import ca.drdgvhbh.vehicleapp.contracts.Vehicle as VehicleContract
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.Request
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.ReadonlyTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger

data class Vehicle(
    val plateNumber: String,
    val model: String
)

class VehicleRepository(
    host: String,
    port: String,
    networkID: String
) {

    private val contractAddress: String = VehicleContract.getPreviouslyDeployedAddress(networkID)

    private val web3: Web3j = Web3j.build(HttpService("http://$host:$port"))

    private val httpService = OkHttpClient.Builder().build()


    fun getAllRegisteredVehicles(ownerAddress: String): Single<List<Pair<BigInteger, Vehicle>>> {
        val transactionManager = ReadonlyTransactionManager(web3, ownerAddress)
        val vehicleContract = ca.drdgvhbh.vehicleapp.contracts.Vehicle.load(
            contractAddress,
            web3,
            transactionManager,
            DefaultGasProvider()
        )

        return vehicleContract.registeredVehicles.flowable()
            .map { list ->
                list as MutableList<BigInteger>
            }
            .flatMapIterable { list -> list }
            .flatMap { tokenID ->
                vehicleContract.tokenURI(tokenID).flowable().flatMap { metaDataURL ->
                    val response = httpService.newCall(Request.Builder().url(metaDataURL).build()).execute()
                    Observable.just(response).toFlowable(BackpressureStrategy.ERROR).map {
                        val vehicle = Gson().fromJson<Vehicle>(response.body()?.charStream(), Vehicle::class.java)
                        return@map Pair(tokenID, vehicle)
                    }
                }
            }
            .toList()
    }
}