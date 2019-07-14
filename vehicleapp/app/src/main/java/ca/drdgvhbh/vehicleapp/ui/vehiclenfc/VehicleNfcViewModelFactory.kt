package ca.drdgvhbh.vehicleapp.ui.vehiclenfc

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import ca.drdgvhbh.vehicleapp.data.SelectedVehicleDatabase

class VehicleNfcViewModelFactory(private val applicationContext: Context): ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VehicleNfcViewModel::class.java)) {
            val db = Room.databaseBuilder(
                applicationContext,
                SelectedVehicleDatabase::class.java,
                "selected-vehicle-database"
            ).enableMultiInstanceInvalidation().build()
            return VehicleNfcViewModel(db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}