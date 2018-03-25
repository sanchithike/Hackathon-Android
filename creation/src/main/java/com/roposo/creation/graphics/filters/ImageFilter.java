package com.roposo.creation.graphics.filters;

import android.opengl.GLES20;
import android.util.Log;

import com.roposo.creation.graphics.gles.Caches;
import com.roposo.creation.graphics.gles.Program;
import com.roposo.creation.graphics.gles.ProgramCache;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by bajaj on 08/10/17.
 */

public class ImageFilter extends BaseFilter {

    public static String NO_FILTER_VERTEX_SHADER = "" +
            "precision mediump float;\n" +
            "attribute vec3 position;\n" +
            "attribute vec2 outPosition;\n" +
            "attribute vec2 texCoords;\n" +
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 mainTextureTransform;\n" +
            "varying vec2 outTexCoordinates;\n" +
            "\n" +
            "void main() {\n" +
            "    outTexCoordinates = (mainTextureTransform * vec4(texCoords, 0.0, 1.0)).xy;\n" +
            "    vec4 transformedPosition = uMVPMatrix * vec4(position, 1.0);\n" +
            "    gl_Position = transformedPosition;\n" +
            "}";

    public static String NO_FILTER_FRAGMENT_SHADER = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 outTexCoordinates;\n" +
            "uniform float blendFactor;\n" +
            "uniform samplerExternalOES baseSampler;\n" +
            "\n" +
            "void main(void) {\n" +
            "    mediump vec4 fragColor;\n" +
            "    fragColor = blendFactor * texture2D(baseSampler, outTexCoordinates);\n" +
            "    gl_FragColor = vec4(fragColor.rgba);\n" +
            "}";

    String VERT_SHADER_ARGS = "";
    String VERT_SHADER_MAIN = "";
    Set<String> FRAG_SHADER_ARGS = new LinkedHashSet<>();
    String FRAG_SHADER_MAIN = "";

    int mTextureSamplerCount;

    public ImageFilter() {
        this(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER);
    }

    public ImageFilter(final String vertexShader, final String fragShader) {
        super(vertexShader, fragShader);
        init();
    }

    @Override
    void copy(BaseFilter baseFilter) {
        super.copy( baseFilter);

        ImageFilter filter = (ImageFilter) baseFilter;
    }

    @Override
    void init() {
        TAG = "ImageFilter";
    }

    @Override
    public void setup(Caches caches) {
        super.setup(caches);

        Log.d(TAG, "Setting up filter: " + this + " for: " + mDescription);

        postSetup();
    }

    public void postSetup() {
    }

    @Override
    public boolean onDraw() {
        if (!isInitialized()) return false;
        exportVariableValues(mProgram);

        super.onDraw();

        return true;
    }

    public void exportVariableValues(Program program) {
    }

    void registerShader() {
        if (!FRAG_SHADER_ARGS.isEmpty()) {
            String ARGS = "";
            for (String s : FRAG_SHADER_ARGS) {
                ARGS += s;
            }
            ProgramCache.registerFragArgs(mFilterMode, ARGS);
        }
        if (!FRAG_SHADER_MAIN.isEmpty()) {
            ProgramCache.registerFragMain(mFilterMode, FRAG_SHADER_MAIN);
        }

        if (!VERT_SHADER_ARGS.isEmpty()) {
            ProgramCache.registerVertArgs(mFilterMode, VERT_SHADER_ARGS);
        }
        if (!VERT_SHADER_MAIN.isEmpty()) {
            ProgramCache.registerVertMain(mFilterMode, VERT_SHADER_MAIN);
        }
    }
}
