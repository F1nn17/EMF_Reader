package com.shiroma.emfreader.data

import androidx.room.*

@Entity(tableName = "magnetic_data")
data class MagneticData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val magnitude: Float
)

@Dao
interface MagneticDataDao {
    @Insert
    suspend fun insert(data: MagneticData)

    @Query("SELECT * FROM magnetic_data ORDER BY timestamp ASC")
    suspend fun getAll(): List<MagneticData>
}

@Database(entities = [MagneticData::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun magneticDataDao(): MagneticDataDao
}
