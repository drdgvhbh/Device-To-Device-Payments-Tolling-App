package ca.drdgvhbh.posterminal

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import ca.drdgvhbh.posterminal.data.TollingBoothAddressDatabase

class TerminalViewModelProviderFactory(private val activity: Activity) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TerminalViewModel::class.java)) {
            val db = Room.databaseBuilder(
                activity.applicationContext,
                TollingBoothAddressDatabase::class.java,
                "tolling-booth-address-database"
            ).enableMultiInstanceInvalidation().build()
            return TerminalViewModel(activity, db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}