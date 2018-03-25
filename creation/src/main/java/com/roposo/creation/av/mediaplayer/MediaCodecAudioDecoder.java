/*
 * Copyright 2016 Mario Guggenberger <mg@protyposis.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.roposo.creation.av.mediaplayer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;

/**
 * Created by Mario on 20.04.2016.
 */
class MediaCodecAudioDecoder extends MediaCodecDecoder {

    private static final boolean VERBOSE = false;
    private AudioPlayback mAudioPlayback;
    private boolean mPlayAudio;

    public MediaCodecAudioDecoder(MediaExtractor extractor, boolean passive, int trackIndex,
                                  OnDecoderEventListener listener, AudioPlayback audioPlayback,
                                  boolean playAudio)
            throws IOException {
        super(extractor, passive, trackIndex, listener);
        mAudioPlayback = audioPlayback;
        this.mPlayAudio = playAudio;
        reinitCodec();
    }

    @Override
    protected void configureCodec(MediaCodec codec, MediaFormat format) {
        super.configureCodec(codec, format);
        mAudioPlayback.init(format);
    }

    @Override
    protected boolean shouldDecodeAnotherFrame() {
        // If this is an active audio track, decode and buffer only as much as this arbitrarily
        // chosen threshold time to avoid filling up the memory with buffered audio data and
        // requesting too much data from the network too fast (e.g. DASH segments).
        if (!isPassive()) {
            return mAudioPlayback.getQueueBufferTimeUs() < 200000;
        } else {
            return super.shouldDecodeAnotherFrame();
        }
    }

    @Override
    public void renderFrame(FrameInfo frameInfo, long offsetUs) {
        if (mPlayAudio)
            mAudioPlayback.write(frameInfo.data, frameInfo.presentationTimeUs);
        if (VERBOSE) {
            if (!mPlayAudio) {
                Log.d(TAG, "frameInfo: " + frameInfo);
            }
        }
        mOnDecoderEventListener.onAudioFrameRendered(frameInfo, offsetUs);
        releaseFrame(frameInfo);
    }

    @Override
    protected void onOutputFormatChanged(MediaFormat format) {
        mAudioPlayback.init(format);
    }
}
