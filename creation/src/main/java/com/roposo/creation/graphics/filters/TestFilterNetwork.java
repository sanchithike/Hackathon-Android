package com.roposo.creation.graphics.filters;

import android.util.Log;

import com.roposo.creation.graphics.gles.Caches;
import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.ProgramCache;

import java.util.Arrays;
import java.util.List;

/**
 * Created by tajveer on 12/5/17.
 */

public class TestFilterNetwork extends ImageFilter {

    MonoFilter monoFilter;
    MirrorFilter mirrorFilter;
    private List<ImageFilter> filters;

    public TestFilterNetwork() {
        monoFilter = new MonoFilter();
        mirrorFilter = new MirrorFilter();

        filters = Arrays.asList(monoFilter, mirrorFilter);
    }
/*
    @Override
    void copy(BaseFilter baseFilter) {
        super.copy(baseFilter);

        TestFilterNetwork testFilterNetwork = (TestFilterNetwork) baseFilter;
        monoFilter = (MonoFilter) testFilterNetwork.monoFilter.copy();
        mirrorFilter = (MirrorFilter) testFilterNetwork.mirrorFilter.copy();

        filters = Arrays.asList(monoFilter, mirrorFilter);
    }

    @Override
    public void setup(Caches cache) {
        mCache = cache;
        Log.d(TAG, "setup filter: " + this);
        if (mDescription == null) {
            Log.w(TAG, "Exit! Description is null");
            return;
        }

        monoFilter.setWriteToFBO(true);
        setDescAndStuff(cache, monoFilter);

        mirrorFilter.setInFrameBufferTexture(monoFilter.getOutFrameBufferTexture());
        setDescAndStuff(cache, mirrorFilter);

        mIsInitialized = true;
    }

    private void setDescAndStuff(Caches cache, ImageFilter filter) {
        ProgramCache.ProgramDescription description = ProgramCache.ProgramDescription.clone
                (mDescription);
        description.setFilterMode(filter.mFilterMode);

        description.mOffscreenSurface = isWriteToFBO();
        if (filter.isReadFromFBO()) {
            description.isFBOBased = true;
            description.hasTexture = true;
            description.hasExternalTexture = false;
        } else {
            description.isFBOBased = false;
        }
        filter.setDescription(description);
        filter.setPreScale(sScale);

        filter.setup(cache);
    }

    public void draw(Drawable drawable) {
        runPendingOnDrawTasks();
        if (!isInitialized()) {
            return;
        }

        monoFilter.draw(drawable);
        mirrorFilter.draw(drawable);
    }*/
}
