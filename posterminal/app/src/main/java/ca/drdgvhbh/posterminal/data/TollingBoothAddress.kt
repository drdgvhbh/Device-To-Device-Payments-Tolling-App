package ca.drdgvhbh.posterminal.data

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Single

@Entity(tableName = "tolling_booth_address")
data class TollingBoothAddress(
    @ColumnInfo(name = "address") val address: String,
    @PrimaryKey val doNotSetMe: Long = 1
)

@Dao
interface TollingBoothAddressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tollingBoothAddress: TollingBoothAddress): Completable

    @Query("SELECT * FROM tolling_booth_address WHERE doNotSetMe == 1")
    fun get(): Single<TollingBoothAddress?>
}

@Database(entities = [TollingBoothAddress::class], version = 1)
abstract class TollingBoothAddressDatabase : RoomDatabase() {
    abstract fun tollingBoothAddressDao(): TollingBoothAddressDao
}