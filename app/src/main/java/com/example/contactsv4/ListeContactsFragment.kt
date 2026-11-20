package com.example.contactsv4

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ListeContactsFragment : Fragment(R.layout.fragment_liste) {

    private val vm: ContactViewModel by activityViewModels()
    private val prefsManager by lazy { PreferencesManager(requireContext()) }
    private var adapter: ContactAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val rv = view.findViewById<RecyclerView>(R.id.rvContacts)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAdd)

        val a = ContactAdapter(
            onContactClick = { contact -> openDetail(contact.id) },
            onCallClick = { contact -> dialNumber(contact.telephone) }
        )
        a.applyPrefs(prefsManager.read())
        adapter = a
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = a

        fab.setOnClickListener { openDetail(0L) }   // 0 = mode création

        etSearch.setText(vm.query)
        etSearch.doAfterTextChanged { vm.setQuery(it?.toString().orEmpty()) }

        // La liste vient de SQLite (chargée par le ViewModel hors du thread principal)
        vm.contacts.observe(viewLifecycleOwner) { list ->
            a.submitList(list)
            tvEmpty.text = if (vm.query.isBlank())
                "Aucun contact enregistré.\nAppuyez sur + pour en ajouter."
            else
                "Aucun contact ne correspond à votre recherche"
            tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    /** Au retour de l'écran Paramètres : relecture immédiate des préférences. */
    override fun onResume() {
        super.onResume()
        adapter?.applyPrefs(prefsManager.read())
    }

    override fun onDestroyView() {
        adapter = null
        super.onDestroyView()
    }

    private fun openDetail(contactId: Long) {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragment_container, DetailContactFragment.newInstance(contactId))
            .addToBackStack(null)
            .commit()
    }
}
