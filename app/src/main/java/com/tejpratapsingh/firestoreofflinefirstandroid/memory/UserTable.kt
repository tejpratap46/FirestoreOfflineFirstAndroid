package com.tejpratapsingh.firestoreofflinefirstandroid.memory

import com.tejpratapsingh.firestoreofflinefirst.interfaces.FirebaseOfflineDocument

data class UserTable(val userId: String, val name: String, val phone: String) :
    FirebaseOfflineDocument {
    override fun firestoreDocumentRepresentation(): Map<String, Any> {
        return mapOf<String, Any>(
            "userId" to userId,
            "name" to name,
            "phone" to phone
        )
    }
}