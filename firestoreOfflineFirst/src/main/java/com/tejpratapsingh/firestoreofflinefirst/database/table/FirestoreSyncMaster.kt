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

        @Query("SELECT * FROM FirestoreSyncMaster where collectionName= :collectionName limit 1")
        fun getSyncMasterByCollectionName(collectionName: String): FirestoreSyncMaster?

        @Query("SELECT * FROM FirestoreSyncMaster")
        fun getAllSyncMaster(): List<FirestoreSyncMaster>
    }
}