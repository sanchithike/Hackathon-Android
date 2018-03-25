/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.roposo.creation.graphics.gles;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.FloatBuffer;
import java.util.HashMap;

/**
 * GL program and supporting functions for textured 2D shapes.
 */
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public class Program {
    @SuppressWarnings("PointlessBooleanExpression")
    private static final boolean VERBOSE = false || GraphicsUtils.VERBOSE;
    private static final String TAG = GlUtil.TAG;
    private String mVertexShader;
    private String mFragmentShader;

    private boolean mUse;

    // Handles to the GL program and various components of it.
    public int mProgramHandle;

    HashMap<String, Integer> mAttributes;
    HashMap<String, Integer> mUniforms;

    private int generateProgram(String vertexShader, String fragmentShader) {
        mProgramHandle = Caches.createProgram(vertexShader, fragmentShader);
        if (mProgramHandle == 0) {
            Log.e(TAG, "Error while creating Program");
        } else {
            Log.d(TAG, "Created program " + mProgramHandle);
            mVertexShader = vertexShader;
            mFragmentShader = fragmentShader;
        }
        return mProgramHandle;
    }

    public Program(String vertexShader, String fragmentShader) {
        mUniforms = new HashMap<>();
        mAttributes = new HashMap<>();

        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;
        generateProgram(vertexShader, fragmentShader);
    }

    /**
     * Releases the program.
     * <p/>
     * The appropriate EGL context must be current (i.e. the one that was used to create
     * the program).
     */
    public void release() {
        Log.d(TAG, "deleting program " + mProgramHandle);
        GLES20.glDeleteProgram(mProgramHandle);
        mProgramHandle = -1;
    }

    void setAttribPointer(String attrib, int vecSize, int value) {
        setAttribPointer(attrib, vecSize, 1, value);
    }

    void setAttribPointer(String attrib, int vecSize, int count, int position) {
        int slot = getAttrib(attrib);
        if (slot < 0) {
            if (VERBOSE) Log.w(TAG, "slot returns invalid value for " + attrib);
            return;
        }
        GLES20.glEnableVertexAttribArray(slot);
        GLES20.glVertexAttribPointer(slot, vecSize, GLES20.GL_FLOAT, false, 0, count * vecSize *
                Caches.BYTES_PER_FLOAT * position);
        Caches.checkGlError("setAttribPointer" + attrib);
    }

    void setInteger(final String uniform, final int intValue) {
        final int slot = getUniform(uniform);
        if (slot < 0) {
            if (VERBOSE) Log.w(TAG, "slot returns invalid value for " + uniform);
            return;
        }
        setInteger(slot, intValue);
    }

    @Deprecated
    void setInteger(final int slot, final int intValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                uniform1i(slot, intValue);
            }
        });
    }

    public void uniform1i(String uniform, int value) {
        final int slot = getUniform(uniform);
        if (slot < 0) {
            if (VERBOSE) Log.w(TAG, "slot returns invalid value for " + uniform);
            return;
        }
        uniform1i(slot, value);
    }

    void uniform1i(int slot, int value) {
        GLES20.glUniform1i(slot, value);
        Caches.checkGlError("uniform1i: " + "slot: " + slot + " value: " + value);
    }

    @Deprecated
    void setFloat(final String uniform, final float floatValue) {
        final int slot = getUniform(uniform);
        if (slot < 0) {
            if (VERBOSE) Log.w(TAG, "slot returns invalid value for " + uniform);
            return;
        }
        setFloat(slot, floatValue);
    }

    @Deprecated
    void setFloat(final int slot, final float floatValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                uniform1f(slot, floatValue);
            }
        });
    }

    public void uniform1f(String uniform, float floatValue) {
        final int slot = getUniform(uniform);
        if (slot < 0) {
            if (VERBOSE) Log.w(TAG, "slot returns invalid value for " + uniform);
            return;
        }
        uniform1f(slot, floatValue);
    }

    void uniform1f(int slot, float value) {
        GLES20.glUniform1f(slot, value);
    }

    @Deprecated
    void setFloatVec2(final String uniform, final float[] arrayValue) {
        final int slot = getUniform(uniform);
        if (slot < 0) {
            if (VERBOSE) Log.w(TAG, "slot returns invalid value for " + uniform);
            return;
        }
        setFloatVec2(slot, arrayValue);
    }

    @Deprecated
    void setFloatVec2(final int slot, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                uniform2fv(slot, arrayValue);
            }
        });
    }

    public void uniform2fv(String uniform, float[] floatValue) {
        final int slot = getUniform(uniform);
        if (slot < 0) {
            if (VERBOSE) Log.w(TAG, "slot returns invalid value for " + uniform);
            return;
        }
        uniform2fv(slot, floatValue);
    }

    void uniform2fv(int slot, float[] matrix) {
        GLES20.glUniform2fv(slot, 1, FloatBuffer.wrap(matrix));
    }

    void setFloatVec3(final String uniform, final float[] arrayValue) {
        final int slot = getUniform(uniform);
        if (slot < 0) {
            if (VERBOSE) Log.w(TAG, "slot returns invalid value for " + uniform);
            return;
        }
        setFloatVec3(slot, arrayValue);
    }

    @Deprecated
    void setFloatVec3(final int slot, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform3fv(slot, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    public void uniform3fv(String uniform, float[] floatValue) {
        final int slot = getUniform(uniform);
        if (slot < 0) {
            if (VERBOSE) Log.w(TAG, "slot returns invalid value for " + uniform);
            return;
        }
        uniform3fv(slot, floatValue);
    }

    public void uniform3fv(int slot, float[] matrix) {
        GLES20.glUniform3fv(slot, 1, FloatBuffer.wrap(matrix));
    }

    public void setFloatVec4(final String uniform, final float[] arrayValue) {
        final int slot = getUniform(uniform);
        if (slot < 0) {
            if (VERBOSE) Log.w(TAG, "slot returns invalid value for " + uniform);
            return;
        }
        setFloatVec4(slot, arrayValue);
    }

    @Deprecated
    void setFloatVec4(final int slot, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform4fv(slot, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    public void uniform4fv(String uniform, float[] floatValue) {
        final int slot = getUniform(uniform);
        if (slot < 0) {
            if (VERBOSE) Log.w(TAG, "slot returns invalid value for " + uniform);
            return;
        }
        uniform4fv(slot, floatValue);
    }

    public void uniform4fv(int slot, float[] matrix) {
        GLES20.glUniform4fv(slot, 1, FloatBuffer.wrap(matrix));
    }

    @Deprecated
    void setFloatArray(final String uniform, final float[] arrayValue) {
        final int slot = getUniform(uniform);
        if (slot < 0) {
            if (VERBOSE) Log.w(TAG, "slot returns invalid value for " + uniform);
            return;
        }
        setFloatArray(slot, arrayValue);
    }

    @Deprecated
    void setFloatArray(final int slot, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                uniform1fv(slot, arrayValue);
            }
        });
    }


    void uniform1fv(String uniform, float[] matrix) {
        final int slot = getUniform(uniform);
        if (slot < 0) {
            if (VERBOSE) Log.w(TAG, "slot returns invalid value for " + uniform);
            return;
        }
        GLES20.glUniform1fv(slot, matrix.length, FloatBuffer.wrap(matrix));
    }

    void uniform1fv(int slot, float[] matrix) {
        GLES20.glUniform1fv(slot, matrix.length, FloatBuffer.wrap(matrix));
    }

    @Deprecated
    void setUniformMatrix3f(final String uniform, final float[] matrix) {
        final int slot = getUniform(uniform);
        if (slot < 0) {
            if (VERBOSE) Log.w(TAG, "slot returns invalid value for " + uniform);
            return;
        }
        setUniformMatrix3f(slot, matrix);
    }

    @Deprecated
    void setUniformMatrix3f(final int slot, final float[] matrix) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                uniformMatrix3fv(slot, matrix);
            }
        });
    }

    void uniformMatrix3fv(int slot, float[] matrix) {
        GLES20.glUniformMatrix3fv(slot, 1, false, matrix, 0);
    }

    @Deprecated
    void setUniformMatrix4f(final String uniform, final float[] matrix) {
        final int slot = getUniform(uniform);
        if (slot < 0) {
            if (VERBOSE) Log.w(TAG, "slot returns invalid value for " + uniform);
            return;
        }
        setUniformMatrix4f(slot, matrix);
    }

    @Deprecated
    void setUniformMatrix4f(final int slot, final float[] matrix) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                uniformMatrix4fv(slot, matrix);
            }
        });
    }

    private void runOnDraw(Runnable runnable) {
        // TODO supercritical
    }

    void uniformMatrix4fv(String uniform, float[] matrix) {
        final int slot = getUniform(uniform);
        if (slot < 0) {
            if (VERBOSE) Log.w(TAG, "slot returns invalid value for " + uniform);
            return;
        }
        uniformMatrix4fv(slot, matrix);
    }

    void uniformMatrix4fv(int slot, float[] matrix) {
        GLES20.glUniformMatrix4fv(slot, 1, false, matrix, 0);
    }

    int addAttrib(final String name) {
        int slot = GLES20.glGetAttribLocation(mProgramHandle, name);
        Caches.mGlUtil.checkGlError("glGetAttribLocation");
        if (slot < 0) {
            Log.e(TAG, "getAttribLocation return -1");
        }
        mAttributes.put(name, slot);
        return slot;
    }

    int bindAttrib(final String name, int bindingSlot) {
        GLES20.glBindAttribLocation(mProgramHandle, bindingSlot, name);
        mAttributes.put(name, bindingSlot);
        return bindingSlot;
    }

    public int getAttrib(final String name) {
        if (mAttributes.containsKey(name)) {
            return mAttributes.get(name);
        }
        return addAttrib(name);
    }

    int addUniform(final String name) {
        int slot = GLES20.glGetUniformLocation(mProgramHandle, name);
        Caches.mGlUtil.checkGlError("addUniform :: glGetUniformLocation: " + name);
        if (slot < 0) {
            Log.e(TAG, "getUniformLocation return -1 for :: " + name);
        }
        mUniforms.put(name, slot);
        return slot;
    }

    public int getUniform(final String name) {
        if (mUniforms.containsKey(name)) {
            return mUniforms.get(name);
        }
        return addUniform(name);
    }

    void setupTexture(int textureUnit) {
        uniform1i(ProgramCache.getIndexedVariable(ProgramCache.SHADER_VAR_SAMPLER, textureUnit), textureUnit);
    }

    public void use() {
        GLES20.glUseProgram(mProgramHandle);
        Caches.mGlUtil.checkGlError("use() :: glUseProgram");
        mUse = true;
    }

    public void unUse() {
        GLES20.glUseProgram(0);
        Caches.mGlUtil.checkGlError("use() :: glUnuseProgram");
        mUse = false;
    }

    boolean isInUse() {
        return mUse;
    }

    void remove() {
        mUse = false;
    }

    public String getVertexShader() {
        return mVertexShader;
    }

    public String getFragmentShader() {
        return mFragmentShader;
    }
}
