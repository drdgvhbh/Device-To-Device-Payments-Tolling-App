package ca.drdgvhbh.vehicleapp.ui

import android.app.Activity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import ca.drdgvhbh.vehicleapp.R
import ca.drdgvhbh.vehicleapp.data.SelectedVehicleDatabase
import ca.drdgvhbh.vehicleapp.data.VehicleRepository
import java.util.*

class MainActivityViewModelFactory(private val owner: LifecycleOwner, private val activity: Activity) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainActivityViewModel::class.java)) {
            val resources = activity.resources
            val rawResource = resources.openRawResource(R.raw.config)
            val properties = Properties()
            properties.load(rawResource)
            return MainActivityViewModel(owner, activity, VehicleRepository(
                properties.getProperty("BLOCKCHAIN_HOST"),
                properties.getProperty("BLOCKCHAIN_PORT"),
                properties.getProperty("NETWORK_ID")
            )) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}