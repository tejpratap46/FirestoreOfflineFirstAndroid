package com.tejpratapsingh.firestoreofflinefirst.database.table

import androidx.room.*

@Entity(tableName = "FirestoreSyncMaster")
class FirestoreSyncMaster(
    @PrimaryKey()
    val collectionName: String,

    @ColumnInfo(name = "downloadedTill")
    var downloadedTill: Long
) {

    @Dao
    interface DataAccess {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insert(firestoreSyncMaster: FirestoreSyncMaster)

        @Update
        fun update(firestoreSyncMaster: FirestoreSyncMaster)

        @Delete
        fun delete(firestoreSyncMaster: FirestoreSyncMaster)

        @Query("SELECT * FROM FirestoreSyncMaster ORDER BY downloadedTill ASC LIMIT 1")
        fun getMinSyncMaster(): List<FirestoreSyncMaster>

        @Query("SELECT * FROM FirestoreSyncMaster WHERE collectionName= :collectionName LIMIT 1")
        fun getSyncMasterByCollectionName(collectionName: String): List<FirestoreSyncMaster>

        @Query("SELECT * FROM FirestoreSyncMaster")
        fun getAllSyncMaster(): List<FirestoreSyncMaster>
    }
}