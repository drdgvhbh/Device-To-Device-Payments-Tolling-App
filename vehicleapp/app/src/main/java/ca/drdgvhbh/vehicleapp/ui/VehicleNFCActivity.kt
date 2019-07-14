package ca.drdgvhbh.vehicleapp.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import ca.drdgvhbh.vehicleapp.R
import ca.drdgvhbh.vehicleapp.ui.vehiclenfc.VehicleNfcFragment

const val ARG_ID = "id"
const val ARG_PLATE_NUMBER = "plateNumber"
const val ARG_MODEL = "model"
const val ARG_THUMBNAIL = "thumbnail"

class VehicleNFCActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vehicle_nfc_activity)
        if (savedInstanceState == null) {
            intent?.let {
                val id = it.getStringExtra(ARG_ID)
                val plateNumber = it.getStringExtra(ARG_PLATE_NUMBER)
                val model = it.getStringExtra(ARG_MODEL)
                val thumbnail = it.getStringExtra(ARG_THUMBNAIL)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container, VehicleNfcFragment.newInstance(id, plateNumber, model, thumbnail))
                    .commitNow()
            }

        }
    }

}
