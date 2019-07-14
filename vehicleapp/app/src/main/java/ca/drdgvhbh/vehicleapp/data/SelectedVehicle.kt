package ca.drdgvhbh.vehicleapp.data

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Single

@Entity(tableName = "selected_vehicle")
data class SelectedVehicle(
    @ColumnInfo(name = "id") val id: String,
    @PrimaryKey val doNotSetMe: Long = 1
)

@Dao
interface SelectedVehicleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(selectedVehicle: SelectedVehicle): Completable

    @Query("SELECT * FROM selected_vehicle WHERE doNotSetMe == 1")
    fun get(): Single<SelectedVehicle?>
}

@Database(entities = [SelectedVehicle::class], version = 1)
abstract class SelectedVehicleDatabase : RoomDatabase() {
    abstract fun selectedVehicleDao(): SelectedVehicleDao
}