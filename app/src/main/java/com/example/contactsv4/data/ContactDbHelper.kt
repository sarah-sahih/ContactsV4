package com.example.contactsv4.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.contactsv4.data.ContactContract as C

/**
 * Singleton construit avec applicationContext (pas de fuite d'Activity,
 * et une seule connexion partagée par toute l'application).
 */
class ContactDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    /** Appelée UNIQUEMENT quand le fichier contacts.db n'existe pas encore. */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE)
        DEMO_CONTACTS.forEach { db.insert(C.TABLE, null, it.toContentValues()) }
    }

    /**
     * Appelée quand DATABASE_VERSION augmente.
     * Version 1 : pas encore de migration, on recrée simplement la table
     * (acceptable en développement). Pour le bonus (colonne favori), on écrirait ici :
     * if (oldVersion < 2) db.execSQL("ALTER TABLE contacts ADD COLUMN favori INTEGER DEFAULT 0")
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS ${C.TABLE}")
        onCreate(db)
    }

    companion object {
        const val DATABASE_NAME = "contacts.db"
        const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: ContactDbHelper? = null

        fun getInstance(context: Context): ContactDbHelper =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ContactDbHelper(context.applicationContext).also { INSTANCE = it }
            }

        private val SQL_CREATE = """
            CREATE TABLE ${C.TABLE} (
                ${C.COL_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${C.COL_NOM} TEXT NOT NULL,
                ${C.COL_PRENOM} TEXT NOT NULL,
                ${C.COL_TELEPHONE} TEXT NOT NULL,
                ${C.COL_EMAIL} TEXT,
                ${C.COL_PHOTO_URI} TEXT,
                ${C.COL_LATITUDE} REAL,
                ${C.COL_LONGITUDE} REAL
            )
        """.trimIndent()

        // 7 contacts de démonstration, dont 2 sans coordonnées GPS
        private val DEMO_CONTACTS = listOf(
            Contact(nom = "Ben Ali", prenom = "Sami", telephone = "20 111 222", email = "sami.benali@mail.com", latitude = 34.7406, longitude = 10.7603),
            Contact(nom = "Trabelsi", prenom = "Amira", telephone = "21 333 444", email = "amira.trabelsi@mail.com", latitude = 36.8065, longitude = 10.1815),
            Contact(nom = "Gharbi", prenom = "Youssef", telephone = "22 555 666", email = "youssef.gharbi@mail.com", latitude = 35.8256, longitude = 10.6084),
            Contact(nom = "O'Neil", prenom = "Sean", telephone = "+216 23 000 111", email = "sean.oneil@mail.com"),
            Contact(nom = "Dupont", prenom = "Claire", telephone = "06 12 34 56 78", email = "claire.dupont@mail.com", latitude = 48.8566, longitude = 2.3522),
            Contact(nom = "Martin", prenom = "Lucas", telephone = "07 98 76 54 32", email = "lucas.martin@mail.com"),
            Contact(nom = "Khelifi", prenom = "Nour", telephone = "23 777 888", email = "nour.khelifi@mail.com", latitude = 33.8815, longitude = 10.0982)
        )
    }
}
