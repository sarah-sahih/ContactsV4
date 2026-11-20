package com.example.contactsv4

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class CarteContactFragment : Fragment(R.layout.fragment_carte) {

    private val vm: ContactViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)
        val mapView = view.findViewById<View>(R.id.map)
        view.findViewById<Button>(R.id.btnRetour).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val id = requireArguments().getLong(ARG_ID)
        viewLifecycleOwner.lifecycleScope.launch {
            val contact = vm.getContact(id)          // lecture SQLite hors thread principal
            val lat = contact?.latitude
            val lon = contact?.longitude

            val positionValide = lat != null && lon != null &&
                lat in -90.0..90.0 && lon in -180.0..180.0
            if (contact == null || lat == null || lon == null || !positionValide) {
                mapView.visibility = View.GONE
                tvMessage.visibility = View.VISIBLE
                tvMessage.text = when {
                    contact == null -> "Contact introuvable."
                    lat == null || lon == null -> "Aucune position n'est enregistrée pour ce contact."
                    else -> "Les coordonnées de ce contact sont invalides."
                }
                return@launch
            }

            tvMessage.visibility = View.GONE
            val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
                ?: return@launch
            mapFragment.getMapAsync { googleMap ->
                val position = LatLng(lat, lon)
                googleMap.addMarker(
                    MarkerOptions().position(position).title("${contact.prenom} ${contact.nom}")
                )
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 14f))
            }
        }
    }

    companion object {
        private const val ARG_ID = "contact_id"

        fun newInstance(contactId: Long) = CarteContactFragment().apply {
            arguments = Bundle().apply { putLong(ARG_ID, contactId) }
        }
    }
}
