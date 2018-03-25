package com.roposo.creation.av;

/**
 * Created by Amud on 18/07/16.
 */

public class VideoEncoderConfig implements Cloneable {
    protected final int mWidth;
    protected final int mHeight;
    protected final int mBitRate;

    public VideoEncoderConfig(int width, int height, int bitRate) {
        mWidth = width;
        mHeight = height;
        mBitRate = bitRate;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getBitRate() {
        return mBitRate;
    }

    @Override
    public VideoEncoderConfig clone() {
        try {
            super.clone();
            return new VideoEncoderConfig(mWidth, mHeight, mBitRate);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        return "VideoEncoderConfig: " + mWidth + "x" + mHeight + " @" + mBitRate + " bps";
    }
}
