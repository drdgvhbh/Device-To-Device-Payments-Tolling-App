package org.drdgvhbh.android.wallet;

import  org.drdgvhbh.android.wallet.databinding.ActivityMainBinding
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.Intent.ACTION_PICK_ACTIVITY
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.textfield.TextInputEditText
import org.web3j.crypto.Bip39Wallet
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.util.*

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var viewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        val resources = this.resources
        val rawResource = resources.openRawResource(R.raw.config)
        val properties = Properties()
        properties.load(rawResource)

        val tolTokenAddress = properties.getProperty("TOL_TOKEN_ADDRESS")
        val host = properties.getProperty("BLOCKCHAIN_HOST")
        val port = properties.getProperty("BLOCKCHAIN_PORT")

        val web3j = Web3j.build(HttpService("http://$host:$port"))
        viewModel = ViewModelProviders.of(
                this,
                MainActivityViewModelFactory(
                        activity = this,
                        tollTokenAddress = tolTokenAddress,
                        web3j = web3j
                )
        ).get(
                MainActivityViewModel::class.java
        )

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        setAllowance(this.intent)
    }

    private fun setAllowance(intent: Intent?) {
        if (intent?.extras != null) {
            if (intent.action != ACTION_PICK_ACTIVITY) {
                return
            }
            if (!intent.hasCategory(CATEGORY_ERC20_WALLET)) {
                return
            }
            val extras = intent.extras!!
            val requestAmount = extras.getString("requestAmount")
            val requesterAddress = extras.getString("requesterAddress")
            val tokenAddress = extras.getString("tokenAddress")
            val type = extras.getString("TYPE")

            if (type!! != "ALLOWANCE") {
                finish()
            }

            val fragment = SetAllowanceFragment.newInstance(
                    requesterAddress,
                    requestAmount,
                    tokenAddress,
                    viewModel.address.value
            );
            fragment.show(supportFragmentManager, fragment.tag)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_main_refresh -> {
                viewModel.refresh()
                return true
            }
            R.id.menu_main_import -> {
                showImportDialog()
                return true
            }
            R.id.menu_main_reset -> {
                viewModel.reset()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showImportDialog() {
        val dialog = ImportWalletDialog(this)
        dialog.show()
    }

    @Override
    override fun onClick(v: View) {
        if (v is TextInputEditText) {
            val clipboard =
                    getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val text = v.text
            clipboard.primaryClip = ClipData.newPlainText(text, text)
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    fun onWalletImported(wallet: Bip39Wallet) {
        viewModel.setWallet(wallet)
    }

    companion object {
        private const val CATEGORY_ERC20_WALLET = "android.intent.category.ERC20_WALLET"
    }
}


