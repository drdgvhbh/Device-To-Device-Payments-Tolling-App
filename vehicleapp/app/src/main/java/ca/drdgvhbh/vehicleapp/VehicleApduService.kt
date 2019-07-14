package ca.drdgvhbh.vehicleapp

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import androidx.room.Room
import at.favre.lib.bytes.Bytes
import ca.drdgvhbh.vehicleapp.data.RequestMoneyDTO
import ca.drdgvhbh.vehicleapp.data.SelectedVehicleDatabase
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.nio.charset.Charset
import java.util.*
import android.content.IntentFilter



private const val TOLLING_CARD_AID = "F466112058"
private const val SELECT_APDU_HEADER = "00A40400"
private const val PUT_DATA_HEADER = "00DA02FF"
private const val REQUEST_AMOUNT_TAG = "01"

private val selectApduCommand: ByteArray = (fun(): ByteArray {
    val hexString = "$SELECT_APDU_HEADER${String.format("%02X", Bytes.parseHex(TOLLING_CARD_AID).length())}$TOLLING_CARD_AID"
    return Bytes.parseHex(hexString).array()
})()

private val UNKNOWN_CMD_SW = Bytes.parseHex("0000").array()
private val SELECT_OK_SW = Bytes.parseHex("9000").array()

const val REQUEST_MONEY_INTENT = "ca.drdgvhbh.vehicleapp.action.REQUEST_MONEY_INTENT"

class VehicleApduService : HostApduService() {
    private lateinit var db: SelectedVehicleDatabase

    override fun onCreate() {
        super.onCreate()

        db = Room.databaseBuilder(
            applicationContext,
            SelectedVehicleDatabase::class.java,
            "selected-vehicle-database"
        ).enableMultiInstanceInvalidation().build()
    }

    override fun onDeactivated(reason: Int) {
    }

    @SuppressLint("CheckResult")
    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray? {
        val header = Bytes.from(commandApdu.slice(IntRange(0, 3)))
        if (Arrays.equals(selectApduCommand, commandApdu)) {
            this.db.selectedVehicleDao().get()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe { it ->
                    val vehicleIDAsBytes = it?.id?.toByteArray()!!
                    sendResponseApdu(byteArrayOf(*vehicleIDAsBytes, *SELECT_OK_SW))
                }

            return null
        } else if (header == Bytes.parseHex(PUT_DATA_HEADER)) {
            val tag = Bytes.from(commandApdu.slice(IntRange(4, 4)))
            val length = Bytes.from(commandApdu.slice(IntRange(5, 5))).encodeHex().toInt(16)
            if (tag == Bytes.parseHex(REQUEST_AMOUNT_TAG)) {
                if (length > 0) {
                    val data = Bytes.from(commandApdu.slice(IntRange(6, 6 + length - 1))).encodeUtf8()
                    val dto = Gson().fromJson<RequestMoneyDTO>(data, RequestMoneyDTO::class.java)
                    Log.d("grief", data)

                    val intent = Intent(REQUEST_MONEY_INTENT)
                    intent.putExtra("requestAmount", dto.requestAmount.toString(10))
                    intent.putExtra("requesterAddress", dto.requesterAddress)
                    intent.putExtra("tokenAddress", dto.tokenAddress)
                    this.sendBroadcast(intent)

                    return byteArrayOf(*SELECT_OK_SW)
                }
            }
        }

        return UNKNOWN_CMD_SW
    }
}