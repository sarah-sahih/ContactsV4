package com.example.contactsv4

import android.content.Context
import android.graphics.Color
import androidx.preference.PreferenceManager

/** Valeurs déjà converties, prêtes à être appliquées par l'adapter. */
data class DisplayPrefs(
    val textSizeSp: Float,
    val textColor: Int,
    val callFromList: Boolean
)

/** Point d'accès unique aux SharedPreferences. */
class PreferencesManager(context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun read(): DisplayPrefs {
        val size = prefs.getString(KEY_TAILLE, DEF_TAILLE)?.toFloatOrNull() ?: DEF_TAILLE.toFloat()
        val color = try {
            Color.parseColor(prefs.getString(KEY_COULEUR, DEF_COULEUR) ?: DEF_COULEUR)
        } catch (e: IllegalArgumentException) {
            Color.BLACK
        }
        val call = prefs.getBoolean(KEY_APPEL, false)
        return DisplayPrefs(size, color, call)
    }

    companion object {
        const val KEY_TAILLE = "taille_police"
        const val KEY_COULEUR = "couleur_preferee"
        const val KEY_APPEL = "appel_depuis_liste"
        private const val DEF_TAILLE = "16"
        private const val DEF_COULEUR = "#000000"
    }
}
