package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

import static com.roposo.creation.graphics.gles.ProgramCache.SHADER_VAR_FRAGCOLOR;
/**
 * @author muddassir on 11/6/17.
 */

public class Blur2DFilter extends ImageFilter {

    private static final String gVS_Header_Varyings_2dBlur_5x5_Neighbours =
                      "float blurCoord0x0 = (float((-2.0 + 2.0) * float(2*2+1) + (-2.0 + 2.5)))/64.0;\n"
                    + "float blurCoord0x1 = (float((-2.0 + 2.0) * float(2*2+1) + (-1.0 + 2.5)))/64.0;\n"
                    + "float blurCoord0x2 = (float((-2.0 + 2.0) * float(2*2+1) + (0.0 + 2.5)))/64.0;\n"
                    + "float blurCoord0x3 = (float((-2.0 + 2.0) * float(2*2+1) + (1.0 + 2.5)))/64.0;\n"
                    + "float blurCoord0x4 = (float((-2.0 + 2.0) * float(2*2+1) + (2.0 + 2.5)))/64.0;\n"
                    + "float blurCoord1x0 = (float((-1.0 + 2.0) * float(2*2+1) + (-2.0 + 2.5)))/64.0;\n"
                    + "float blurCoord1x1 = (float((-1.0 + 2.0) * float(2*2+1) + (-1.0 + 2.5)))/64.0;\n"
                    + "float blurCoord1x2 = (float((-1.0 + 2.0) * float(2*2+1) + (0.0 + 2.5)))/64.0;\n"
                    + "float blurCoord1x3 = (float((-1.0 + 2.0) * float(2*2+1) + (1.0 + 2.5)))/64.0;\n"
                    + "float blurCoord1x4 = (float((-1.0 + 2.0) * float(2*2+1) + (2.0 + 2.5)))/64.0;\n"
                    + "float blurCoord2x0 = (float((0.0 + 2.0) * float(2*2+1) + (-2.0 + 2.5)))/64.0;\n"
                    + "float blurCoord2x1 = (float((0.0 + 2.0) * float(2*2+1) + (-1.0 + 2.5)))/64.0;\n"
                    + "float blurCoord2x2 = (float((0.0 + 2.0) * float(2*2+1) + (0.0 + 2.5)))/64.0;\n"
                    + "float blurCoord2x3 = (float((0.0 + 2.0) * float(2*2+1) + (1.0 + 2.5)))/64.0;\n"
                    + "float blurCoord2x4 = (float((0.0 + 2.0) * float(2*2+1) + (2.0 + 2.5)))/64.0;\n"
                    + "float blurCoord3x0 = (float((1.0 + 2.0) * float(2*2+1) + (-2.0 + 2.5)))/64.0;\n"
                    + "float blurCoord3x1 = (float((1.0 + 2.0) * float(2*2+1) + (-1.0 + 2.5)))/64.0;\n"
                    + "float blurCoord3x2 = (float((1.0 + 2.0) * float(2*2+1) + (0.0 + 2.5)))/64.0;\n"
                    + "float blurCoord3x3 = (float((1.0 + 2.0) * float(2*2+1) + (1.0 + 2.5)))/64.0;\n"
                    + "float blurCoord3x4 = (float((1.0 + 2.0) * float(2*2+1) + (2.0 + 2.5)))/64.0;\n"
                    + "float blurCoord4x0 = (float((2.0 + 2.0) * float(2*2+1) + (-2.0 + 2.5)))/64.0;\n"
                    + "float blurCoord4x1 = (float((2.0 + 2.0) * float(2*2+1) + (-1.0 + 2.5)))/64.0;\n"
                    + "float blurCoord4x2 = (float((2.0 + 2.0) * float(2*2+1) + (0.0 + 2.5)))/64.0;\n"
                    + "float blurCoord4x3 = (float((2.0 + 2.0) * float(2*2+1) + (1.0 + 2.5)))/64.0;\n"
                    + "float blurCoord4x4 = (float((2.0 + 2.0) * float(2*2+1) + (2.0 + 2.5)))/64.0;\n";

    private static final String gFS_Main_Fetch_Variable_BlurTexels =
//                   "if (originalTexCoords.x < 0.5) {\n" +
                             "    fragColor2 = vec4(0.0, 0.0, 0.0, 0.0);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord0x0, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(-2.0, -2.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord0x1, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(-2.0, -1.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord0x2, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(-2.0, 0.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord0x3, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(-2.0, 1.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord0x4, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(-2.0, 2.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord1x0, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(-1.0, -2.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord1x1, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(-1.0, -1.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord1x2, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(-1.0, 0.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord1x3, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(-1.0, 1.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord1x4, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(-1.0, 2.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord2x0, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(0.0, -2.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord2x1, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(0.0, -1.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord2x2, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(0.0, 0.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord2x3, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(0.0, 1.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord2x4, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(0.0, 2.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord3x0, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(1.0, -2.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord3x1, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(1.0, -1.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord3x2, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(1.0, 0.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord3x3, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(1.0, 1.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord3x4, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(1.0, 2.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord4x0, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(2.0, -2.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord4x1, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(2.0, -1.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord4x2, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(2.0, 0.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord4x3, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(2.0, 1.0) * texelSize);\n"
                           + "    fragColor2 += sampleTexture2(vec2(blurCoord4x4, 0.5)).r *" + " " + "sampleTexture(outTexCoords + vec2(2.0, 2.0) * texelSize);\n"
                    + "       " + SHADER_VAR_FRAGCOLOR + " = (fragColor2)*(256.0/248.0);\n"
                    + "       " + SHADER_VAR_FRAGCOLOR + ".a = 1.0;\n"
//                    + "}\n"
            ;

    private static final String gFS_Uniforms_Texture_TexelSize = "uniform sampler2D blurCoeffSampler;\n";

    public Blur2DFilter() {
        super();
        mFilterMode.add(FilterManager.BLUR_2D_FILTER);
        FRAG_SHADER_MAIN += gFS_Main_Fetch_Variable_BlurTexels;
        FRAG_SHADER_ARGS.add(gVS_Header_Varyings_2dBlur_5x5_Neighbours);
        registerShader();
    }

}
