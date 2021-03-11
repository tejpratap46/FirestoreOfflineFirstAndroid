package com.tejpratapsingh.firestoreofflinefirst.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName

class FirestoreCostTracker {

    @SerializedName("totalRead")
    var totalRead: Long = 0

    @SerializedName("totalWrite")
    var totalWrite: Long = 0

    fun getAsJson(): String {
        val gson: Gson = GsonBuilder().create()

        return gson.toJson(this)
    }
}