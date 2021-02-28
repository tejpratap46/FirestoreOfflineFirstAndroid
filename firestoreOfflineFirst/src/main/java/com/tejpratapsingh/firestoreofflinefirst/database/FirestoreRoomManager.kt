package com.tejpratapsingh.firestoreofflinefirst.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tejpratapsingh.firestoreofflinefirst.database.table.FirestoreLocalSettings
import com.tejpratapsingh.firestoreofflinefirst.database.table.FirestoreSyncMaster
import com.tejpratapsingh.firestoreofflinefirst.database.table.FirestoreSyncTracker

@Database(
    entities = [
        FirestoreSyncTracker::class,
        FirestoreSyncMaster::class,
        FirestoreLocalSettings::class
    ],
    exportSchema = true,
    version = 1
)
abstract class FirestoreRoomManager: RoomDatabase() {

    abstract fun firestoreSyncTrackerDao(): FirestoreSyncTracker.DataAccess
    abstract fun firestoreSyncMasterDao(): FirestoreSyncMaster.DataAccess
    abstract fun firestoreLocalSettingsDao(): FirestoreLocalSettings.DataAccess

    companion object {
        private val TAG = "RoomManager"

        private var instance: FirestoreRoomManager? = null
        private val DB_NAME = "firestoreOfflineDB"

        fun getInstance(context: Context): FirestoreRoomManager {
            if (instance == null) {
                synchronized(FirestoreRoomManager::class) {
                    instance = Room.databaseBuilder(
                        context.applicationContext, FirestoreRoomManager::class.java,
                        this.DB_NAME
                    ).allowMainThreadQueries() /* SHOULD NOT BE USED IN PRODUCTION !!! */
                        .addMigrations()
                        .build()
                }
            }
            return instance!!
        }

        fun destroyDataBase() {
            instance = null
        }
    }
}