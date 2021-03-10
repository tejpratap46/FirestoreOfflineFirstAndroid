package com.tejpratapsingh.firestoreofflinefirst.extensions

import com.google.firebase.firestore.Query
import com.tejpratapsingh.firestoreofflinefirst.firestore.FirestoreSyncManager

/**
 * Add userId field filter for query
 */
fun Query.maskQueryForUser(firebaseUserId: String?): Query {
    if (firebaseUserId != null) {
        return this.whereEqualTo(
            FirestoreSyncManager.FIREBASE_COLLECTION_PROPERTIES.USER_ID.propertyName,
            firebaseUserId
        )
    } else {
        return this
    }
}