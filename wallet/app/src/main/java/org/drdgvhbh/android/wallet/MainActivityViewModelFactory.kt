package org.drdgvhbh.android.wallet

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.web3j.protocol.Web3j

class MainActivityViewModelFactory(
        private val activity: AppCompatActivity,
        private val tollTokenAddress: String,
        private val web3j: Web3j) :
        ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainActivityViewModel::class.java)) {
            return MainActivityViewModel(
                    context = activity,
                    tollTokenAddress = tollTokenAddress,
                    web3j = web3j
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}