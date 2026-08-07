// data/local/ParcoursCache.kt
package ht.mbds.calebtoussaint.trailgo.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Cache de la derniere liste de parcours consultee (vue par defaut,
 * sans filtre). Une seule ligne (id fixe = 0), ecrasee a chaque appel
 * reseau reussi.
 */
@Entity(tableName = "liste_parcours_cache")
data class ListeParcoursCacheEntity(
    @PrimaryKey val id: Int = 0,
    val json: String,
    val dateMiseAJour: Long
)

/** Cache du detail d'un parcours, une ligne par parcours deja consulte. */
@Entity(tableName = "detail_parcours_cache")
data class DetailParcoursCacheEntity(
    @PrimaryKey val parcoursId: Long,
    val json: String,
    val dateMiseAJour: Long
)

/** Cache du trace GeoJSON d'un parcours. */
@Entity(tableName = "trace_parcours_cache")
data class TraceParcoursCacheEntity(
    @PrimaryKey val parcoursId: Long,
    val json: String,
    val dateMiseAJour: Long
)

@Dao
interface ParcoursCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enregistrerListe(entite: ListeParcoursCacheEntity)

    @Query("SELECT * FROM liste_parcours_cache WHERE id = 0")
    suspend fun recupererListe(): ListeParcoursCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enregistrerDetail(entite: DetailParcoursCacheEntity)

    @Query("SELECT * FROM detail_parcours_cache WHERE parcoursId = :id")
    suspend fun recupererDetail(id: Long): DetailParcoursCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enregistrerTrace(entite: TraceParcoursCacheEntity)

    @Query("SELECT * FROM trace_parcours_cache WHERE parcoursId = :id")
    suspend fun recupererTrace(id: Long): TraceParcoursCacheEntity?
}