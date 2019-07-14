package org.drdgvhbh.android.wallet

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel;
import com.roger.catloadinglibrary.CatLoadingView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.drdgvhbh.android.wallet.contracts.CodegenERC20
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.tx.ClientTransactionManager
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger

private fun computeDecimalString(value: BigInteger, decimals: Int): String {
    val divisionResult = value.div(BigInteger("10").pow(decimals))

    return "%.2f".format(divisionResult.toFloat())
}

class SetAllowanceViewModel(
        private val activity: FragmentActivity,
        allowanceRequestor: String,
        credentials: Credentials,
        web3j: Web3j,
        private val requestAmountBI: BigInteger,
        contractAddress: String
) : ViewModel() {
    private val contract: CodegenERC20


    private val _allowanceRequestor = MutableLiveData<String>()
    val allowanceRequestor: LiveData<String> = _allowanceRequestor

    private val _token = MutableLiveData<String>()
    val token: LiveData<String> = _token

    private val _currentBalance = MutableLiveData<String>()
    val currentBalance: LiveData<String> = _currentBalance

    private val _requestAmount = MutableLiveData<String>()
    val requestAmount: LiveData<String> = _requestAmount

    private val compositeDisposables = CompositeDisposable()



    init {
        this._allowanceRequestor.value = allowanceRequestor
        this._requestAmount.value = "0.0"
        this._currentBalance.value = "0.0"

        val transactionManager = RawTransactionManager(web3j, credentials)
        contract = CodegenERC20.load(
                contractAddress,
                web3j,
                transactionManager,
                DefaultGasProvider()
        )

        compositeDisposables.add(
                contract.decimals().flowable()
                        .map { decimals ->
                            _requestAmount.postValue(
                                    computeDecimalString(requestAmountBI, decimals.toInt()))

                            decimals
                        }
                        .subscribeOn(Schedulers.io())
                        .subscribe { decimals ->
                            contract.balanceOf(credentials.address).flowable()
                                    .subscribeOn(Schedulers.io())
                                    .subscribe { currentBalance ->
                                        _currentBalance.postValue(
                                                computeDecimalString(currentBalance, decimals.toInt())
                                        )
                                    }
                        })

        compositeDisposables.add(
                Observable.zip(
                        contract.name().flowable().toObservable(),
                        contract.symbol().flowable().toObservable(), BiFunction<String, String, Pair<String, String>> { name, symbol ->
                    Pair(name, symbol)
                })
                        .subscribeOn(Schedulers.io())
                        .subscribe { nameAndSymbol ->
                            _token.postValue("${nameAndSymbol.first} (${nameAndSymbol.second})")
                        }
        )
    }

    fun onReject() {
        activity.setResult(AppCompatActivity.RESULT_CANCELED)
        activity.finish()
    }

    fun onAccept() {
        val catLoadingView = CatLoadingView()
        catLoadingView.isCancelable = false
        catLoadingView.setText("Processing");
        catLoadingView.show(activity.supportFragmentManager, catLoadingView.javaClass.name)
        compositeDisposables.add(contract
                .approve(allowanceRequestor.value, requestAmountBI)
                .flowable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    activity.setResult(AppCompatActivity.RESULT_OK)
                    activity.finish()
                    catLoadingView.dismiss()
                })
    }
}
