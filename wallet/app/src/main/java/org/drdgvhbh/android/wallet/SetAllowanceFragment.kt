package org.drdgvhbh.android.wallet

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.drdgvhbh.android.wallet.databinding.SetAllowanceFragmentBinding
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Bip44WalletUtils
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.math.BigInteger
import java.util.*

class SetAllowanceFragment : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(
                allowanceRequestor: String? = "",
                requestAmount: String? = "0",
                contractAddress: String? = "",
                walletAddress: String? = ""
        ): SetAllowanceFragment {
            val fragment = SetAllowanceFragment().apply {
                arguments = Bundle().apply {
                    putString("allowanceRequestor", allowanceRequestor)
                    putString("requestAmount", requestAmount)
                    putString("contractAddress", contractAddress)
                    putString("walletAddress", walletAddress)
                }

            }

            return fragment
        }
    }

    private lateinit var viewModel: SetAllowanceViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<SetAllowanceFragmentBinding>(
                inflater, R.layout.set_allowance_fragment, container, false)

        val allowanceRequestor = arguments!!.getString("allowanceRequestor")!!
        val requestAmount = BigInteger(arguments!!.getString("requestAmount"))
        val contractAddress = arguments!!.getString("contractAddress")!!

        val resources = activity!!.resources
        val rawResource = resources.openRawResource(R.raw.config)
        val properties = Properties()
        properties.load(rawResource)

        val host = properties.getProperty("BLOCKCHAIN_HOST")
        val port = properties.getProperty("BLOCKCHAIN_PORT")

        val web3j = Web3j.build(HttpService("http://$host:$port"))

        val prefs = App.get(this.context!!).prefs
        val mnemonic = prefs.getString("mnemonic", "")
        val rootCredentials = Bip44WalletUtils.loadBip44Credentials(App.PASSWORD, mnemonic)
        // m/44'/60'/0'/0
        val keyPair = rootCredentials.ecKeyPair as Bip32ECKeyPair
        // m/44'/60'/0'/0/0
        val accountCredentials = Credentials.create(Bip32ECKeyPair.deriveKeyPair(keyPair, intArrayOf(0)))

        viewModel = ViewModelProviders.of(
                this, SetAllowanceViewModelFactory(
                activity = activity!!,
                allowanceRequestor = allowanceRequestor,
                requestAmount = requestAmount,
                web3j = web3j,
                contractAddress = contractAddress,
                credentials = accountCredentials
        )
        ).get(
                SetAllowanceViewModel::class.java
        )

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // TODO: Use the ViewModel
    }

}
