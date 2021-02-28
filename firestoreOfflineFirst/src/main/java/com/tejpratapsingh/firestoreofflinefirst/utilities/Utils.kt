package com.tejpratapsingh.firestoreofflinefirst.utilities

import com.google.gson.Gson


class Utils {

    companion object {
        fun getJsonToMap(json: String): HashMap<*, *>? {
            val gson = Gson()
            return gson.fromJson(json, HashMap::class.java)
        }

        fun getMapToJson(map: HashMap<String, Any>): String {
            val gson = Gson()
            return gson.toJson(map)
        }
    }

}