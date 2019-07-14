package ca.drdgvhbh.vehicleapp.ui.vehiclenfc

import android.graphics.drawable.BitmapDrawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ca.drdgvhbh.vehicleapp.data.SelectedVehicle
import ca.drdgvhbh.vehicleapp.data.SelectedVehicleDatabase
import io.reactivex.schedulers.Schedulers

class VehicleNfcViewModel(
    private val selectedVehicleDatabase: SelectedVehicleDatabase
) : ViewModel() {
    private val _id = MutableLiveData<String>()
    val id: LiveData<String> = _id

    private val _plateNumber = MutableLiveData<String>()
    val plateNumber: LiveData<String> = _plateNumber

    private val _model = MutableLiveData<String>()
    val model: LiveData<String> = _model

    private val _drawable = MutableLiveData<BitmapDrawable>()
    val drawable: LiveData<BitmapDrawable> = _drawable

    fun setID(id: String) {
        _id.value = id
        selectedVehicleDatabase.selectedVehicleDao().insert(SelectedVehicle(id))
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    fun setPlateNumber(plateNumber: String) {
        _plateNumber.value = plateNumber
    }

    fun setModel(model: String) {
        _model.value = model
    }

    fun setDrawable(drawable: BitmapDrawable) {
        _drawable.value = drawable
    }
}

