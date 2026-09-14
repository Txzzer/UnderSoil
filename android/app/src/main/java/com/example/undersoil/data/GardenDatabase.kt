package com.example.undersoil.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "my_garden")
data class GardenEntity(
    @PrimaryKey val plantId: Int
)

@Dao
interface GardenDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(gardenEntity: GardenEntity)

    @Delete
    fun delete(gardenEntity: GardenEntity)

    @Query("SELECT * FROM my_garden")
    fun getAll(): Flow<List<GardenEntity>>
}

@Database(entities = [GardenEntity::class], version = 1, exportSchema = false)
abstract class GardenDatabase : RoomDatabase() {
    abstract fun gardenDao(): GardenDao
}
