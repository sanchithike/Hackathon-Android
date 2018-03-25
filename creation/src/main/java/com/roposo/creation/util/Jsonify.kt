package com.roposo.creation.util

import org.json.JSONObject

/**
 * @author muddassir on 12/27/17.
 */

interface Jsonify {
    fun toJson(): JSONObject
}

interface Trackable {
    fun toTrackJson(): JSONObject
}
