// data/local/TrailGoDatabase.kt
package ht.mbds.calebtoussaint.trailgo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ListeParcoursCacheEntity::class,
        DetailParcoursCacheEntity::class,
        TraceParcoursCacheEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TrailGoDatabase : RoomDatabase() {
    abstract fun parcoursCacheDao(): ParcoursCacheDao
}