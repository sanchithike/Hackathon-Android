package com.roposo.core.kotlinExtensions

import org.json.JSONArray
import org.json.JSONObject

/**
 * @author muddassir on 2/28/18.
 */
inline fun <E> JSONArray?.toArrayList(block: (jsonObject: JSONObject) -> E): ArrayList<E> {
    return if (this == null) ArrayList<E>()
    else (0 until length()).mapIndexedTo(ArrayList()) { index, _ -> block(optJSONObject(index)) }
}

inline fun <E> JSONArray?.toStringArrayList(block: (jsonObject: String) -> E): ArrayList<E> {
    return if (this == null) ArrayList<E>()
    else (0 until length()).mapIndexedTo(ArrayList()) { index, _ -> block(optString(index)) }
}

inline fun <E> MutableCollection<E>?.toJsonArray(block: (E) -> Any): JSONArray {
    return if (this == null) JSONArray()
    else {
        JSONArray().apply { forEach { put(block(it)) } }
    }
}