package com.tejpratapsingh.firestoreofflinefirst.interfaces

interface FirebaseOfflineDocument {

    fun firestoreDocumentRepresentation(): HashMap<String, Any>

}