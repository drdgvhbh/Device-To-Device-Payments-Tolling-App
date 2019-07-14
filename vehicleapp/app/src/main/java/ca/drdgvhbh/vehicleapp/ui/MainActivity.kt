package ca.drdgvhbh.vehicleapp.ui

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import ca.drdgvhbh.vehicleapp.R
import ca.drdgvhbh.vehicleapp.databinding.ActivityMainBinding
import ca.drdgvhbh.vehicleapp.ui.model.Vehicle
import kotlinx.android.synthetic.main.activity_main.*
import org.apache.commons.io.IOUtils
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Bip44WalletUtils
import org.web3j.crypto.Credentials
import org.web3j.crypto.WalletUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {


    private lateinit var requestFileIntent: Intent
    private lateinit var inputPFD: ParcelFileDescriptor

    private lateinit var viewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(
            this, MainActivityViewModelFactory(this, this)
        ).get(
            MainActivityViewModel::class.java
        )

        val binding: ActivityMainBinding = DataBindingUtil.setContentView(this,
            R.layout.activity_main
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        requestFileIntent = Intent(Intent.ACTION_PICK).apply {
            type = "application/json"
        }

        viewModel.vehicles.observe(this, Observer {
            this.vehiclesListView.adapter = VehicleListAdapter(this, it)
        })

        this.vehiclesListView.onItemClickListener = AdapterView.OnItemClickListener { adapter, view, position, _ ->
            val vehicle = this.vehiclesListView.getItemAtPosition(position) as Vehicle
            val bitmap = vehicle.vehicleThumbnail
            val bitmapFile = File(this.cacheDir, "vehicle")
            bitmapFile.createNewFile()
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream)
            val bitmapData = outputStream.toByteArray()
            val fileOutputStream = FileOutputStream(bitmapFile)
            fileOutputStream.write(bitmapData)
            fileOutputStream.flush()
            fileOutputStream.close()

            vehicle.let {
                val intent = Intent(this, VehicleNFCActivity::class.java).apply {
                    putExtra(ARG_ID, it.id)
                    putExtra(ARG_PLATE_NUMBER, it.plateNumber)
                    putExtra(ARG_MODEL, it.model)
                    putExtra(ARG_THUMBNAIL, bitmapFile.absolutePath)
                }
                startActivity(intent)
            }

        }
        // requestFile()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_main_edit -> {
                val alert = AlertDialog.Builder(this)
                alert.setTitle("Enter your ethereum address")

                // Set an EditText view to get user input
                val input = EditText(this)
                input.setOnFocusChangeListener { textField, hasFocus ->
                    if (hasFocus) {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = (textField as EditText).text
                        text.replace(0, text.length,clipboard.primaryClip?.getItemAt(0)?.text)
                        Toast.makeText(this, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                    }
                }
                alert.setView(input)

                alert.setPositiveButton("Ok") { dialog, whichButton ->
                    val value = input.text.toString()
                    viewModel.ethereumAddress.value = value
                }

                alert.setNegativeButton("Cancel") { dialog, which ->
                }
                alert.show()
                return true
            }
            R.id.menu_main_refresh -> {
                viewModel.refresh()
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, returnIntent: Intent?) {
        // If the selection didn't work
        if (resultCode != Activity.RESULT_OK) {
            // Exit without doing anything else
            return
        }
        // Get the file's content URI from the incoming Intent
        returnIntent?.data?.also { returnUri ->
            /*
             * Try to open the file for "read" access using the
             * returned URI. If the file isn't found, write to the
             * error log and return.
             */
            inputPFD = try {
                /*
                 * Get the content resolver instance for this context, and use it
                 * to get a ParcelFileDescriptor for the file.
                 */
                contentResolver.openFileDescriptor(returnUri, "r")
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                Log.e("MainActivity", "File not found.")
                return
            }

            // Get a regular file descriptor for the file
            val fd = inputPFD.fileDescriptor

            val mimeType: String? = returnIntent.data?.let { returnUri ->
                contentResolver.getType(returnUri)
            }

            val inputStream = contentResolver.openInputStream(returnUri)
            val tempFile = File.createTempFile("wallet", "json")
            tempFile.deleteOnExit()
            val out = FileOutputStream(tempFile)
            IOUtils.copy(inputStream, out)
            val credentials = WalletUtils.loadCredentials("", tempFile)
            val address = credentials.address

            val bip44cred = Bip44WalletUtils.loadCredentials("", tempFile)
            val bip44address = bip44cred.address

            val keyPair = bip44cred.ecKeyPair as Bip32ECKeyPair

            val account1 = Credentials.create(Bip32ECKeyPair.deriveKeyPair(keyPair, intArrayOf(0)))
            val derp = account1.address
            Log.d("yolo", credentials.toString())
        }
    }
}
