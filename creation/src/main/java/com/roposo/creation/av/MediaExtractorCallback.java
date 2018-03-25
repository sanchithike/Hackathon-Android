package com.roposo.creation.av;

import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

/**
 * Created by bajaj on 18/08/16.
 */
public interface MediaExtractorCallback {
    void onMediaExtractorPrepared(SessionConfig config);
    void onMediaExtractionFinished();

    /**
     * called every frame before time adjusting
     * return true if you don't want to use internal time adjustment
     */
    boolean onFrameAvailable(long presentationTimeUs);

    void sendAudioFrame(ByteBuffer data, BufferInfo bufferInfo, boolean eos);
//    void sendDirectAudioFrame(ByteBuffer data, BufferInfo bufferInfo);
    void sendDirectAudioFrame(Muxer.MediaFrame mediaFrame);

    void addTrack(MediaFormat format);
}
