package com.roposo.creation.models

import com.roposo.core.util.FileUtilities
import com.roposo.creation.util.Trackable
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.Serializable

/**
 * @author Anil Sharma on 29/05/17.
 */

data class AudioEntry @JvmOverloads constructor(val id: String,
                                                var title: String,
                                                var duration: Int = Int.MIN_VALUE,
                                                var path: String,
                                                var serverPath: String?,
                                                var playStartMili: Int = 0,
                                                var playdurationMili: Int = 0

) : Serializable, Trackable {
    override fun toTrackJson(): JSONObject = JSONObject()
            .put("id", id)
            .put("pd", playdurationMili)
            .put("ps", playStartMili)
            .put("isFromServer", isAudioFromServer)

    var isAudioFromServer = false

    // optional parameters
    var author: String = ""
    var genre: String = ""

    //    public int awsTransferId;
    var isDownloading: Boolean = false
    var volumeLevel: Float? = null
    var isDirty: Boolean = false

    val copy: AudioEntry
        get() {
            val aeCopy = AudioEntry(this.id, this.title, duration, this.path, this.serverPath)
            aeCopy.isAudioFromServer = this.isAudioFromServer
            aeCopy.playStartMili = this.playStartMili
            aeCopy.playdurationMili = this.playdurationMili
            aeCopy.volumeLevel = this.volumeLevel
            return aeCopy
        }

    @Deprecated("")
    constructor(id: Long, title: String, duration: Int, path: String, serverPath: String?) : this(id.toString(), title, duration, path, serverPath)

    fun toJSON(): JSONObject {
        val res = JSONObject()
        try {
            res.run {
                put("id", id)
                put("title", title)
                put("dur", duration)
                put("path", path)
                serverPath?.let {
                    put("svrPath", it)
                }

                put("au", author)
                put("gen", genre)
                put("ps", playStartMili)
                put("pd", playdurationMili)
                put("isFromServer", isAudioFromServer)
                volumeLevel?.let {
                    put("vl", it)
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return res
    }

    companion object {

        fun fromJSON(itemJSON: JSONObject?): AudioEntry? {
            if (null == itemJSON || null == itemJSON.optString("path", null) && null == itemJSON.optString("svrPath", null)) {
                return null
            }


            var entry: AudioEntry? = null
            itemJSON.let {
                entry = AudioEntry(
                        it.optString("id"),
                        it.optString("title"),
                        it.optInt("dur", Integer.MIN_VALUE),
                        it.optString("path", null),
                        it.optString("svrPath", null)
                ).apply {
                    author = it.optString("au", null)
                    genre = it.optString("gen", null)
                    playStartMili = it.optInt("ps", 0)
                    playdurationMili = it.optInt("pd", 0)
                    isAudioFromServer = it.optBoolean("isFromServer")
                    if (it.has("vl")) {
                        volumeLevel = it.optDouble("vl").toFloat()
                    }
                }
            }

            return entry
        }


        fun generatePath(serverPath: String): String? {
            var diskName = serverPath.replace("[\\\\/]".toRegex(), File.separator).replace(".mp3".toRegex(), "")
            val dotIdx = diskName.lastIndexOf(".")
            if (-1 != dotIdx) {
                diskName = diskName.substring(0, dotIdx)
            }
            val f = FileUtilities.generateMediaFile(diskName, FileUtilities.FILE_TYPE_MP3, FileUtilities.MEDIA_DIR_AUDIO)
            return f?.absolutePath

        }
    }
}
