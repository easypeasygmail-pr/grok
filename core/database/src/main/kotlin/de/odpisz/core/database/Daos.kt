package de.odpisz.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LetterDao {
    @Query("SELECT * FROM letters ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<LetterEntity>>

    @Query("SELECT * FROM letters WHERE id = :id")
    suspend fun get(id: String): LetterEntity?

    @Upsert
    suspend fun upsert(entity: LetterEntity)

    @Query("DELETE FROM letters")
    suspend fun deleteAll()
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = :id")
    fun observe(id: Int = ProfileEntity.PROFILE_ID): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = :id")
    suspend fun get(id: Int = ProfileEntity.PROFILE_ID): ProfileEntity?

    @Upsert
    suspend fun upsert(entity: ProfileEntity)

    @Query("DELETE FROM profile")
    suspend fun deleteAll()
}
