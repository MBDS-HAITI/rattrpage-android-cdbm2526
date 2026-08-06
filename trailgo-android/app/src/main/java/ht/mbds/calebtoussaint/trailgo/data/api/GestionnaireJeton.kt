// data/api/GestionnaireJeton.kt
package ht.mbds.calebtoussaint.trailgo.data.api

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stockage securise du jeton JWT et des informations de l'utilisateur
 * connecte, via EncryptedSharedPreferences.
 *
 * POURQUOI EncryptedSharedPreferences ET NON SharedPreferences NORMAL :
 * un SharedPreferences classique enregistre les donnees en clair dans
 * un fichier XML lisible par quiconque a acces au telephone (root,
 * sauvegarde ADB...). EncryptedSharedPreferences chiffre a la fois les
 * cles et les valeurs avec une cle geree par le Keystore materiel
 * d'Android, invisible meme en cas d'extraction du fichier.
 *
 * C'est une exigence explicite du sujet pour la persistance du jeton.
 */
class GestionnaireJeton(context: Context) {

    private val prefs: SharedPreferences

    companion object {
        private const val NOM_FICHIER = "trailgo_secure_prefs"
        private const val CLE_JETON = "jeton"
        private const val CLE_UTILISATEUR_ID = "utilisateur_id"
        private const val CLE_UTILISATEUR_EMAIL = "utilisateur_email"
        private const val CLE_UTILISATEUR_NOM = "utilisateur_nom"
        private const val CLE_UTILISATEUR_ROLE = "utilisateur_role"
    }

    init {
        // La cle maitresse est generee et stockee dans le Keystore
        // materiel du telephone (une puce dediee sur la plupart des
        // appareils recents), jamais accessible au niveau applicatif.
        val cleMaitresse = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            NOM_FICHIER,
            cleMaitresse,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun enregistrerSession(
        jeton: String,
        utilisateurId: Long,
        email: String,
        nom: String,
        role: String
    ) {
        prefs.edit()
            .putString(CLE_JETON, jeton)
            .putLong(CLE_UTILISATEUR_ID, utilisateurId)
            .putString(CLE_UTILISATEUR_EMAIL, email)
            .putString(CLE_UTILISATEUR_NOM, nom)
            .putString(CLE_UTILISATEUR_ROLE, role)
            .apply()
    }

    fun obtenirJeton(): String? = prefs.getString(CLE_JETON, null)

    fun obtenirRole(): String? = prefs.getString(CLE_UTILISATEUR_ROLE, null)

    fun obtenirNom(): String? = prefs.getString(CLE_UTILISATEUR_NOM, null)

    fun obtenirEmail(): String? = prefs.getString(CLE_UTILISATEUR_EMAIL, null)

    fun estConnecte(): Boolean = obtenirJeton() != null

    fun effacerSession() {
        prefs.edit().clear().apply()
    }
}
