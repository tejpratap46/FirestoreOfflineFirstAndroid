package com.tejpratapsingh.firestoreofflinefirstandroid

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tejpratapsingh.firestoreofflinefirst.firestore.FirestoreSyncManager

class SyncManager : FirestoreSyncManager() {
    override fun getFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    override fun getFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
}