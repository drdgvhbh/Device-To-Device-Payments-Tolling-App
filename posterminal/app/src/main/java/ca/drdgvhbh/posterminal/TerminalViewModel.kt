package ca.drdgvhbh.posterminal

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.NfcEvent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ca.drdgvhbh.posterminal.contracts.TollToken
import ca.drdgvhbh.posterminal.data.TollingBoothAddress
import ca.drdgvhbh.posterminal.data.TollingBoothAddressDatabase
import cn.pedant.SweetAlert.SweetAlertDialog
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.ReadonlyTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import java.util.*

private const val ETHER_ADDRESS_KEY = "ether-account"

class TerminalViewModel(
    private val activity: Activity,
    private val tollingBoothAddressDatabase: TollingBoothAddressDatabase
) : ViewModel() {
    private val _ethereumAddress = MutableLiveData<String>()
    val ethereumAddress: LiveData<String> = _ethereumAddress

    private val tollTokenContractAddress: String

    private val web3: Web3j


    init {
        val resources = activity.resources
        val rawResource = resources.openRawResource(R.raw.config)
        val properties = Properties()
        properties.load(rawResource)

        val host = properties.getProperty("BLOCKCHAIN_HOST")
        val port = properties.getProperty("BLOCKCHAIN_PORT")
        val networkID = properties.getProperty("NETWORK_ID")

        tollTokenContractAddress = TollToken.getPreviouslyDeployedAddress(networkID)
        web3 = Web3j.build(HttpService("http://$host:$port"))

        val preferences = activity.getPreferences(Context.MODE_PRIVATE)
        setEthereumAddress(preferences.getString(ETHER_ADDRESS_KEY, "0x0000000000000000000000000000000000000000")!!)


    }


    @SuppressLint("CheckResult")
    fun setEthereumAddress(address: String) {
        this._ethereumAddress.value = address
        val preferences = activity.getPreferences(Context.MODE_PRIVATE)
        preferences.edit().putString(ETHER_ADDRESS_KEY, address).apply()
        tollingBoothAddressDatabase.tollingBoothAddressDao()
            .insert(TollingBoothAddress(address))
            .toSingleDefault(Unit)
            .flatMap {
                checkIfAddressIsATollingBooth(address)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ isAddressATollingBooth ->
                if (!isAddressATollingBooth) {
                    warnUserThatAddressIsNotATollingBooth()
                }
            }, { err ->
                Log.e("?", err.message, err)
            })
    }

    fun copyEthereumAddressToClipboard() {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = ethereumAddress.value
        clipboard.primaryClip = ClipData.newPlainText(text, text)

        val toast = Toast.makeText(activity, "$text\n\nCopied to clipboard", Toast.LENGTH_SHORT)
        toast.show()
    }

    private fun checkIfAddressIsATollingBooth(address: String): Single<Boolean> {
        val transactionManager = ReadonlyTransactionManager(web3, address)
        val tollContract =
            TollToken.load(tollTokenContractAddress, web3, transactionManager, DefaultGasProvider())

        return tollContract.isTollingBooth(address).flowable().toObservable().firstOrError()
    }

    private fun warnUserThatAddressIsNotATollingBooth() {
        SweetAlertDialog(activity, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("Address is not a toll booth")
            .setContentText("You should register this ethereum address in the webapp first")
            .show()
    }
}
