package com.example.contactsv4

import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.contactsv4.data.Contact

class ContactAdapter(
    private val onContactClick: (Contact) -> Unit,
    private val onCallClick: (Contact) -> Unit
) : ListAdapter<Contact, ContactAdapter.ContactViewHolder>(DiffCallback) {

    private var prefs = DisplayPrefs(textSizeSp = 16f, textColor = Color.BLACK, callFromList = false)

    /** Applique de nouvelles préférences et redessine les lignes si elles ont changé. */
    fun applyPrefs(newPrefs: DisplayPrefs) {
        if (newPrefs == prefs) return
        prefs = newPrefs
        notifyDataSetChanged()
    }

    class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView = view.findViewById(R.id.ivPhoto)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvPhone: TextView = view.findViewById(R.id.tvPhone)
        val btnCall: ImageButton = view.findViewById(R.id.btnCallRow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(v)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val c = getItem(position)
        holder.tvName.text = "${c.nom} ${c.prenom}"
        holder.tvPhone.text = c.telephone

        // Préférences : taille (nom + téléphone), couleur (nom + prénom), bouton Appeler
        holder.tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, prefs.textSizeSp)
        holder.tvPhone.setTextSize(TypedValue.COMPLEX_UNIT_SP, prefs.textSizeSp)
        holder.tvName.setTextColor(prefs.textColor)
        holder.btnCall.visibility = if (prefs.callFromList) View.VISIBLE else View.GONE

        val bmp = c.photoUri?.let { ImageUtils.loadBitmap(it, 150) }
        if (bmp != null) holder.ivPhoto.setImageBitmap(bmp)
        else holder.ivPhoto.setImageResource(R.drawable.ic_person)

        // On transmet l'objet Contact (donc son _id), jamais la position
        holder.itemView.setOnClickListener { onContactClick(c) }
        // Le bouton consomme son propre clic : le détail ne s'ouvre pas
        holder.btnCall.setOnClickListener { onCallClick(c) }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(old: Contact, new: Contact) = old.id == new.id
        override fun areContentsTheSame(old: Contact, new: Contact) = old == new
    }
}
