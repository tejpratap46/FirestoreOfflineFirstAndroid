package com.tejpratapsingh.firestoreofflinefirst.database.table

import androidx.room.*
import com.google.firebase.firestore.DocumentReference
import java.util.*

@Entity(tableName = "FirestoreSyncTracker")
class FirestoreSyncTracker(
    /**
     * Combination of local + firestoreId [localId:firestoreId]
     * @see com.tejpratapsingh.firestoreofflinefirst.database.table.FirestoreSyncTracker.generateId
     */
    @PrimaryKey()
    val firestoreId: String,

    @ColumnInfo(name = "userId")
    val userId: String?,

    @ColumnInfo(name = "collectionName")
    val collectionName: String,

    // Locally Created On [in milli], for conflict resolution
    // Never updated, Only Set when data is created
    @ColumnInfo(name = "createdOn")
    val createdOn: Long,

    // Locally Updated On [in milli], for conflict resolution
    // updated every time data is updated
    @ColumnInfo(name = "updatedOn")
    var updatedOn: Long,

    // Device Id who updated it locally
    @ColumnInfo(name = "updatedBy")
    var updatedBy: String,

    // Should be called after local save
    @ColumnInfo(name = "updatedLocally")
    var updatedLocally: Boolean,

    // This is the actual data that has to be saved in firestore
    @ColumnInfo(name = "deleted")
    var deleted: Boolean,

    // This is the actual data that has to be saved in firestore
    @ColumnInfo(name = "data")
    var data: String
) {

    @Dao
    interface DataAccess {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insert(firestoreSyncTracker: FirestoreSyncTracker)

        @Update
        fun update(firestoreSyncTracker: FirestoreSyncTracker)

        @Delete
        fun delete(firestoreSyncTracker: FirestoreSyncTracker)

        @Query("SELECT * FROM FirestoreSyncTracker where firestoreId= :synTrackerId LIMIT 1")
        fun getSyncTrackerById(synTrackerId: String): List<FirestoreSyncTracker>

        @Query("SELECT * FROM FirestoreSyncTracker where collectionName= :collectionName AND firestoreId= :firestoreId LIMIT 1")
        fun getSyncTrackerById(collectionName: String, firestoreId: String): List<FirestoreSyncTracker>

        @Query("SELECT * FROM FirestoreSyncTracker where updatedLocally= :updatedLocally")
        fun getAllUpdatedDocuments(updatedLocally: Boolean): List<FirestoreSyncTracker>

        @Query("SELECT * FROM FirestoreSyncTracker")
        fun getAllSyncTrackers(): List<FirestoreSyncTracker>
    }
}