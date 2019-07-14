package ca.drdgvhbh.vehicleapp.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Context.MODE_PRIVATE
import android.graphics.BitmapFactory
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.lifecycle.*
import ca.drdgvhbh.vehicleapp.R
import ca.drdgvhbh.vehicleapp.data.SelectedVehicleDatabase
import ca.drdgvhbh.vehicleapp.data.VehicleRepository
import ca.drdgvhbh.vehicleapp.ui.model.Vehicle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import java.math.BigInteger

private const val ETHER_ADDRESS_KEY = "ether-account"

class MainActivityViewModel(
    owner: LifecycleOwner,
    private val activity: Activity,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {
    val ethereumAddress: MutableLiveData<String> = MutableLiveData()

    private val _ethereumAddressEllipsis = MutableLiveData<String>()

    private val disposables = CompositeDisposable()

    private val _vehicles = MutableLiveData<List<Vehicle>>()
    val vehicles: LiveData<List<Vehicle>> = _vehicles

    val ethereumAddressEllipsis: LiveData<String> = _ethereumAddressEllipsis

     init {
        val preferences = activity.getPreferences(MODE_PRIVATE)
        ethereumAddress.value = preferences.getString(ETHER_ADDRESS_KEY, "0x0000000000000000000000000000000000000000")

        val ethereumAddressObserver = Observer<String> { address ->
            _ethereumAddressEllipsis.value =
                address.slice(IntRange(0, 5)) + "..." + address.slice(IntRange(address.length - 5, address.length - 1))
            preferences.edit().putString(ETHER_ADDRESS_KEY, address).apply()
            fetchRegisteredCars()
        }
        ethereumAddress.observe(owner, ethereumAddressObserver)

        val ethereumAddressStr = ethereumAddress.value!!
        val ethereumAddressWithoutPrefix = ethereumAddressStr.slice(IntRange(2, ethereumAddressStr.length -1 ));
        val etherAddressNumeric = BigInteger(ethereumAddressWithoutPrefix, 16)
        if (etherAddressNumeric >= BigInteger.ZERO) {
            fetchRegisteredCars()
        }

    }

    fun refresh() {
        fetchRegisteredCars();
    }

    private fun fetchRegisteredCars() {
        val subscription = vehicleRepository.getAllRegisteredVehicles(ethereumAddress.value!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { list ->
                list.map {
                    val tokenId = it.first
                    val vehicle = it.second
                    val carImage = BitmapFactory.decodeResource(activity.resources, R.drawable.swag_car)

                    Vehicle(
                        id = tokenId.toString(10),
                        plateNumber = vehicle.plateNumber,
                        model = vehicle.model,
                        vehicleThumbnail = carImage,
                        context = this.activity
                    )
                }
            }
            .subscribe({ vehicleList ->
                _vehicles.value = vehicleList
            }, { err ->
                Log.e(this.javaClass::getSimpleName.toString(), err.message)
            })

        subscription.addTo(this.disposables)
    }

    fun copyEthereumAddressToClipboard() {
        val clipboard = activity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val text = ethereumAddress.value
        clipboard.primaryClip = ClipData.newPlainText(text, text)

        val toast = Toast.makeText(activity, "$text\n\nCopied to clipboard", Toast.LENGTH_SHORT)
        toast.show()

    }

}