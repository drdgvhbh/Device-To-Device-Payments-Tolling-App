package ca.drdgvhbh.posterminal

import android.content.ClipboardManager
import android.content.Context
import android.nfc.NfcAdapter
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.terminal_fragment.*
import androidx.databinding.DataBindingUtil
import ca.drdgvhbh.posterminal.databinding.TerminalFragmentBinding
import java.util.*

private const val READER_FLAGS =
    NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK


class TerminalFragment : Fragment() {
    private lateinit var nfcAdapter: NfcAdapter

    private lateinit var posTerminalReader: PosTerminalReader

    private lateinit var viewModel: TerminalViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<TerminalFragmentBinding>(inflater, R.layout.terminal_fragment, container, false)
        viewModel = ViewModelProviders.of(
            this, TerminalViewModelProviderFactory(this.activity!!)
        ).get(
            TerminalViewModel::class.java
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_main_edit -> {
                val alert = AlertDialog.Builder(this.context!!)
                alert.setTitle("Enter your ethereum address")

                // Set an EditText view to get user input
                val input = EditText(this.activity)
                input.setOnFocusChangeListener { textField, hasFocus ->
                    if (hasFocus) {
                        val activity = this.activity!!
                        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = (textField as EditText).text
                        text.replace(0, text.length,clipboard.primaryClip?.getItemAt(0)?.text)
                        Toast.makeText(this.context!!, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                    }
                }
                alert.setView(input)

                alert.setPositiveButton("Ok") { dialog, whichButton ->
                    val value = input.text.toString()
                    viewModel.setEthereumAddress(value)
                }

                alert.setNegativeButton("Cancel") { dialog, which ->
                }
                alert.show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val resources = activity!!.resources
        val rawResource = resources.openRawResource(R.raw.config)
        val properties = Properties()
        properties.load(rawResource)
        posTerminalReader = PosTerminalReader(
            activityContext = activity!!,
            applicationContext = activity!!.applicationContext,
            host = properties.getProperty("BLOCKCHAIN_HOST"),
            port = properties.getProperty("BLOCKCHAIN_PORT"),
            networkID = properties.getProperty("NETWORK_ID"),
            paymentLogicAppUrl = properties.getProperty("PAYMENT_LOGIC_APP_URL"))
        nfcAdapter = NfcAdapter.getDefaultAdapter(this.activity)
        enableReaderMode()

        this.pulsator.start()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.terminal, menu)

        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPause() {
        super.onPause()
        disableReaderMode()
    }

    override fun onResume() {
        super.onResume()
        enableReaderMode()
    }

    private fun enableReaderMode() {
        this.nfcAdapter.enableReaderMode(activity, this.posTerminalReader, READER_FLAGS, null)
    }

    private fun disableReaderMode() {
        this.nfcAdapter.disableReaderMode(activity)
    }
}
