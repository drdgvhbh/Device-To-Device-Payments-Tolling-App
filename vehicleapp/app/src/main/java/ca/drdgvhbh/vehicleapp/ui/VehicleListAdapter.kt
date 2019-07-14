package ca.drdgvhbh.vehicleapp.ui

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.databinding.DataBindingUtil
import ca.drdgvhbh.vehicleapp.R
import ca.drdgvhbh.vehicleapp.databinding.RowVehicleItemBinding
import ca.drdgvhbh.vehicleapp.ui.model.Vehicle

class VehicleListAdapter(
    private val activity: Activity,
    private val vehicles: List<Vehicle>
) : BaseAdapter() {
    override fun getItem(position: Int): Any {
        return vehicles[position]
    }

    override fun getItemId(position: Int): Long = 0

    override fun getCount(): Int = vehicles.count()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var binding: RowVehicleItemBinding? = null
        if (convertView == null) {
            val newConvertView = LayoutInflater.from(activity).inflate(
                R.layout.row_vehicle_item,
                null
            )
            binding = DataBindingUtil.bind(newConvertView)
            newConvertView.tag = binding
        } else {
            binding = convertView.tag as RowVehicleItemBinding
        }
        binding!!.vehicle = vehicles[position]

        return binding.root
    }
}