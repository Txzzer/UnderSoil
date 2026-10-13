package com.example.undersoil.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "my_garden")
data class GardenEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val plantId: Int,
    val userId: String = "",
    val isSynced: Boolean = false
)

@Dao
interface GardenDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(gardenEntity: GardenEntity)

    @Delete
    fun delete(gardenEntity: GardenEntity)

    @Query("DELETE FROM my_garden WHERE plantId = :plantId AND userId = :userId")
    fun deleteByPlantAndUser(plantId: Int, userId: String)

    @Query("SELECT * FROM my_garden WHERE userId = :userId GROUP BY plantId")
    fun getAll(userId: String = ""): Flow<List<GardenEntity>>

    @Query("SELECT * FROM my_garden WHERE userId = :userId GROUP BY plantId")
    fun getAllSync(userId: String = ""): List<GardenEntity>

    @Query("SELECT * FROM my_garden WHERE isSynced = 0")
    fun getUnsyncedCrops(): List<GardenEntity>

    @Query("UPDATE my_garden SET isSynced = 1 WHERE id = :id")
    fun markCropAsSynced(id: String)
}

@Database(entities = [GardenEntity::class], version = 3, exportSchema = false)
abstract class GardenDatabase : RoomDatabase() {
    abstract fun gardenDao(): GardenDao

    companion object {
        @Volatile
        private var INSTANCE: GardenDatabase? = null

        fun getInstance(context: Context): GardenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GardenDatabase::class.java,
                    "garden-database"
                )
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
