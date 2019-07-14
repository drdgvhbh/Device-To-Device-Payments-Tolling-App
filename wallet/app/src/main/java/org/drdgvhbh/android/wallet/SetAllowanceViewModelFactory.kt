package org.drdgvhbh.android.wallet

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import java.math.BigInteger

class SetAllowanceViewModelFactory(
        private val activity: FragmentActivity,
        private val allowanceRequestor: String,
        private val credentials: Credentials,
        private val web3j: Web3j,
        private val requestAmount: BigInteger,
        private val contractAddress: String) :
        ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SetAllowanceViewModel::class.java)) {
            return SetAllowanceViewModel(
                    activity = activity,
                    allowanceRequestor = allowanceRequestor,
                    credentials = credentials,
                    web3j = web3j,
                    requestAmountBI = requestAmount,
                    contractAddress = contractAddress
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}