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

@Entity(tableName = "my_garden", primaryKeys = ["plantId", "userId"])
data class GardenEntity(
    val plantId: Int,
    val userId: Long = 1
)

@Dao
interface GardenDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(gardenEntity: GardenEntity)

    @Delete
    fun delete(gardenEntity: GardenEntity)

    @Query("DELETE FROM my_garden WHERE plantId = :plantId AND userId = :userId")
    fun deleteByPlantAndUser(plantId: Int, userId: Long)

    @Query("SELECT * FROM my_garden WHERE userId = :userId")
    fun getAll(userId: Long = 1): Flow<List<GardenEntity>>

    @Query("SELECT * FROM my_garden WHERE userId = :userId")
    fun getAllSync(userId: Long = 1): List<GardenEntity>
}

@Database(entities = [GardenEntity::class], version = 2, exportSchema = false)
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
