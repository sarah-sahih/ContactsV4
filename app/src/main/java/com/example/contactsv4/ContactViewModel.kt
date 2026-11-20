package com.example.contactsv4

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.contactsv4.data.Contact
import com.example.contactsv4.data.ContactDao
import com.example.contactsv4.data.ContactDbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel partagé. Tout accès à SQLite se fait sur Dispatchers.IO,
 * lancé depuis viewModelScope (jamais sur le thread principal).
 */
class ContactViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = ContactDao(ContactDbHelper.getInstance(application))

    private val _contacts = MutableLiveData<List<Contact>>()
    val contacts: LiveData<List<Contact>> = _contacts

    private val _result = MutableLiveData<Event<DbResult>>()
    val result: LiveData<Event<DbResult>> = _result

    var query: String = ""
        private set

    private var searchJob: Job? = null

    init {
        refresh()
    }

    /** Filtre en temps réel : la recherche est faite par SQLite (LIKE). */
    fun setQuery(text: String) {
        if (text == query) return
        query = text
        refresh()
    }

    private fun refresh() {
        searchJob?.cancel()   // une frappe plus récente annule la recherche précédente
        searchJob = viewModelScope.launch { _contacts.value = loadList() }
    }

    private suspend fun loadList(): List<Contact> = withContext(Dispatchers.IO) {
        try {
            dao.search(query)
        } catch (e: SQLiteException) {
            emptyList()
        }
    }

    suspend fun getContact(id: Long): Contact? = withContext(Dispatchers.IO) {
        try {
            dao.getById(id)
        } catch (e: SQLiteException) {
            null
        }
    }

    /** Create si id == 0, sinon Update. */
    fun save(contact: Contact) {
        viewModelScope.launch {
            val result: DbResult = withContext(Dispatchers.IO) {
                try {
                    if (contact.id == 0L) {
                        if (dao.insert(contact) == -1L) DbResult.Error("Insertion échouée : le contact n'a pas été enregistré.")
                        else DbResult.Success("Contact ajouté")
                    } else {
                        if (dao.update(contact) == 0) DbResult.Error("Contact introuvable : il a peut-être été supprimé.")
                        else DbResult.Success("Contact enregistré")
                    }
                } catch (e: SQLiteException) {
                    DbResult.Error("Erreur de base de données : ${e.message}")
                }
            }
            if (result is DbResult.Success) _contacts.value = loadList()
            _result.value = Event(result)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            val result: DbResult = withContext(Dispatchers.IO) {
                try {
                    if (dao.delete(id) == 0) DbResult.Error("Contact introuvable : il a peut-être déjà été supprimé.")
                    else DbResult.Success("Contact supprimé")
                } catch (e: SQLiteException) {
                    DbResult.Error("Erreur de base de données : ${e.message}")
                }
            }
            if (result is DbResult.Success) _contacts.value = loadList()
            _result.value = Event(result)
        }
    }
}
