package com.tejpratapsingh.firestoreofflinefirst.firestore

import android.content.Context
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestoreSettings
import com.tejpratapsingh.firestoreofflinefirst.database.FirestoreRoomManager
import com.tejpratapsingh.firestoreofflinefirst.database.table.FirestoreLocalSettings
import com.tejpratapsingh.firestoreofflinefirst.database.table.FirestoreSyncMaster
import com.tejpratapsingh.firestoreofflinefirst.database.table.FirestoreSyncTracker
import com.tejpratapsingh.firestoreofflinefirst.extensions.getFirstOrNull
import com.tejpratapsingh.firestoreofflinefirst.interfaces.FirebaseOfflineDocument
import com.tejpratapsingh.firestoreofflinefirst.utilities.Utils
import java.util.*

open class FirestoreOfflineManager {
    companion object {
        private val TAG = "FirestoreSyncManager"

        private var instance: FirestoreOfflineManager? = null

        fun getInstance(): FirestoreOfflineManager {
            if (instance == null) {
                synchronized(FirestoreOfflineManager::class) {
                    instance = FirestoreOfflineManager()
                }
            }
            return instance!!
        }
    }

    var firestoreLocalSettings: FirestoreLocalSettings? = null

    var firesoreRoomManager: FirestoreRoomManager? = null

    var firebaseUserId: String? = null
    var firestore: FirebaseFirestore? = null

    /**
     * Initialise Firebase Offline
     *
     * @param applicationContext application context
     * @param firestore current firestore instance
     * @param firebaseUserId User Id from Firebase Auth
     */
    fun initialise(
        applicationContext: Context,
        firestore: FirebaseFirestore,
        firebaseUserId: String
    ): FirestoreOfflineManager {
        val settings = firestoreSettings {
            isPersistenceEnabled = false
        }
        firestore.firestoreSettings = settings
        this.firestore = firestore

        this.firebaseUserId = firebaseUserId

        firesoreRoomManager = FirestoreRoomManager.getInstance(applicationContext)

        val firestoreLocalSettingsDao: FirestoreLocalSettings.DataAccess =
            firesoreRoomManager!!.firestoreLocalSettingsDao()
        if (firestoreLocalSettingsDao.getAllLocalSettings().isNotEmpty()) {
            firestoreLocalSettings = firestoreLocalSettingsDao.getAllLocalSettings()[0]
        } else {
            // Create settings
            firestoreLocalSettings = FirestoreLocalSettings(1, UUID.randomUUID().toString())
            firestoreLocalSettingsDao.insert(firestoreLocalSettings!!)
        }

        return this
    }

    /**
     * After a restore, let this library know till where you have restored Data
     * Call this function for every table
     *
     * @param collectionName table/collection Name
     * @param downloadedTill till which point you have restored data
     */
    fun initialiseSyncMaster(
        collectionName: String,
        downloadedTill: Long
    ): FirestoreOfflineManager {
        checkFirestoreInitialised()
        var firestoreSyncMaster: FirestoreSyncMaster? =
            firesoreRoomManager!!.firestoreSyncMasterDao()
                .getSyncMasterByCollectionName(collectionName = collectionName).getFirstOrNull()

        var isCreate: Boolean = false
        if (firestoreSyncMaster == null) {
            firestoreSyncMaster = FirestoreSyncMaster(
                collectionName = collectionName,
                downloadedTill = downloadedTill
            )
            isCreate = true
        }

        if (firestoreSyncMaster.downloadedTill < downloadedTill) {
            // We have older downloaded till, just bump it up
            firestoreSyncMaster.downloadedTill = downloadedTill
        }

        if (isCreate) {
            firesoreRoomManager!!.firestoreSyncMasterDao().insert(firestoreSyncMaster)
        } else {
            firesoreRoomManager!!.firestoreSyncMasterDao().update(firestoreSyncMaster)
        }

        return this
    }

    /**
     * @see com.tejpratapsingh.firestoreofflinefirst.firestore.FirestoreOfflineManager.initialiseSyncMaster
     *
     * @param list of sync master to be initialised
     */
    fun initialiseSyncMasters(syncMasterList: List<FirestoreSyncMaster>): FirestoreOfflineManager {
        syncMasterList.forEach {
            initialiseSyncMaster(
                collectionName = it.collectionName,
                downloadedTill = it.downloadedTill
            )
        }

        return this
    }

    @Throws(Exception::class)
    fun checkFirestoreInitialised() {
        if (this.firestore == null) {
            throw Exception("Firestore not initialised: call initialise(FirebaseFirestore) before using any other function")
        }
    }

    /**
     * Add data to be synced with firestore
     *
     * @param collectionName table/collection Name
     * @param customFirestoreId Custom firestoreId, if null then id is randomly generated by firestore
     * @param documentToSave data to be saved, must implement FirebaseOfflineDocument interface
     * @see com.tejpratapsingh.firestoreofflinefirst.interfaces.FirebaseOfflineDocument
     */
    @Throws(Exception::class)
    fun <T : FirebaseOfflineDocument> addData(
        collectionName: String,
        customFirestoreId: String?,
        documentToSave: T
    ): FirestoreSyncTracker {
        checkFirestoreInitialised()

        val documentReference: DocumentReference = firestore!!.collection(collectionName).document()
        val firestoreId: String = customFirestoreId ?: documentReference.id

        val createdOn: Long = System.currentTimeMillis()
        val updatedOn: Long = System.currentTimeMillis()
        val updatedBy: String = firestoreLocalSettings!!.deviceInstallationId
        val data: String = Utils.getMapToJson(documentToSave.firestoreDocumentRepresentation())

        val firestoreSyncTracker: FirestoreSyncTracker = FirestoreSyncTracker(
            firestoreId = firestoreId,
            userId = firebaseUserId,
            collectionName = collectionName,
            createdOn = createdOn,
            updatedOn = updatedOn,
            updatedBy = updatedBy,
            updatedLocally = true,
            deleted = false,
            data = data
        )
        firesoreRoomManager!!.firestoreSyncTrackerDao().insert(firestoreSyncTracker)

        return firestoreSyncTracker
    }

    /**
     * Data to be updated and then synced with firestore
     *
     * @param collectionName table/collection Name
     * @param firestoreId firestoreId to update
     * @param documentToSave data to be saved, must implement FirebaseOfflineDocument interface
     * @see com.tejpratapsingh.firestoreofflinefirst.interfaces.FirebaseOfflineDocument
     */
    @Throws(Exception::class)
    fun <T : FirebaseOfflineDocument> updateData(
        collectionName: String,
        firestoreId: String,
        documentToSave: T
    ): FirestoreSyncTracker {
        checkFirestoreInitialised()

        val createdOn: Long = System.currentTimeMillis()
        val updatedOn: Long = System.currentTimeMillis()
        val updatedBy: String = firestoreLocalSettings!!.deviceInstallationId
        val data: String = Utils.getMapToJson(documentToSave.firestoreDocumentRepresentation())

        var firestoreSyncTracker: FirestoreSyncTracker? =
            firesoreRoomManager!!.firestoreSyncTrackerDao()
                .getSyncTrackerById(collectionName = collectionName, firestoreId = firestoreId).getFirstOrNull()

        if (firestoreSyncTracker == null) {
            // This is new data, create new one
            firestoreSyncTracker = FirestoreSyncTracker(
                firestoreId = firestoreId,
                userId = firebaseUserId,
                collectionName = collectionName,
                createdOn = createdOn,
                updatedOn = updatedOn,
                updatedBy = updatedBy,
                updatedLocally = true,
                deleted = false,
                data = data
            )
            firesoreRoomManager!!.firestoreSyncTrackerDao().insert(firestoreSyncTracker)
        } else {
            // This is an old data, Updated it
            firestoreSyncTracker.updatedOn = updatedOn
            firestoreSyncTracker.updatedBy = updatedBy
            firestoreSyncTracker.updatedLocally = true
            firestoreSyncTracker.data = data

            firesoreRoomManager!!.firestoreSyncTrackerDao().update(firestoreSyncTracker)
        }

        return firestoreSyncTracker
    }

    /**
     * Data to be deleted and synced with firestore to be deleted on other devices
     *
     * @param collectionName table/collection Name
     * @param firestoreId firestoreId to update
     * @param documentToSave data to be saved, must implement FirebaseOfflineDocument interface
     * @see com.tejpratapsingh.firestoreofflinefirst.interfaces.FirebaseOfflineDocument
     */
    @Throws(Exception::class)
    fun deleteData(
        collectionName: String,
        firestoreId: String
    ): FirestoreSyncTracker {
        checkFirestoreInitialised()

        val createdOn: Long = System.currentTimeMillis()
        val updatedOn: Long = System.currentTimeMillis()
        val updatedBy: String = firestoreLocalSettings!!.deviceInstallationId

        var firestoreSyncTracker: FirestoreSyncTracker? =
            firesoreRoomManager!!.firestoreSyncTrackerDao()
                .getSyncTrackerById(collectionName = collectionName, firestoreId = firestoreId).getFirstOrNull()

        if (firestoreSyncTracker == null) {
            // This is new data, create new one
            firestoreSyncTracker = FirestoreSyncTracker(
                firestoreId = firestoreId,
                userId = firebaseUserId,
                collectionName = collectionName,
                createdOn = createdOn,
                updatedOn = updatedOn,
                updatedBy = updatedBy,
                updatedLocally = true,
                deleted = true,
                data = ""
            )

            firesoreRoomManager!!.firestoreSyncTrackerDao().insert(firestoreSyncTracker)
        } else {
            // This is an old data, Updated it
            firestoreSyncTracker.updatedOn = updatedOn
            firestoreSyncTracker.updatedBy = updatedBy
            firestoreSyncTracker.updatedLocally = true
            firestoreSyncTracker.deleted = true
            firestoreSyncTracker.data = ""

            firesoreRoomManager!!.firestoreSyncTrackerDao().update(firestoreSyncTracker)
        }

        return firestoreSyncTracker
    }
}