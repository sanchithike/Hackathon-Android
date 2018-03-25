package com.roposo.creation.av;

import android.os.Build;

import com.roposo.core.util.FileUtilities;

/**
 * Created by Amud on 18/07/16.
 */

public class SessionConfig implements Cloneable {

    private final VideoEncoderConfig mVideoConfig;
    private final AudioEncoderConfig mAudioConfig;
    private Muxer mMuxer;
    private boolean mConvertVerticalVideo;
    private String mFilePath;


    public SessionConfig( Muxer muxer, VideoEncoderConfig videoConfig, AudioEncoderConfig audioConfig) {
        mVideoConfig = checkNotNull(videoConfig);
        mAudioConfig = checkNotNull(audioConfig);
        mMuxer = muxer;
    }

    public Muxer getMuxer() {
        return mMuxer;
    }

    public int getVideoWidth() {
        return mVideoConfig.getWidth();
    }

    public int getVideoHeight() {
        return mVideoConfig.getHeight();
    }

    public int getVideoBitrate() {
        return mVideoConfig.getBitRate();
    }

    //TODO critical
    // return false if source is Video. True if source is camera.
    public boolean isCamera() {
        return true;
    }

    public int getNumAudioChannels() {
        return mAudioConfig.getNumChannels();
    }

    public int getAudioBitrate() {
        return mAudioConfig.getBitrate();
    }

    public int getAudioSamplerate() {
        return mAudioConfig.getSampleRate();
    }

    public String getOutputFilePath() {
        return mFilePath;
    }

    public boolean isConvertingVerticalVideo() {
        return mConvertVerticalVideo;
    }

    @Override
    public SessionConfig clone() {
        try {
            super.clone();
            return new SessionConfig(getMuxer(), mVideoConfig.clone(), mAudioConfig.clone());
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class Builder {
        private int mWidth;
        private int mHeight;
        private int mVideoBitrate;

        private int mAudioSamplerate;
        private int mAudioBitrate;
        private int mNumAudioChannels;

        private String mFilePath;

        private Muxer mMuxer;
        private boolean mHasVideo, mHasAudio;

        public Builder(String outputLocation) {
            setAVDefaults();
            mFilePath = outputLocation;
            if (FileUtilities.isFileNameVideo(outputLocation)) {
                mMuxer = AndroidMuxer.create(outputLocation, Muxer.FORMAT.MPEG4);
            }
        }

        private void setAVDefaults() {
            mWidth = 1280;
            mHeight = 720;
            mVideoBitrate = 2 * 1000 * 1000;

            mAudioSamplerate = 44100;
            mAudioBitrate = 96 * 1000;
            mNumAudioChannels = 1;
        }

        public Builder withAudioBitrate(int bitRate) {
            mAudioBitrate = bitRate; // Max 96kbps
            return this;
        }

        public Builder withAudioSampleRate(int sampleRate) {
            mAudioSamplerate = sampleRate; // Max 48000 supported by android
            return this;
        }

        public Builder withAudioChannels(int numAudioChannels) {
            mNumAudioChannels = numAudioChannels; // Max 2 channels
            return this;
        }

        public Builder withVideoResolution(int width, int height) {
            mWidth = width;
            mHeight = height;
            return this;
        }

        public Builder withVideoBitrate(int bitrate) {
            mVideoBitrate = bitrate;
            return this;
        }

        public SessionConfig build() {
            SessionConfig session = new SessionConfig( mMuxer,
                    new VideoEncoderConfig(mWidth, mHeight, mVideoBitrate),
                    new AudioEncoderConfig(mNumAudioChannels, mAudioSamplerate, mAudioBitrate));
            session.setOutputFilePath(mFilePath);
            return session;
        }

        public void withVideoDecoded() {
            mHasVideo = true;
            mMuxer.setHasVideo(mHasVideo);
        }

        public void withAudioDecoded() {
            mHasAudio = true;
            mMuxer.setHasAudio(mHasAudio);
        }
    }

    public void setOutputFilePath(String filePath) {
        mFilePath = filePath;
    }

    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    public static boolean isKitKat() {
        return Build.VERSION.SDK_INT >= 19;
    }


}

