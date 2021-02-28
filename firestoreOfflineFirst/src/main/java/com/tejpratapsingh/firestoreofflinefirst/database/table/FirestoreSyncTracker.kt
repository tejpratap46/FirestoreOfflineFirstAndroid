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
    val id: String,

    @ColumnInfo(name = "userId")
    val userId: String,

    @ColumnInfo(name = "collectionName")
    val collectionName: String,

    // Locally Created On, for conflict resolution
    // Never updated once Set
    @ColumnInfo(name = "createdOn")
    val createdOn: Long,

    // Locally Updated On, for conflict resolution
    // updated every time new set is called
    @ColumnInfo(name = "updatedOn")
    var updatedOn: Long,

    // Device Id who updated it locally
    @ColumnInfo(name = "updatedBy")
    var updatedBy: String,

    // Should be called after local save
    @ColumnInfo(name = "updatedLocally")
    var updatedLocally: Boolean,

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

        @Query("SELECT * FROM FirestoreSyncTracker where id= :synTrackerId limit 1")
        fun getSyncTrackerById(synTrackerId: String): FirestoreSyncTracker?

        @Query("SELECT * FROM FirestoreSyncTracker where collectionName= :collectionName AND id= :synTrackerId limit 1")
        fun getSyncTrackerById(collectionName: String, synTrackerId: String): FirestoreSyncTracker?

        @Query("SELECT * FROM FirestoreSyncTracker")
        fun getAllSyncTrackers(): List<FirestoreSyncTracker>
    }

    companion object {
        fun generateId(localId: String, firestoreDocumentReference: DocumentReference): String {
            return String.format(Locale.getDefault(), "%s::%s", localId, firestoreDocumentReference.id)
        }
    }
}