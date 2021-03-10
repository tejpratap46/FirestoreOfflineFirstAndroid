package com.tejpratapsingh.firestoreofflinefirstandroid.memory

import com.tejpratapsingh.firestoreofflinefirst.interfaces.FirebaseOfflineDocument

class UserTable(val userId: String, val name: String, val phone: String) : FirebaseOfflineDocument {
    override fun firestoreDocumentRepresentation(): HashMap<String, Any> {
        val data: HashMap<String, Any> = HashMap()

        data.put("userId", userId)
        data.put("name", name)
        data.put("phone", phone)

        return data
    }
}