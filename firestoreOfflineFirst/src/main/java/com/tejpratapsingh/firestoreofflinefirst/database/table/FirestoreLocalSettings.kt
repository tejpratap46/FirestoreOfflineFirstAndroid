package com.tejpratapsingh.firestoreofflinefirst.database.table

import androidx.room.*

@Entity(tableName = "FirestoreLocalSettings")
class FirestoreLocalSettings(
    @PrimaryKey()
    val id: Int,

    @ColumnInfo(name = "deviceId")
    val deviceId: String
) {

    @Dao
    interface DataAccess {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insert(firestoreLocalSettings: FirestoreLocalSettings)

        @Update
        fun update(firestoreLocalSettings: FirestoreLocalSettings)

        @Delete
        fun delete(firestoreLocalSettings: FirestoreLocalSettings)

        @Query("SELECT * FROM FirestoreLocalSettings")
        fun getAllLocalSettings(): List<FirestoreLocalSettings>
    }
}