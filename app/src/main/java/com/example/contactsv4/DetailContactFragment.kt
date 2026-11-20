package com.example.contactsv4

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.contactsv4.data.Contact
import kotlinx.coroutines.launch
import java.io.File

class DetailContactFragment : Fragment(R.layout.fragment_detail) {

    private val vm: ContactViewModel by activityViewModels()

    private var contactId = 0L                 // 0 = création
    private var original: Contact? = null      // contact chargé depuis la base
    private var populated = false              // champs déjà remplis ? (évite d'écraser une saisie)
    private var pendingPhoto: String? = null
    private var cameraPath: String? = null
    private var deleteDialog: AlertDialog? = null

    private lateinit var ivPhoto: ImageView
    private lateinit var etNom: EditText
    private lateinit var etPrenom: EditText
    private lateinit var etTel: EditText
    private lateinit var etEmail: EditText
    private lateinit var etLat: EditText
    private lateinit var etLon: EditText
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button
    private lateinit var btnMap: Button

    // ---- Galerie ----
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) {
                toast("Aucune image sélectionnée")
            } else {
                val path = ImageUtils.copyToPrivateStorage(requireContext(), uri)
                if (path != null) {
                    pendingPhoto = path
                    showPhoto(path)
                } else toast("Impossible de lire cette image")
            }
        }

    // ---- Caméra ----
    private val takePhoto =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val path = cameraPath
            if (success && path != null) {
                pendingPhoto = path
                showPhoto(path)
            } else {
                path?.let { File(it).delete() }
                toast("Prise de photo annulée")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactId = requireArguments().getLong(ARG_ID, 0L)
        populated = savedInstanceState?.getBoolean(KEY_POPULATED) ?: false
        pendingPhoto = savedInstanceState?.getString(KEY_PENDING)
        cameraPath = savedInstanceState?.getString(KEY_CAMERA)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ivPhoto = view.findViewById(R.id.ivPhoto)
        etNom = view.findViewById(R.id.etNom)
        etPrenom = view.findViewById(R.id.etPrenom)
        etTel = view.findViewById(R.id.etTel)
        etEmail = view.findViewById(R.id.etEmail)
        etLat = view.findViewById(R.id.etLat)
        etLon = view.findViewById(R.id.etLon)
        btnSave = view.findViewById(R.id.btnSave)
        btnDelete = view.findViewById(R.id.btnDelete)
        btnMap = view.findViewById(R.id.btnMap)

        applyPreferences()

        val creation = contactId == 0L
        if (creation) {                       // rien à supprimer ni à localiser avant l'enregistrement
            btnDelete.visibility = View.GONE
            btnMap.visibility = View.GONE
        }
        btnSave.isEnabled = creation          // en modification : activé une fois le contact chargé

        view.findViewById<Button>(R.id.btnCall).setOnClickListener { dialNumber(etTel.text.toString()) }
        view.findViewById<Button>(R.id.btnEmail).setOnClickListener { sendEmail() }
        view.findViewById<Button>(R.id.btnGallery).setOnClickListener { openGallery() }
        view.findViewById<Button>(R.id.btnCamera).setOnClickListener { openCamera() }
        btnMap.setOnClickListener { openMap() }
        btnSave.setOnClickListener { save() }
        btnDelete.setOnClickListener { confirmDelete() }
        view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            parentFragmentManager.popBackStack()   // Annuler : rien n'est écrit en base
        }

        // Résultat des opérations d'écriture (Create / Update / Delete)
        vm.result.observe(viewLifecycleOwner) { event ->
            when (val r = event.getIfNotHandled()) {
                is DbResult.Success -> {
                    toast(r.message)
                    parentFragmentManager.popBackStack()
                }
                is DbResult.Error -> {
                    toast(r.message)
                    btnSave.isEnabled = true
                    btnDelete.isEnabled = true
                }
                null -> Unit
            }
        }

        // Read : le contact est lu dans SQLite (hors thread principal)
        viewLifecycleOwner.lifecycleScope.launch {
            val c = if (creation) null else vm.getContact(contactId)
            if (!creation && c == null) {
                toast("Contact introuvable")
                parentFragmentManager.popBackStack()
                return@launch
            }
            original = c
            if (!populated) {        // on ne remplit qu'une fois : une saisie en cours est conservée
                c?.let { fillFields(it) }
                populated = true
            }
            showPhoto(pendingPhoto ?: c?.photoUri)
            btnSave.isEnabled = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_POPULATED, populated)
        outState.putString(KEY_PENDING, pendingPhoto)
        outState.putString(KEY_CAMERA, cameraPath)
    }

    override fun onDestroyView() {
        deleteDialog?.dismiss()
        deleteDialog = null
        super.onDestroyView()
    }

    // ------------------------------------------------------------------
    // Préférences (taille, couleur) appliquées au détail
    // ------------------------------------------------------------------
    private fun applyPreferences() {
        val p = PreferencesManager(requireContext()).read()
        listOf(etNom, etPrenom).forEach {
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, p.textSizeSp)
            it.setTextColor(p.textColor)
        }
        etTel.setTextSize(TypedValue.COMPLEX_UNIT_SP, p.textSizeSp)
    }

    private fun fillFields(c: Contact) {
        etNom.setText(c.nom)
        etPrenom.setText(c.prenom)
        etTel.setText(c.telephone)
        etEmail.setText(c.email.orEmpty())
        etLat.setText(c.latitude?.toString().orEmpty())
        etLon.setText(c.longitude?.toString().orEmpty())
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------
    private fun sendEmail() {
        val mail = etEmail.text.toString().trim()
        if (mail.isEmpty()) return toast("Adresse e-mail vide")
        try {
            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$mail")))
        } catch (e: ActivityNotFoundException) {
            toast("Aucune application de messagerie disponible")
        }
    }

    private fun openGallery() {
        try {
            pickImage.launch("image/*")
        } catch (e: ActivityNotFoundException) {
            toast("Aucune application galerie disponible")
        }
    }

    private fun openCamera() {
        try {
            val file = ImageUtils.newPhotoFile(requireContext())
            cameraPath = file.absolutePath
            val uri = FileProvider.getUriForFile(
                requireContext(), "${requireContext().packageName}.fileprovider", file
            )
            takePhoto.launch(uri)
        } catch (e: ActivityNotFoundException) {
            toast("Aucune application appareil photo disponible")
        } catch (e: Exception) {
            toast("Impossible de lancer l'appareil photo")
        }
    }

    private fun openMap() {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragment_container, CarteContactFragment.newInstance(contactId))
            .addToBackStack(null)
            .commit()
    }

    private fun confirmDelete() {
        deleteDialog = AlertDialog.Builder(requireContext())
            .setTitle("Supprimer ce contact ?")
            .setMessage("Cette action est définitive.")
            .setPositiveButton("Supprimer") { _, _ ->
                btnDelete.isEnabled = false
                btnSave.isEnabled = false
                vm.delete(contactId)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun save() {
        val nom = etNom.text.toString().trim()
        val prenom = etPrenom.text.toString().trim()
        val tel = etTel.text.toString().trim()
        val email = etEmail.text.toString().trim()
        var ok = true

        if (nom.isEmpty()) { etNom.error = "Nom obligatoire"; ok = false }
        if (prenom.isEmpty()) { etPrenom.error = "Prénom obligatoire"; ok = false }
        if (tel.isEmpty()) { etTel.error = "Téléphone obligatoire"; ok = false }
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Adresse e-mail invalide"; ok = false
        }

        val (latOk, lat) = readCoord(etLat, -90.0, 90.0, "Latitude")
        val (lonOk, lon) = readCoord(etLon, -180.0, 180.0, "Longitude")
        if (!latOk || !lonOk) ok = false
        if (latOk && lonOk && ((lat == null) != (lon == null))) {
            etLat.error = "Renseignez latitude ET longitude (ou aucune des deux)"
            ok = false
        }
        if (!ok) return toast("Corrigez les champs en erreur")   // rien n'est écrit en base

        val contact = Contact(
            id = contactId,
            nom = nom,
            prenom = prenom,
            telephone = tel,
            email = email.ifEmpty { null },
            photoUri = pendingPhoto ?: original?.photoUri,
            latitude = lat,
            longitude = lon
        )
        btnSave.isEnabled = false       // évite le double enregistrement
        vm.save(contact)                // insert (id = 0) ou update, sur Dispatchers.IO
    }

    // ------------------------------------------------------------------
    // Utilitaires
    // ------------------------------------------------------------------
    private fun readCoord(et: EditText, min: Double, max: Double, label: String): Pair<Boolean, Double?> {
        val txt = et.text.toString().trim().replace(',', '.')
        if (txt.isEmpty()) return true to null
        val v = txt.toDoubleOrNull()
        if (v == null || v.isNaN() || v.isInfinite()) {
            et.error = "$label : valeur numérique attendue"
            return false to null
        }
        if (v < min || v > max) {
            et.error = "$label doit être entre ${min.toInt()} et ${max.toInt()}"
            return false to null
        }
        return true to v
    }

    private fun showPhoto(path: String?) {
        val bmp = path?.let { ImageUtils.loadBitmap(it, 400) }
        if (bmp != null) ivPhoto.setImageBitmap(bmp) else ivPhoto.setImageResource(R.drawable.ic_person)
    }

    companion object {
        private const val ARG_ID = "contact_id"
        private const val KEY_POPULATED = "populated"
        private const val KEY_PENDING = "pending_photo"
        private const val KEY_CAMERA = "camera_path"

        /** contactId = 0 : mode création. */
        fun newInstance(contactId: Long = 0L) = DetailContactFragment().apply {
            arguments = Bundle().apply { putLong(ARG_ID, contactId) }
        }
    }
}
