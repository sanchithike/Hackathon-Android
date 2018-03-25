package com.roposo.creation.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.roposo.core.util.textUtlis.WordUtils;

/**
 * Created by anilshar on 3/7/16.
 */
public class UserLocation implements Parcelable {
    /**
     * Static field used to regenerate object, individually or as arrays
     */
    public static final Parcelable.Creator<UserLocation> CREATOR = new Parcelable.Creator<UserLocation>() {
        public UserLocation createFromParcel(Parcel pc) {
            return new UserLocation(pc);
        }

        public UserLocation[] newArray(int size) {
            return new UserLocation[size];
        }
    };
    private String placeName;
    private String placeDescription;
    private double latitude = Double.NaN;
    private double longitude = Double.NaN;


    public UserLocation(String placeName) {
        this.placeName = WordUtils.capitalize(placeName);
    }

    private UserLocation(Parcel pc) {
        this.placeName = pc.readString();
        this.latitude = pc.readDouble();
        this.longitude = pc.readDouble();
    }

    public String getPlaceName() {
        return placeName;
    }

    public String getPlaceDescription() {
        return placeDescription;
    }

    public void setPlaceDescription(String desc) {
        placeDescription = desc;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLatLng(double lat, double lng) {
        latitude = lat;
        longitude = lng;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(placeName);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
    }


}
