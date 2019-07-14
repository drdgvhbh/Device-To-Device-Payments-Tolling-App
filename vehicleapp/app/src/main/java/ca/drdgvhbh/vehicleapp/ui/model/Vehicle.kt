package ca.drdgvhbh.vehicleapp.ui.model

import android.graphics.Bitmap
import android.content.Context
import android.graphics.drawable.BitmapDrawable


class Vehicle(
    val id: String,
    val plateNumber: String,
    val model: String,
    val vehicleThumbnail: Bitmap,
    context: Context
)  {
    val drawable: BitmapDrawable = BitmapDrawable(context.resources, vehicleThumbnail)
}