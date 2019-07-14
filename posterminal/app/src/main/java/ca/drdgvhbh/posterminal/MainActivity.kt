package ca.drdgvhbh.posterminal

import android.nfc.NfcAdapter
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private lateinit var  currentFragment: Fragment

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_terminal -> {
                currentFragment = TerminalFragment()
                setFragment()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        currentFragment = TerminalFragment()
        setFragment()

        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

    }

    private fun setFragment() {
        val tx = supportFragmentManager.beginTransaction()
        tx.replace(R.id.activity_main_container, currentFragment)
        tx.commit()
    }


}
