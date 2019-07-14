package ca.drdgvhbh.posterminal

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import androidx.room.Room
import at.favre.lib.bytes.Bytes
import ca.drdgvhbh.posterminal.contracts.TollToken
import ca.drdgvhbh.posterminal.contracts.Vehicle
import ca.drdgvhbh.posterminal.data.PayTollAPI
import ca.drdgvhbh.posterminal.data.PayTollBody
import ca.drdgvhbh.posterminal.data.TollingBoothAddressDatabase
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.gson.Gson
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.tx.ReadonlyTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigInteger
import java.nio.charset.Charset

private const val TOLLING_CARD_AID = "F466112058"
private const val SELECT_APDU_HEADER = "00A40400"
private const val PUT_DATA_HEADER = "00DA02FF"
private const val REQUEST_AMOUNT_TAG = "01"

private val selectApduCommand: ByteArray = (fun(): ByteArray {
    val length = String.format("%02X", Bytes.parseHex(TOLLING_CARD_AID).length())
    val hexString =
        "$SELECT_APDU_HEADER$length$TOLLING_CARD_AID"
    return Bytes.parseHex(hexString).array()
})()

private fun putDataRequestAmountCommand(data: RequestMoneyDTO): ByteArray {
    val serializedData = Gson().toJson(data)
    val requestAmountHex = Bytes.from(serializedData).encodeHex()
    val length = String.format("%02X", Bytes.parseHex(requestAmountHex).length())
    val hexString =
        "$PUT_DATA_HEADER$REQUEST_AMOUNT_TAG$length$requestAmountHex"
    return Bytes.parseHex(hexString).array()
}

private val SELECT_OK_SW = Bytes.parseHex("9000").array()

class PosTerminalReader(
    private val paymentLogicAppUrl: String,
    private val activityContext: Context,
    applicationContext: Context,
    host: String,
    port: String,
    networkID: String
) : NfcAdapter.ReaderCallback {
    private val tollTokenContractAddress: String = TollToken.getPreviouslyDeployedAddress(networkID)
    private val vehicleTokenContractAddress: String = Vehicle.getPreviouslyDeployedAddress(networkID)

    private val web3: Web3j = Web3j.build(HttpService("http://$host:$port"))

    private val compositeDispoables = CompositeDisposable()

    private var awaitingResponseDialog: SweetAlertDialog? = null


    private val retrofit = Retrofit.Builder()
        .baseUrl("https://prod-23.centralus.logic.azure.com")
        .client(OkHttpClient.Builder().addInterceptor { chain ->
            val original = chain.request()

            val url = HttpUrl.parse(paymentLogicAppUrl)!!.url()
            val requestBuilder = original.newBuilder()
                .url(url)

            val request = requestBuilder.build()

            chain.proceed(request)
        }.build())
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()

    private val payTollAPI = retrofit.create(PayTollAPI::class.java)

    private val db = Room.databaseBuilder(
        applicationContext,
        TollingBoothAddressDatabase::class.java,
        "tolling-booth-address-database"
    ).enableMultiInstanceInvalidation().build()

    @SuppressLint("CheckResult")
    override fun onTagDiscovered(tag: Tag?) {
        val isoDep = IsoDep.get(tag)
        isoDep.connect()

        var result = isoDep.transceive(selectApduCommand)
        var statusCode = result.slice(IntRange(result.size - 2, result.size - 1)).toByteArray()

        if (!statusCode.contentEquals(SELECT_OK_SW)) {
            return
        }
        val vehicleIdentifier =
            String(result.slice(IntRange(0, result.size - 3)).toByteArray(), Charset.defaultCharset())


        if (awaitingResponseDialog != null) {
            awaitingResponseDialog!!.dismiss()
        }
        Observable.just(Unit).subscribeOn(Schedulers.io()).subscribe {
            compositeDispoables.clear()
        }
        val subscription = db.tollingBoothAddressDao()
            .get()
            .flatMap { tollingBooth ->
                val transactionManager = ReadonlyTransactionManager(web3, tollingBooth.address)
                val tollContract =
                    TollToken.load(tollTokenContractAddress, web3, transactionManager, DefaultGasProvider())

                tollContract.isTollingBooth(tollingBooth.address)
                    .flowable()
                    .filter { isTollingBooth -> isTollingBooth }
                    .firstOrError()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError {
                        SweetAlertDialog(activityContext, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Payment Failed")
                            .setContentText("This tolling booth is not registered.")
                            .show()
                    }
                    .flatMap {
                        tollContract.tollAmount()
                            .flowable()
                            .firstOrError()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .flatMap { tollAmount ->
                                val data = RequestMoneyDTO(
                                    tokenAddress = tollTokenContractAddress,
                                    requestAmount = tollAmount,
                                    requesterAddress = tollingBooth.address
                                )
                                val cmd = putDataRequestAmountCommand(data)
                                result = isoDep.transceive(cmd)
                                statusCode = result.slice(IntRange(result.size - 2, result.size - 1)).toByteArray()
                                if (statusCode.contentEquals(SELECT_OK_SW)) {
                                    awaitingResponseDialog =
                                        SweetAlertDialog(activityContext, SweetAlertDialog.PROGRESS_TYPE).apply {
                                            progressHelper.barColor = Color.parseColor("#2196f3")
                                            titleText = "Awaiting Request Approval"
                                            contentText = "Tap again to cancel/restart"
                                            setCancelable(false)
                                        }
                                    awaitingResponseDialog?.show()

                                    val vehicleContract = Vehicle.load(
                                        vehicleTokenContractAddress, web3, transactionManager, DefaultGasProvider()
                                    )

                                    vehicleContract.ownerOf(BigInteger(vehicleIdentifier))
                                        .flowable()
                                        .firstOrError()
                                        .subscribeOn(Schedulers.io())
                                        .flatMap { ownerAddress ->
                                            tollContract
                                                .approvalEventFlowable(
                                                    DefaultBlockParameterName.LATEST, DefaultBlockParameterName.LATEST
                                                )
                                                .filter {
                                                    it.spender == tollingBooth.address && it.owner == ownerAddress && it.value == tollAmount
                                                }.firstOrError().observeOn(AndroidSchedulers.mainThread()).map {
                                                    awaitingResponseDialog?.dismiss()
                                                    val processingDialog = SweetAlertDialog(
                                                        activityContext,
                                                        SweetAlertDialog.PROGRESS_TYPE
                                                    ).apply {
                                                        titleText = "Processing Payment"
                                                        setCancelable(false)
                                                    }
                                                    processingDialog.show()
                                                    payTollAPI.payToll(
                                                        PayTollBody(
                                                            tollAmount.toString(10),
                                                            vehicleIdentifier,
                                                            ownerAddress,
                                                            tollingBooth.address
                                                        )
                                                    )
                                                        .subscribeOn(Schedulers.io())
                                                        .observeOn(AndroidSchedulers.mainThread())
                                                        .subscribe({ tx ->
                                                            processingDialog.dismiss()
                                                            SweetAlertDialog(
                                                                activityContext,
                                                                SweetAlertDialog.SUCCESS_TYPE
                                                            )
                                                                .setTitleText("Payment Successful")
                                                                .setContentText("Tx Hash: ${tx.TransactionHash}")
                                                                .show()
                                                        }, { err ->
                                                            SweetAlertDialog(
                                                                activityContext,
                                                                SweetAlertDialog.ERROR_TYPE
                                                            )
                                                                .setTitleText("Payment Failed")
                                                                .setContentText(err.message)
                                                                .show()
                                                            processingDialog.dismiss()
                                                        })
                                                }
                                        }
                                } else {
                                    Observable.just(Unit).firstOrError()
                                }
                            }
                    }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({}, { err ->
                SweetAlertDialog(activityContext, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Oops...")
                    .setContentText(err.message)
                    .show()
            })

        compositeDispoables.add(subscription)
    }
}