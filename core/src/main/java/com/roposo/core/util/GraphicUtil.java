package com.roposo.core.util;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @Author : Anil Sharma on 24/10/16.
 */

public class GraphicUtil {

    public static boolean isInCircle(int cX, int cY, int rad, int x, int y) {
        return (x-cX)*(x-cX) + (y-cY)*(y-cY) - rad*rad < 0;
    }

    public static boolean rectIsInCircle(int cx, int cy, int rad, int left, int top, int right, int bottom) {
        if(right > cx - rad && left < cx + rad && top  < cy + rad && bottom > cy - rad) {
            return  isInCircle(cx, cy, rad, left, top) || isInCircle(cx, cy, rad, left, bottom)
                    || isInCircle(cx, cy, rad, right, top) || isInCircle(cx, cy, rad, right, bottom);
        }
        return false;
    }

    public static boolean rectOverlap(int l1, int t1, int r1, int b1, int l2, int t2, int r2, int b2) {
        return r1 > l2 && l1 < r2 && t1  < b2 && b1 > t2;
    }

    public static class Size implements Parcelable {
        public int width;
        public int height;

        public Size() {
        }

        public Size(int width, int height) {
            this.width = width;
            this.height = height;
        }

        /**
         * Parcelable interface methods
         */
        @Override
        public int describeContents() {
            return 0;
        }

        /**
         * Write this point to the specified parcel. To restore a point from
         * a parcel, use readFromParcel()
         * @param out The parcel to write the point's coordinates into
         */
        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(width);
            out.writeInt(width);
        }

        public static final Parcelable.Creator<Size> CREATOR = new Parcelable.Creator<Size>() {
            /**
             * Return a new point from the data in the specified parcel.
             */
            public Size createFromParcel(Parcel in) {
                Size r = new Size();
                r.readFromParcel(in);
                return r;
            }

            /**
             * Return an array of rectangles of the specified size.
             */
            public Size[] newArray(int size) {
                return new Size[size];
            }
        };

        /**
         * Set the point's coordinates from the data stored in the specified
         * parcel. To write a point to a parcel, call writeToParcel().
         *
         * @param in The parcel to read the point's coordinates from
         */
        public void readFromParcel(Parcel in) {
            width = in.readInt();
            height = in.readInt();
        }
    }
}
