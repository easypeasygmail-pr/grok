package de.odpisz.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LetterEntity::class, ProfileEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class OdpiszDatabase : RoomDatabase() {
    abstract fun letterDao(): LetterDao

    abstract fun profileDao(): ProfileDao
}
