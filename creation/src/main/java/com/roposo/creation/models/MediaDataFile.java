package com.roposo.creation.models;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Harsh on 2/13/18.
 */

public class MediaDataFile extends RealmObject {
    @PrimaryKey
    public String _iD;

    public static int FILE_UPLOADING = -1;
    public static int FILE_UPLOAD_FAILED = 0;
    public static int FILE_UPLOADED = 1;

    private String storyId;
    public String localFilePath;
    public String serverPath;
    public int status;
    public boolean isUploaded = false;
    public int transferId = Integer.MIN_VALUE;



    public MediaDataFile(){

    }

    public MediaDataFile(String localFilePath, String id, String storyId){
        this.localFilePath = localFilePath;
        this._iD = id;
        this.storyId = storyId;

    }

    public boolean isUploading() {
        return status == FILE_UPLOADING;
    }

    public boolean isUploaded() {
        return status == FILE_UPLOADED;
    }

    public boolean isUploadFailed() {
        return status == FILE_UPLOAD_FAILED;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public String getServerPath() {
        return serverPath;
    }


    public void setUploaded(boolean b) {
        status = FILE_UPLOADED;
        isUploaded = true;
    }
}
