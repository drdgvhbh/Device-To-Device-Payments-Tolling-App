package ca.drdgvhbh.vehicleapp.ui.vehiclenfc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.CATEGORY_DEFAULT
import android.content.Intent.CATEGORY_MONKEY
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import ca.drdgvhbh.vehicleapp.R
import ca.drdgvhbh.vehicleapp.databinding.VehicleNfcFragmentBinding
import kotlinx.android.synthetic.main.vehicle_nfc_fragment.*
import java.io.File
import ca.drdgvhbh.vehicleapp.REQUEST_MONEY_INTENT

const val CATEGORY_ERC20_WALLET = "android.intent.category.ERC20_WALLET"

private const val ARG_ID = "id"
private const val ARG_PLATE_NUMBER = "plateNumber"
private const val ARG_MODEL = "model"
private const val ARG_THUMBNAIL = "thumbnail"

class VehicleNfcFragment : Fragment() {
    private val requestMoneyBroadcastReceiver = RequestMoneyBroadcastReceiver()

    private lateinit var viewModel: VehicleNfcViewModel

    private lateinit var bundle: Bundle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            bundle = it
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DataBindingUtil.inflate<VehicleNfcFragmentBinding>(
            inflater, R.layout.vehicle_nfc_fragment, container, false
        )

        viewModel = ViewModelProviders.of(
            this, VehicleNfcViewModelFactory(this.activity!!.applicationContext)
        ).get(
            VehicleNfcViewModel::class.java
        )

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        bundle.let {
            val thumbnailFileURL = it.getString(ARG_THUMBNAIL)
            val thumbnailFile = File(thumbnailFileURL)
            val bitmap = BitmapFactory.decodeFile(thumbnailFile.absolutePath)
            val drawable = BitmapDrawable(context!!.resources, bitmap)

            viewModel.setID(it.getString(ARG_ID)!!)
            viewModel.setPlateNumber(it.getString(ARG_PLATE_NUMBER)!!)
            viewModel.setModel(it.getString(ARG_MODEL)!!)
            viewModel.setDrawable(drawable)

        }

        val pulsator = this.pulsator
        pulsator.count
        pulsator.start()
        // TODO: Use the ViewModel
    }

    override fun onStart() {
        super.onStart()
        val hceNotificationsFilter = IntentFilter().apply {
            addAction(REQUEST_MONEY_INTENT)
        }
        activity!!.registerReceiver(requestMoneyBroadcastReceiver, hceNotificationsFilter)
    }

    override fun onStop() {
        super.onStop()
        activity!!.unregisterReceiver(requestMoneyBroadcastReceiver)
    }

    companion object {
        @JvmStatic
        fun newInstance(id: String, plateNumber: String, model: String, thumbNailFileURL: String) =
            VehicleNfcFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ID, id)
                    putSerializable(ARG_PLATE_NUMBER, plateNumber)
                    putSerializable(ARG_MODEL, model)
                    putSerializable(ARG_THUMBNAIL, thumbNailFileURL)
                }
            }
    }

    private inner class RequestMoneyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {

            val walletIntent = Intent(Intent.ACTION_PICK_ACTIVITY).apply {
                addCategory(CATEGORY_ERC20_WALLET)
                putExtras(intent)
                putExtra("TYPE", "ALLOWANCE")
            }
            startActivityForResult(walletIntent, 0)
        }

    }


}
