package com.example.contactsv4

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Thème clair imposé : les couleurs de texte proposées (noir, bleu...) restent lisibles
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.menu_main)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_settings -> {
                    openSettings()
                    true
                }
                else -> false
            }
        }

        // Seulement au premier lancement (sinon fragments en double après rotation)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, ListeContactsFragment())
                .commit()
        }
    }

    private fun openSettings() {
        val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (current is ParametresFragment) return   // déjà affiché
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragment_container, ParametresFragment())
            .addToBackStack(null)
            .commit()
    }
}
