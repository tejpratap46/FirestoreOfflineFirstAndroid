package com.tejpratapsingh.firestoreofflinefirst.utilities

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


object Utils {
    
    fun getJsonToMap(json: String): MutableMap<String, Any> {
        val gson = Gson()
        val type: Type = object : TypeToken<MutableMap<String, Any>>() {}.type
        return gson.fromJson(json, type)
    }

    fun getMapToJson(map: Map<String, Any>): String {
        val gson = Gson()
        return gson.toJson(map)
    }
}