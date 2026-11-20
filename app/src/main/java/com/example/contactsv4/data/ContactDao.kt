package com.example.contactsv4.data

import android.content.ContentValues
import android.database.Cursor
import com.example.contactsv4.data.ContactContract as C

/** Seule classe qui contient du SQL. Toutes les valeurs variables passent par des « ? ». */
class ContactDao(private val helper: ContactDbHelper) {

    /** @return l'_id attribué, ou -1 si l'insertion a échoué. */
    fun insert(contact: Contact): Long =
        helper.writableDatabase.insert(C.TABLE, null, contact.toContentValues())

    /** @return le nombre de lignes modifiées (0 = contact introuvable). */
    fun update(contact: Contact): Int =
        helper.writableDatabase.update(
            C.TABLE, contact.toContentValues(), "${C.COL_ID} = ?", arrayOf(contact.id.toString())
        )

    /** @return le nombre de lignes supprimées (0 = contact introuvable). */
    fun delete(id: Long): Int =
        helper.writableDatabase.delete(C.TABLE, "${C.COL_ID} = ?", arrayOf(id.toString()))

    fun getAll(): List<Contact> = query(null, null)

    fun getById(id: Long): Contact? =
        query("${C.COL_ID} = ?", arrayOf(id.toString())).firstOrNull()

    /** Recherche sur nom, prénom et téléphone (espaces ignorés dans le numéro). */
    fun search(texte: String): List<Contact> {
        val q = texte.trim()
        if (q.isEmpty()) return getAll()
        val like = "%" + escapeLike(q) + "%"
        val likePhone = "%" + escapeLike(q.replace(" ", "")) + "%"
        val selection =
            "${C.COL_NOM} LIKE ? ESCAPE '\\' OR " +
                "${C.COL_PRENOM} LIKE ? ESCAPE '\\' OR " +
                "REPLACE(${C.COL_TELEPHONE}, ' ', '') LIKE ? ESCAPE '\\'"
        return query(selection, arrayOf(like, like, likePhone))
    }

    // ---------------------------------------------------------------

    private fun query(selection: String?, args: Array<String>?): List<Contact> {
        val result = mutableListOf<Contact>()
        helper.readableDatabase
            .query(C.TABLE, null, selection, args, null, null, ORDER_BY)
            .use { cursor ->
                val iId = cursor.getColumnIndexOrThrow(C.COL_ID)
                val iNom = cursor.getColumnIndexOrThrow(C.COL_NOM)
                val iPrenom = cursor.getColumnIndexOrThrow(C.COL_PRENOM)
                val iTel = cursor.getColumnIndexOrThrow(C.COL_TELEPHONE)
                val iEmail = cursor.getColumnIndexOrThrow(C.COL_EMAIL)
                val iPhoto = cursor.getColumnIndexOrThrow(C.COL_PHOTO_URI)
                val iLat = cursor.getColumnIndexOrThrow(C.COL_LATITUDE)
                val iLon = cursor.getColumnIndexOrThrow(C.COL_LONGITUDE)
                while (cursor.moveToNext()) {
                    result.add(
                        Contact(
                            id = cursor.getLong(iId),
                            nom = cursor.getString(iNom),
                            prenom = cursor.getString(iPrenom),
                            telephone = cursor.getString(iTel),
                            email = cursor.stringOrNull(iEmail),
                            photoUri = cursor.stringOrNull(iPhoto),
                            latitude = cursor.doubleOrNull(iLat),
                            longitude = cursor.doubleOrNull(iLon)
                        )
                    )
                }
            }
        return result
    }

    private fun Cursor.stringOrNull(index: Int): String? = if (isNull(index)) null else getString(index)
    private fun Cursor.doubleOrNull(index: Int): Double? = if (isNull(index)) null else getDouble(index)

    /** Neutralise % et _ saisis par l'utilisateur (ils ont un sens spécial dans LIKE). */
    private fun escapeLike(s: String): String =
        s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    companion object {
        // Tri par nom puis prénom, sans tenir compte de la casse
        private val ORDER_BY =
            "${C.COL_NOM} COLLATE NOCASE ASC, ${C.COL_PRENOM} COLLATE NOCASE ASC"
    }
}

/** L'_id n'est pas mis : SQLite l'attribue (AUTOINCREMENT). Position absente = NULL. */
fun Contact.toContentValues(): ContentValues {
    val values = ContentValues()
    values.put(C.COL_NOM, nom)
    values.put(C.COL_PRENOM, prenom)
    values.put(C.COL_TELEPHONE, telephone)
    val mail = email
    val photo = photoUri
    val lat = latitude
    val lon = longitude
    if (mail != null) values.put(C.COL_EMAIL, mail) else values.putNull(C.COL_EMAIL)
    if (photo != null) values.put(C.COL_PHOTO_URI, photo) else values.putNull(C.COL_PHOTO_URI)
    if (lat != null) values.put(C.COL_LATITUDE, lat) else values.putNull(C.COL_LATITUDE)
    if (lon != null) values.put(C.COL_LONGITUDE, lon) else values.putNull(C.COL_LONGITUDE)
    return values
}
