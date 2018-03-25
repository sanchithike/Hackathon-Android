package com.roposo.core.util;

/**
 * Created by hchhabra on 01/03/16.
 */
public class DataTracking {
    String type;
    String feedSources; //map with fs
    String eid;

    //adding for search results params from backend
    String query;
    int itemPositionNumber = -1;
    String url;
    String detBlockType;

    public DataTracking() {

    }

    public DataTracking(String ty, String fs) {
        this.type = ty;
        this.feedSources = fs;
    }

    public String getDetBlockType() {
        return detBlockType;
    }

    public void setDetBlockType(String detBlockType) {
        this.detBlockType = detBlockType;
    }

    public String getEid() {
        return eid;
    }

    public void setEid(String eid) {
        this.eid = eid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFeedSources() {
        return feedSources;
    }

    public void setFeedSources(String feedSources) {
        this.feedSources = feedSources;
    }

    public int getItemPositionNumber() {
        return itemPositionNumber;
    }

    public void setItemPositionNumber(int itemPositionNumber) {
        this.itemPositionNumber = itemPositionNumber;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
