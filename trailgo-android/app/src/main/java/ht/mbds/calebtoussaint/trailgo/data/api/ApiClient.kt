// data/api/ApiClient.kt
package ht.mbds.calebtoussaint.trailgo.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Point d'entree UNIQUE pour la configuration reseau, l'equivalent
 * exact de src/api/client.js cote React.
 *
 * ============================================================
 * ADRESSE DE L'API : A MODIFIER
 * ============================================================
 * "localhost" depuis un TELEPHONE PHYSIQUE designerait le telephone
 * lui-meme, pas l'ordinateur qui fait tourner l'API Spring Boot.
 * Il faut l'adresse IP locale de l'ordinateur sur le reseau Wi-Fi
 * (visible via "ipconfig" dans un terminal Windows), et les deux
 * appareils doivent etre connectes au MEME reseau Wi-Fi.
 *
 * Remplace 192.168.1.XX par ta propre adresse.
 * ============================================================
 */
object ApiClient {

    private const val URL_BASE = "http://192.168.0.87:8081/"

    /**
     * L'API renvoie des chemins RELATIFS pour les images
     * ("/uploads/xxx.jpg"), valides uniquement combines a l'adresse du
     * serveur. Sans ce prefixe, Coil cherche l'image a une adresse
     * inexistante et n'affiche rien. Meme piege que cote React,
     * corrige de la meme maniere.
     */
    fun urlAbsolueImage(cheminRelatif: String?): String? {
        if (cheminRelatif.isNullOrBlank()) return null
        if (cheminRelatif.startsWith("http")) return cheminRelatif
        return URL_BASE.trimEnd('/') + cheminRelatif
    }

    /**
     * Intercepteur : ajoute automatiquement le jeton JWT a chaque
     * requete, exactement comme l'intercepteur de requete d'axios
     * cote React. Nulle part ailleurs dans le code on ne manipule
     * l'en-tete Authorization a la main.
     */
    private fun creerIntercepteurAuth(gestionnaireJeton: GestionnaireJeton): Interceptor {
        return Interceptor { chain ->
            val requeteOriginale = chain.request()
            val jeton = gestionnaireJeton.obtenirJeton()

            val requete = if (jeton != null) {
                requeteOriginale.newBuilder()
                    .addHeader("Authorization", "Bearer $jeton")
                    .build()
            } else {
                requeteOriginale
            }
            chain.proceed(requete)
        }
    }

    /**
     * Journalise chaque requete et reponse dans Logcat (le "terminal"
     * d'Android Studio). Indispensable en developpement pour voir
     * exactement ce qui part et ce qui revient, mais a desactiver
     * en production (fuite d'information potentielle dans les logs).
     */
    private val intercepteurLog = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    fun creerRetrofit(gestionnaireJeton: GestionnaireJeton): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor(creerIntercepteurAuth(gestionnaireJeton))
            .addInterceptor(intercepteurLog)
            .build()

        return Retrofit.Builder()
            .baseUrl(URL_BASE)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}