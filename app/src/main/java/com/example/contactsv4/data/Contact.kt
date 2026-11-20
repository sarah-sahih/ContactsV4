package com.example.contactsv4.data

/** id = 0 tant que le contact n'est pas enregistré, puis = _id attribué par SQLite. */
data class Contact(
    val id: Long = 0,
    val nom: String,
    val prenom: String,
    val telephone: String,
    val email: String? = null,
    val photoUri: String? = null,   // chemin d'une copie de l'image dans filesDir
    val latitude: Double? = null,
    val longitude: Double? = null
)
