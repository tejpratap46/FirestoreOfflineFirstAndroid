package com.tejpratapsingh.firestoreofflinefirst.firestore

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestoreSettings
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.tejpratapsingh.firestoreofflinefirst.database.FirestoreRoomManager
import com.tejpratapsingh.firestoreofflinefirst.database.table.FirestoreLocalSettings
import com.tejpratapsingh.firestoreofflinefirst.database.table.FirestoreSyncMaster
import com.tejpratapsingh.firestoreofflinefirst.database.table.FirestoreSyncTracker
import com.tejpratapsingh.firestoreofflinefirst.extensions.getFirstOrNull
import com.tejpratapsingh.firestoreofflinefirst.extensions.maskQueryForUser
import com.tejpratapsingh.firestoreofflinefirst.utilities.Utils
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

abstract class FirestoreSyncManager {

    enum class FIREBASE_COLLECTION_PROPERTIES(val propertyName: String) {
        USER_ID("userId"),
        CREATED_ON("createdOn"),
        UPDATED_ON("updatedOn"),
        UPLOADED_AT("uploadedAt"),
        UPDATED_BY("updatedBy"),
        DELETED("deleted")
    }

    enum class USER_DEVICE_PROPERTIES(val propertyName: String) {
        DEVICE_ID("deviceId"),
        USER_ID(FIREBASE_COLLECTION_PROPERTIES.USER_ID.propertyName),
        UPLOADED_AT(FIREBASE_COLLECTION_PROPERTIES.UPLOADED_AT.propertyName)
    }

    enum class FIREBASE_COLLECTION(val collectionName: String) {
        USER_DEVICES("userDevices")
    }

    companion object {
        private val TAG = "FirestoreSyncManager"

        private val syncLock: Any = Any()

        private var isSyncRunning = false
    }

    protected abstract fun getFirestore(): FirebaseFirestore
    protected abstract fun getFirebaseAuth(): FirebaseAuth

    private val activeDeviceList: ArrayList<String> = ArrayList()

    /**
     * Start firestore upload and dowload
     *
     * @param applicationContext application context
     * @param firestore firestore instance
     * @param firebaseUserId Firebase Auth User Id
     */
    @Throws(Exception::class)
    open fun startSync(
        applicationContext: Context
    ) {
        Log.d(TAG, "startSync: called")
        if (isSyncRunning) {
            throw Exception("Sync already runnung")
        }

        // Activate lock
        synchronized(syncLock) {
            isSyncRunning = true;
        }

        val firebaseUserId: String? = getFirebaseAuth().currentUser?.uid

        Log.d(TAG, "startSync: firebaseUserId: $firebaseUserId")

        val settings = firestoreSettings {
            isPersistenceEnabled = false
        }
        getFirestore().firestoreSettings = settings

        val firesoreRoomManager: FirestoreRoomManager =
            FirestoreRoomManager.getInstance(applicationContext)

        // get local settings
        val firestoreLocalSettings: FirestoreLocalSettings? =
            firesoreRoomManager.firestoreLocalSettingsDao().getLocalSettings().getFirstOrNull()

        if (firestoreLocalSettings == null) {
            Log.e(TAG, "startSync: error", Exception("FirestoreOfflineManager not initialised"))
            throw Exception("FirestoreOfflineManager not initialised")
        }

        // get devices after last upload
        val minDownloadSyncMaster: FirestoreSyncMaster? =
            firesoreRoomManager.firestoreSyncMasterDao().getMinSyncMaster().getFirstOrNull()

        var minDownloadedTill: Long = 0

        minDownloadSyncMaster?.let { firestoreSyncMaster ->
            minDownloadedTill = firestoreSyncMaster.downloadedTill
        }

        val userDeviceQuery: QuerySnapshot = Tasks.await(
            getFirestore().collection(FIREBASE_COLLECTION.USER_DEVICES.collectionName)
                .whereGreaterThan(
                    USER_DEVICE_PROPERTIES.UPLOADED_AT.propertyName,
                    Date(minDownloadedTill)
                )
                .maskQueryForUser(firebaseUserId)
                .orderBy(USER_DEVICE_PROPERTIES.UPLOADED_AT.propertyName, Query.Direction.ASCENDING)
                .get(Source.SERVER)
        )

        if (userDeviceQuery.documents.isNotEmpty()) {
            userDeviceQuery.documents.forEach { userDeviceDocument ->
                userDeviceDocument.getString(USER_DEVICE_PROPERTIES.DEVICE_ID.propertyName)
                    ?.let { deviceId ->
                        activeDeviceList.add(deviceId)
                    }
            }
        }

        // Remove current device, as we do not want to fetch changes that are made by this device again
        activeDeviceList.remove(firestoreLocalSettings.deviceInstallationId)

        // Download Changes from firestore
        val syncMasters: List<FirestoreSyncMaster> =
            firesoreRoomManager.firestoreSyncMasterDao().getAllSyncMaster()
        syncMasters.forEach { firestoreSyncMaster ->
            // Download updated docs from server one by one
            val collectionName: String = firestoreSyncMaster.collectionName
            val downloadedTill: Long = firestoreSyncMaster.downloadedTill

            val querySnapshot: QuerySnapshot = Tasks.await(
                getFirestore().collection(collectionName)
                    .whereGreaterThan(
                        FIREBASE_COLLECTION_PROPERTIES.UPLOADED_AT.propertyName,
                        Date(downloadedTill)
                    )
                    .whereIn(
                        FIREBASE_COLLECTION_PROPERTIES.UPDATED_BY.propertyName,
                        activeDeviceList
                    )
                    .maskQueryForUser(firebaseUserId)
                    .orderBy(
                        FIREBASE_COLLECTION_PROPERTIES.UPLOADED_AT.propertyName,
                        Query.Direction.ASCENDING
                    )
                    .get(Source.SERVER)
            )

            querySnapshot.documents.forEach { documentSnapshot ->
                // Save Trackers
                val firestoreId: String = documentSnapshot.id
                val documentUserId: String? =
                    documentSnapshot.getString(FIREBASE_COLLECTION_PROPERTIES.USER_ID.propertyName)
                val createdOn: Long =
                    documentSnapshot.getLong(FIREBASE_COLLECTION_PROPERTIES.CREATED_ON.propertyName)
                        ?: 0
                val updatedOn: Long =
                    documentSnapshot.getLong(FIREBASE_COLLECTION_PROPERTIES.UPDATED_ON.propertyName)
                        ?: 0
                val uploadedAt: Long =
                    (documentSnapshot.getTimestamp(FIREBASE_COLLECTION_PROPERTIES.UPLOADED_AT.propertyName)
                        ?: Timestamp.now()).toDate().time
                val updatedBy: String =
                    documentSnapshot.getString(FIREBASE_COLLECTION_PROPERTIES.UPDATED_BY.propertyName)
                        ?: ""
                val deleted: Boolean =
                    documentSnapshot.getBoolean(FIREBASE_COLLECTION_PROPERTIES.DELETED.propertyName)
                        ?: false

                var firestoreSyncTracker: FirestoreSyncTracker? =
                    firesoreRoomManager.firestoreSyncTrackerDao()
                        .getSyncTrackerById(
                            collectionName = collectionName,
                            firestoreId = firestoreId
                        ).getFirstOrNull()

                val dataMap: MutableMap<String, Any> = documentSnapshot.data ?: HashMap()
                FIREBASE_COLLECTION_PROPERTIES.values().forEach { property ->
                    dataMap.remove(property.propertyName)
                }
                val data: String = Utils.getMapToJson(dataMap)

                if (firestoreSyncTracker == null) {
                    // This is new data, create new one
                    firestoreSyncTracker = FirestoreSyncTracker(
                        firestoreId = firestoreId,
                        userId = documentUserId ?: "",
                        collectionName = collectionName,
                        createdOn = createdOn,
                        updatedOn = updatedOn,
                        updatedBy = updatedBy,
                        updatedLocally = false,
                        deleted = deleted,
                        data = data
                    )

                    firesoreRoomManager.firestoreSyncTrackerDao().insert(firestoreSyncTracker)
                } else if (firestoreSyncTracker.updatedOn > updatedOn) {
                    // This has been updated on server and is newer, update it locally
                    firestoreSyncTracker.updatedOn = updatedOn
                    firestoreSyncTracker.updatedBy = updatedBy
                    firestoreSyncTracker.updatedLocally = false
                    firestoreSyncTracker.deleted = deleted
                    firestoreSyncTracker.data = data

                    firesoreRoomManager.firestoreSyncTrackerDao().update(firestoreSyncTracker)
                }

                // Update sync master
                firestoreSyncMaster.downloadedTill = uploadedAt
                firesoreRoomManager.firestoreSyncMasterDao().update(firestoreSyncMaster)
            }
        }

        // get all pending changes and write to firestore
        val updatedDocuments: List<FirestoreSyncTracker> =
            firesoreRoomManager.firestoreSyncTrackerDao().getAllUpdatedDocuments(true)

        Log.d(TAG, "startSync: Updated Locally Docs: " + updatedDocuments.size)

        updatedDocuments.forEach { firestoreSyncTracker ->
            // upload document
            val dataMap: MutableMap<String, Any> = Utils.getJsonToMap(firestoreSyncTracker.data)
            if (firestoreSyncTracker.userId != null) {
                dataMap[FIREBASE_COLLECTION_PROPERTIES.USER_ID.propertyName] =
                    firestoreSyncTracker.userId
            }
            dataMap[FIREBASE_COLLECTION_PROPERTIES.CREATED_ON.propertyName] =
                firestoreSyncTracker.createdOn
            dataMap[FIREBASE_COLLECTION_PROPERTIES.UPDATED_ON.propertyName] =
                firestoreSyncTracker.updatedOn
            dataMap[FIREBASE_COLLECTION_PROPERTIES.UPDATED_BY.propertyName] =
                firestoreSyncTracker.updatedBy
            dataMap[FIREBASE_COLLECTION_PROPERTIES.UPLOADED_AT.propertyName] =
                FieldValue.serverTimestamp()
            dataMap[FIREBASE_COLLECTION_PROPERTIES.DELETED.propertyName] =
                firestoreSyncTracker.deleted

            Tasks.await(
                getFirestore().collection(firestoreSyncTracker.collectionName)
                    .document(firestoreSyncTracker.firestoreId).set(dataMap)
            )
            Log.d(
                TAG,
                "startSync: Document Uploaded: $firestoreSyncTracker.collectionName ($firestoreSyncTracker.firestoreId)"
            )

            // Update Sync Tracker
            firestoreSyncTracker.updatedLocally = false
            firesoreRoomManager.firestoreSyncTrackerDao().update(firestoreSyncTracker)
        }

        // Register device
        if (updatedDocuments.isNotEmpty()) {
            registerDevice(
                firestoreLocalSettings = firestoreLocalSettings,
                firestore = getFirestore(),
                firebaseUserId = firebaseUserId
            )
        }

        // Release lock
        synchronized(syncLock) {
            isSyncRunning = false;
        }
    }

    /**
     * Register user device instance to firestore,
     * we can use this deviceId to find which other devices were used by user
     * and only download data which are uploaded by other devices NOT this
     *
     * @param firestoreLocalSettings local settings to know about this device
     * @param firestore instance to connect to firestore
     * @param userId assign to user
     *
     * @return true if success
     */
    fun registerDevice(
        firestoreLocalSettings: FirestoreLocalSettings,
        firestore: FirebaseFirestore,
        firebaseUserId: String?
    ): Boolean {
        val dataMap: MutableMap<String, Any> = HashMap()
        dataMap[USER_DEVICE_PROPERTIES.DEVICE_ID.propertyName] =
            firestoreLocalSettings.deviceInstallationId
        dataMap[USER_DEVICE_PROPERTIES.UPLOADED_AT.propertyName] = FieldValue.serverTimestamp()
        if (firebaseUserId != null) {
            dataMap[USER_DEVICE_PROPERTIES.USER_ID.propertyName] = firebaseUserId
        }

        Tasks.await(
            firestore.collection(FIREBASE_COLLECTION.USER_DEVICES.collectionName)
                .document(firestoreLocalSettings.deviceInstallationId).set(dataMap)
        )

        return true
    }

    /**
     * Get data Successfully backed-up to firestore
     *
     * @return List if trackers that has been successfully uploaded to firestore
     */
    fun getServerUpdatedDocuments(applicationContext: Context): List<FirestoreSyncTracker> {
        val firesoreRoomManager: FirestoreRoomManager =
            FirestoreRoomManager.getInstance(applicationContext)

        val updatedDocuments: List<FirestoreSyncTracker> =
            firesoreRoomManager.firestoreSyncTrackerDao().getAllUpdatedDocuments(false)

        return updatedDocuments
    }

    /**
     * Upload DB backup to FirebaseStorage in current thread
     *
     * @param firebaseStorage FirebaseStorage instance
     * @param databaseFile database file to upload
     * @param firebaseUserId Firebase Auth UserId, to identify which file belongs to user
     * @return uploaded file's `StorageReference`
     */
    @Throws(Exception::class)
    fun createDBBackup(
        firebaseStorage: FirebaseStorage,
        databaseFile: File,
        firebaseUserId: String
    ): StorageReference {
        if (databaseFile.exists() == false) {
            throw Exception("Database File not found")
        }

        val dbReference: StorageReference =
            firebaseStorage.reference.child("database").child(firebaseUserId)

        val uploadTask: UploadTask = dbReference.putFile(Uri.fromFile(databaseFile))

        Tasks.await(uploadTask)

        return dbReference
    }

    /**
     * Download File in current thread
     *
     * @param firebaseStorage FirebaseStorage instance
     * @param firebaseUserId Firebase Auth UserId, to identify which file belongs to user
     * @param fileToSave file be saved, You can use below function to create a temp file
     * <pre>
     * {@code
     * File.createTempFile("${System.currentTimeMillis()}", ".db")
     * }
     * </pre>
     *
     * @return saved file
     */
    @Throws(Exception::class)
    fun downloadDBBackup(
        firebaseStorage: FirebaseStorage,
        firebaseUserId: String,
        fileToSave: File
    ): File {
        val dbReference: StorageReference =
            firebaseStorage.reference.child("database").child(firebaseUserId)

        val fileDownloadTask: FileDownloadTask = dbReference.getFile(fileToSave)

        Tasks.await(fileDownloadTask)

        return fileToSave
    }
}