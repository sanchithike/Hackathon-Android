package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.Caches;
import com.roposo.creation.graphics.gles.ProgramCache;

import static com.roposo.creation.graphics.gles.ProgramCache.SHADER_VAR_FRAGCOLOR;
import static com.roposo.creation.graphics.gles.ProgramCache.SHADER_VAR_TEXELSIZE;

/**
 * @author muddassir on 11/6/17.
 */

public class BlurFilter extends ImageFilter {

    private float texelWidthFactor, texelHeightFactor;

    private static final String gFS_Header_Varyings_1d_Blur_Neighbours =
            " vec2 oneStepLeftTextureCoordinate;\n" +
                    " vec2 oneStepRightTextureCoordinate;\n" +
                    " vec2 twoStepLeftTextureCoordinate;\n" +
                    " vec2 twoStepRightTextureCoordinate;\n" +
                    " vec2 threeStepLeftTextureCoordinate;\n" +
                    " vec2 threeStepRightTextureCoordinate;\n" +
                    " vec2 fourStepLeftTextureCoordinate;\n" +
                    " vec2 fourStepRightTextureCoordinate;\n";

    private static final String gFS_Main_FetchBlurTexels =
            SHADER_VAR_FRAGCOLOR + " = 0.20 * texture2D(baseSampler, outTexCoords);\n"
                    + SHADER_VAR_FRAGCOLOR + " += 0.165 * texture2D(baseSampler, oneStepLeftTextureCoordinate);\n"
                    + SHADER_VAR_FRAGCOLOR + " += 0.165 * texture2D(baseSampler, oneStepRightTextureCoordinate);"
                    + SHADER_VAR_FRAGCOLOR + " += 0.13 * texture2D(baseSampler, twoStepLeftTextureCoordinate);\n"
                    + SHADER_VAR_FRAGCOLOR + " += 0.13 * texture2D(baseSampler, twoStepRightTextureCoordinate);\n"
                    + SHADER_VAR_FRAGCOLOR + " += 0.07 * texture2D(baseSampler, threeStepLeftTextureCoordinate);"
                    + SHADER_VAR_FRAGCOLOR + " += 0.07 * texture2D(baseSampler, threeStepRightTextureCoordinate);"
                    + SHADER_VAR_FRAGCOLOR + " += 0.04 * texture2D(baseSampler, fourStepLeftTextureCoordinate);\n"
                    + SHADER_VAR_FRAGCOLOR + " += 0.04 * texture2D(baseSampler, fourStepRightTextureCoordinate);\n";

    private static final String gVS_Header_Varyings_1dBlur_Neighbours =
            "vec2 texturecoordinates = outTexCoords.xy;\n" +
                    "vec2 texeloffset = " + SHADER_VAR_TEXELSIZE + ";\n" +
                    "vec2 firstOffset = vec2(1.5 * texeloffset.x, 1.5 * texeloffset.y);\n" +
                    "vec2 secondOffset = vec2(2.5 * texeloffset.x, 2.5 * texeloffset.y);\n" +
                    "vec2 thirdOffset = vec2(3.5 * texeloffset.x, 3.5 * texeloffset.y);\n" +
                    "vec2 fourthOffset = vec2(4.5 * texeloffset.x, 4.5 * texeloffset.y);\n" +
                    "oneStepLeftTextureCoordinate = texturecoordinates.xy - firstOffset;\n" +
                    "oneStepRightTextureCoordinate = texturecoordinates.xy + firstOffset;\n" +
                    "twoStepLeftTextureCoordinate = texturecoordinates.xy - secondOffset;\n" +
                    "twoStepRightTextureCoordinate = texturecoordinates.xy + secondOffset;\n" +
                    "threeStepLeftTextureCoordinate = texturecoordinates.xy - thirdOffset;\n" +
                    "threeStepRightTextureCoordinate = texturecoordinates.xy + thirdOffset;\n" +
                    "fourStepLeftTextureCoordinate = texturecoordinates.xy - fourthOffset;\n" +
                    "fourStepRightTextureCoordinate = texturecoordinates.xy + fourthOffset;\n";

    public BlurFilter(boolean horizontal) {
        super();
        if (horizontal) {
            texelWidthFactor = 1;
            texelHeightFactor = 0;
        } else {
            texelWidthFactor = 0;
            texelHeightFactor = 1;
        }
        mFilterMode.add("BlurFilter");
        ProgramCache.registerFragMain(mFilterMode
                , gFS_Header_Varyings_1d_Blur_Neighbours
                         + gVS_Header_Varyings_1dBlur_Neighbours + gFS_Main_FetchBlurTexels);
    }

    @Override
    public void setup(Caches caches) {
        super.setup(caches);
    }
}
