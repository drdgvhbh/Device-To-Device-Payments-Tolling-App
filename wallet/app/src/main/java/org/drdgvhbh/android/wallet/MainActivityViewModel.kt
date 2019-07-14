package org.drdgvhbh.android.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.android.material.textfield.TextInputEditText
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.drdgvhbh.android.wallet.contracts.CodegenERC20
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Bip39Wallet
import org.web3j.crypto.Bip44WalletUtils
import org.web3j.crypto.Credentials
import org.web3j.protocol.core.DefaultBlockParameterName
import java.io.File
import java.io.IOException
import org.web3j.crypto.CipherException
import org.web3j.protocol.Web3j
import org.web3j.tx.ReadonlyTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigDecimal
import java.math.BigInteger

private fun computeDecimalString(value: BigInteger, decimals: Int): String {
    val divisionResult = value.toBigDecimal().divide(
            BigDecimal("10.0").pow(decimals))

    return divisionResult.toEngineeringString()
}

class MainActivityViewModel(
        private val context: Context,
        private val tollTokenAddress: String,
        private val web3j: Web3j
): ViewModel() {
    private var wallet: Bip39Wallet? = null

    private val _mnemonic = MutableLiveData<String>()
    val mnemonic: LiveData<String> = _mnemonic

    private val _address = MutableLiveData<String>()
    val address: LiveData<String> = _address

    private val _ethBalance = MutableLiveData<String>()
    val ethBalance: LiveData<String> = _ethBalance

    private val _tollTokenBalance = MutableLiveData<String>()
    val tollTokenBalance: LiveData<String> = _tollTokenBalance

    private val compositeDisposables = CompositeDisposable()

    private lateinit var contract: CodegenERC20

    init {
        load()
    }

    fun setWallet(wallet: Bip39Wallet) {
        load(wallet)
    }

    fun refresh() {
        compositeDisposables
                .add(retrieveBalances(_address.value!!)
                .subscribeOn(Schedulers.io())
                .subscribe(
                    {},
                    { err ->
                        SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE).apply {
                            titleText = "Oops..."
                            contentText = err.message
                        }
                    }))
        Toast.makeText(context, "Refreshed", Toast.LENGTH_SHORT).show()

    }

    fun reset() {
        val app = App.get(context)
        val prefs = app.prefs

        prefs.edit().clear().apply()
        load()
    }

    fun copyToClipboard(v: View) {
        if (v is TextInputEditText) {
            val clipboard =
                    context.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
            val text = v.text
            clipboard.primaryClip = ClipData.newPlainText(text, text)
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    fun load(existingWallet: Bip39Wallet? = null) {
        val processingDialog =  SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE).apply {
            titleText = "Loading Wallet"
            contentText = "This may take some time on older phones. Please be patient!"
            setCancelable(false)
        }
        processingDialog.show()
        _mnemonic.value = ""
        _address.value = ""
        _ethBalance.value = ""
        _tollTokenBalance.value = ""

        val walletObs = if (existingWallet != null) {
            Observable.just(existingWallet)
        } else {
            getWallet()
        }

        compositeDisposables.add((walletObs)
                .map {
                    wallet = it
                    val mnemonic = it.mnemonic
                    _mnemonic.postValue(mnemonic)
                    val credentials = Bip44WalletUtils.loadBip44Credentials(
                            App.PASSWORD, mnemonic)
                    val keyPair = credentials.ecKeyPair as Bip32ECKeyPair
                    val hdWalletCredentials = Credentials.create(
                            Bip32ECKeyPair.deriveKeyPair(keyPair, arrayOf(0).toIntArray()))
                    _address.postValue(hdWalletCredentials.address)

                    return@map hdWalletCredentials.address
                }
                .flatMap { address ->
                    contract = CodegenERC20.load(
                            tollTokenAddress,
                            web3j,
                            ReadonlyTransactionManager(web3j, address),
                            DefaultGasProvider()
                    )
                    retrieveBalances(address)
                }
                .subscribeOn(Schedulers.io())
                .subscribe({
                    processingDialog.dismiss()
                }, { err ->
                    SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE).apply {
                        titleText = "Oops..."
                        contentText = err.message
                    }
                }))
    }

    private fun retrieveBalances(address: String): Observable<Unit> {
        val web3j = App.get(context).web3j
        return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).flowable().map {
            val ethBalance = computeDecimalString(it.balance, 18)
            _ethBalance.postValue(ethBalance)
        }.toObservable().mergeWith(contract.balanceOf(address).flowable().map {balance ->
            val tolTokenBalance = computeDecimalString(balance, 18)
            _tollTokenBalance.postValue(tolTokenBalance)
        }.toObservable())

    }

    private fun getWallet(): Observable<Bip39Wallet> {
        return Observable.just(0).flatMap {
            val app = App.get(context)
            val dir = app.filesDir
            val prefs = app.prefs
            val filename = prefs.getString("filename", "")
            val mnemonic = prefs.getString("mnemonic", "")
            if (TextUtils.isEmpty(filename) || TextUtils.isEmpty(mnemonic)) {
                return@flatMap generateWallet(dir, prefs)
            }

            return@flatMap Observable.just(Bip39Wallet(filename, mnemonic))
        }
    }

    @Throws(CipherException::class, IOException::class)
    private fun generateWallet(dir: File, prefs: SharedPreferences): Observable<Bip39Wallet> {
        return Observable.just(0).map {
            val wallet = Bip44WalletUtils.generateBip44Wallet(App.PASSWORD, dir)
            val file = File(dir, wallet.filename)
            if (!file.exists()) {
                throw IOException("Failed to generate wallet")
            }

            val editor = prefs.edit()
            editor.putString("filename", wallet.filename)
            editor.putString("mnemonic", wallet.mnemonic)
            editor.apply()

            return@map wallet
        }

    }
}