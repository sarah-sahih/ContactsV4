package com.example.contactsv4

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.fragment.app.Fragment

fun Fragment.toast(message: String) =
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

/** ACTION_DIAL : ouvre le composeur, aucune permission requise (contrairement à ACTION_CALL). */
fun Fragment.dialNumber(phone: String) {
    val number = phone.trim()
    if (number.isEmpty()) {
        toast("Numéro de téléphone vide")
        return
    }
    try {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))))
    } catch (e: ActivityNotFoundException) {
        toast("Aucune application d'appel disponible")
    }
}
