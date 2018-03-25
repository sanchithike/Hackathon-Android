package com.roposo.creation.graphics.gles;

import android.util.Log;
import android.util.LruCache;

import com.roposo.creation.graphics.ImageSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Generates and caches program. Programs are generated based on
 * ProgramDescriptions.
 */
@SuppressWarnings({"WeakerAccess", "unused", "UnusedAssignment"})
public class ProgramCache {

    private static final String TAG = "GLES20-ProgramCache";
    private static final boolean VERBOSE = false || GraphicsUtils.VERBOSE;

    public static final String SHADER_VAR_MVPMATRIX = "uMVPMatrix";
    public static final String SHADER_VAR_TEXTURETRANSFORM = "mainTextureTransform";
    public static final String SHADER_VAR_POSITION = "position";
    public static final String SHADER_VAR_TEXCOORDS = "texCoords";
    public static final String SHADER_VAR_OUTTEXCOORDS = "outTexCoords";
    public static final String SHADER_VAR_ORIGINALTEXCOORDS = "originalTexCoords";
    public static final String SHADER_VAR_TEXELSIZE = "texelSize";
    public static final String SHADER_VAR_COLOR = "color";
    public static final String SHADER_VAR_OUTCOLOR = "outColor";
    public static final String SHADER_VAR_SAMPLER = "baseSampler";
    public static final String SHADER_VAR_BLENDFACTOR = "blendFactor";
    public static final String SHADER_VAR_FRAGCOLOR = "fragColor";
    public static final String SHADER_VAR_FRAGCOLOR2 = "fragColor2";
    public static final String SHADER_VAR_ALPHAFACTOR = "alphaFactor";


    public static final String SHADER_FUN_TEX_SAMPLER = "sampleTexture";
    public static final String SHADER_LUT_TEX_SAMPLER = "sampleLutTexture";

    private static final String SHADER_TYPE_SAMPLER = "sampler2D";
    private static final String SHADER_TYPE_EXTERNALSAMPLER = "samplerExternalOES";

    private static final String gVS_Header_Locals =
            "";

    private static final String gVS_Header_Attributes =
            "attribute vec3 " + SHADER_VAR_POSITION + ";\n";

    private static final String gVS_Header_Varyings_Position =
            "attribute vec2 outPosition;\n";

    private static final String gVS_Header_Attributes_Colors =
            "attribute vec4 " + SHADER_VAR_COLOR + ";\n";

    private static final String gVS_Header_Uniforms_HasGradient =
            "uniform mat4 screenSpace;\n";

    private static final String gVS_Header_Uniforms_TexelSize =
            "uniform vec2 " + SHADER_VAR_TEXELSIZE +";\n";

    private static final String gVS_Header_Varyings_Colors =
            "varying vec4 " + SHADER_VAR_OUTCOLOR + ";\n";

    private static final String gVS_Header_Varyings_Neighours =
            "vec2 texturecoordinates = outTexCoords.xy;\n" +
                    "vec2 texeloffset = " + SHADER_VAR_TEXELSIZE + ";\n" +
                    "leftTextureCoordinate = texturecoordinates + vec2(-texeloffset.x, 0.0);\n" +
                    "rightTextureCoordinate = texturecoordinates + vec2(texeloffset.x, 0.0);\n" +
                    "topTextureCoordinate = texturecoordinates + vec2(0.0, -texeloffset.y);\n" +
                    "topLeftTextureCoordinate = texturecoordinates + vec2(-texeloffset.x, -texeloffset.y);\n" +
                    "topRightTextureCoordinate = texturecoordinates + vec2(-texeloffset.x, texeloffset.y);\n" +
                    "bottomTextureCoordinate = texturecoordinates + vec2(0.0, texeloffset.y);\n" +
                    "bottomLeftTextureCoordinate = texturecoordinates - vec2(texeloffset.x, -texeloffset.y);" + "\n" +
                    "bottomRightTextureCoordinate = texturecoordinates + vec2(texeloffset.x, texeloffset.y);\n";

    // 2 pass box blur
    // Here texelSize is responsible for holding only horizontal or vertical offsets.
    // 1 of them have to be 0 for 2 pass box blur filter to work properly
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

    private static final String gVS_Header_Varyings_2dBlur_5x5_Neighbours =
            "   blurCoord0x0 = (float((-2+2)*(2*2+1)+(-2+2)))/64.0;\n" +
                    "   blurCoord0x1 = (float((-2+2)*(2*2+1)+(-1+2)))/64.0;\n" +
                    "   blurCoord0x2 = (float((-2+2)*(2*2+1)+(0+2)))/64.0;\n" +
                    "   blurCoord0x3 = (float((-2+2)*(2*2+1)+(1+2)))/64.0;\n" +
                    "   blurCoord0x4 = (float((-2+2)*(2*2+1)+(2+2)))/64.0;\n"
                    +
                    "   blurCoord1x0 = (float((-1+2)*(2*2+1)+(-2+2)))/64.0;\n" +
                    "   blurCoord1x1 = (float((-1+2)*(2*2+1)+(-1+2)))/64.0;\n" +
                    "   blurCoord1x2 = (float((-1+2)*(2*2+1)+(0+2)))/64.0;\n" +
                    "   blurCoord1x3 = (float((-1+2)*(2*2+1)+(1+2)))/64.0;\n" +
                    "   blurCoord1x4 = (float((-1+2)*(2*2+1)+(2+2)))/64.0;\n"
                    +
                    "   blurCoord2x0 = (float((0+2)*(2*2+1)+(-2+2)))/64.0;\n" +
                    "   blurCoord2x1 = (float((0+2)*(2*2+1)+(-1+2)))/64.0;\n" +
                    "   blurCoord2x2 = (float((0+2)*(2*2+1)+(0+2)))/64.0;\n" +
                    "   blurCoord2x3 = (float((0+2)*(2*2+1)+(1+2)))/64.0;\n" +
                    "   blurCoord2x4 = (float((0+2)*(2*2+1)+(2+2)))/64.0;\n"
                    +
                    "   blurCoord3x0 = (float((1+2)*(2*2+1)+(-2+2)))/64.0;\n" +
                    "   blurCoord3x1 = (float((1+2)*(2*2+1)+(-1+2)))/64.0;\n" +
                    "   blurCoord3x2 = (float((1+2)*(2*2+1)+(0+2)))/64.0;\n" +
                    "   blurCoord3x3 = (float((1+2)*(2*2+1)+(1+2)))/64.0;\n" +
                    "   blurCoord3x4 = (float((1+2)*(2*2+1)+(2+2)))/64.0;\n"
                    +
                    "   blurCoord4x0 = (float((2+2)*(2*2+1)+(-2+2)))/64.0;\n" +
                    "   blurCoord4x1 = (float((2+2)*(2*2+1)+(-1+2)))/64.0;\n" +
                    "   blurCoord4x2 = (float((2+2)*(2*2+1)+(0+2)))/64.0;\n" +
                    "   blurCoord4x3 = (float((2+2)*(2*2+1)+(1+2)))/64.0;\n" +
                    "   blurCoord4x4 = (float((2+2)*(2*2+1)+(2+2)))/64.0;\n";

    private static final String gFS_Header_Varyings_2dBlur_3x3_Neighours =
            "varying vec2 leftTextureCoordinate;\n" +
                    "varying vec2 rightTextureCoordinate;\n" +
                    "varying vec2 topTextureCoordinate;\n" +
                    "varying vec2 topLeftTextureCoordinate;\n" +
                    "varying vec2 topRightTextureCoordinate;\n" +
                    "varying vec2 bottomTextureCoordinate;\n" +
                    "varying vec2 bottomLeftTextureCoordinate;\n" +
                    "varying vec2 bottomRightTextureCoordinate;\n";

    private static final String gFS_Header_Varyings_2dBlur_5x5_Neighours =
            "varying float blurCoord0x0;\n" +
                    "varying float blurCoord0x1;\n" +
                    "varying float blurCoord0x2;\n" +
                    "varying float blurCoord0x3;\n" +
                    "varying float blurCoord0x4;\n" +

                    "varying float blurCoord1x0;\n" +
                    "varying float blurCoord1x1;\n" +
                    "varying float blurCoord1x2;\n" +
                    "varying float blurCoord1x3;\n" +
                    "varying float blurCoord1x4;\n" +

                    "varying float blurCoord2x0;\n" +
                    "varying float blurCoord2x1;\n" +
                    "varying float blurCoord2x2;\n" +
                    "varying float blurCoord2x3;\n" +
                    "varying float blurCoord2x4;\n" +

                    "varying float blurCoord3x0;\n" +
                    "varying float blurCoord3x1;\n" +
                    "varying float blurCoord3x2;\n" +
                    "varying float blurCoord3x3;\n" +
                    "varying float blurCoord3x4;\n" +

                    "varying float blurCoord4x0;\n" +
                    "varying float blurCoord4x1;\n" +
                    "varying float blurCoord4x2;\n" +
                    "varying float blurCoord4x3;\n" +
                    "varying float blurCoord4x4;\n";

    private static final String gFS_Header_Varyings_1d_Blur_Neighbours =
            "varying vec2 oneStepLeftTextureCoordinate;\n" +
                    "varying vec2 oneStepRightTextureCoordinate;\n" +
                    "varying vec2 twoStepLeftTextureCoordinate;\n" +
                    "varying vec2 twoStepRightTextureCoordinate;\n" +
                    "varying vec2 threeStepLeftTextureCoordinate;\n" +
                    "varying vec2 threeStepRightTextureCoordinate;\n" +
                    "varying vec2 fourStepLeftTextureCoordinate;\n" +
                    "varying vec2 fourStepRightTextureCoordinate;\n";

    public static final String gFS_Header_Func_HSV =
            "vec3 rgb2hsv(vec3 c)\n" +
                    "{\n" +
                    "    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);\n" +
                    "    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));\n" +
                    "    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));\n" +
                    "\n" +
                    "    float d = q.x - min(q.w, q.y);\n" +
                    "    float e = 1.0e-10;\n" +
                    "    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);\n" +
                    "}\n"
                    +
                    "vec3 hsv2rgb(vec3 c)\n" +
                    "{\n" +
                    "    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);\n" +
                    "    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);\n" +
                    "    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);\n" +
                    "}\n";

    public static final String gFS_Header_Func_CIE_LAB =
            "vec4 cieLAB(vec4 pixel) {\n" +
                    "    float r = pixel.r;\n" +
                    "    float g = pixel.g;\n" +
                    "    float b = pixel.b;\n" +
                    "\n" +
                    "    float eps = 0.008856;\n" +
                    "    float k = 24389.0/27.0;\n" +
                    "\n" +
                    "    float Xr = 0.950470;  // reference white D50\n" +
                    "    float Yr = 1.0;\n" +
                    "    float Zr = 1.088830;\n" +
                    "\n" +
                    "    // assuming sRGB (D65)\n" +
                    "    if (r <= 0.04045)\n" +
                    "        r = r/12.92;\n" +
                    "    else\n" +
                    "        r = pow((r+0.055)/1.055, 2.4);\n" +
                    "\n" +
                    "    if (g <= 0.04045)\n" +
                    "        g = g/12.92;\n" +
                    "    else\n" +
                    "        g = pow((g+0.055)/1.055,2.4);\n" +
                    "\n" +
                    "    if (b <= 0.04045)\n" +
                    "        b = b/12.92;\n" +
                    "    else\n" +
                    "        b = pow((b+0.055)/1.055,2.4);\n" +
                    "\n" +
                    "    float X =  0.4124564*r  + 0.3575761*g + 0.1804375*b;\n" +
                    "    float Y =  0.2126729*r  + 0.7151522*g + 0.0721750*b;\n" +
                    "    float Z =  0.0193339*r  + 0.1191920*g + 0.9503041 *b;\n" +
                    "\n" +
                    "    // XYZ to Lab\n" +
                    "    float xr = X/Xr;\n" +
                    "    float yr = Y/Yr;\n" +
                    "    float zr = Z/Zr;\n" +
                    "\n" +
                    "    float fx, fy, fz;\n" +
                    "\n" +
                    "    if ( xr > eps )\n" +
                    "        fx =  pow(xr, 1.0/3.0);\n" +
                    "    else\n" +
                    "        fx = 7.787037*xr + 4.0/29.0;\n" +
                    "\n" +
                    "    if ( yr > eps )\n" +
                    "        fy =  pow(yr, 1.0/3.0);\n" +
                    "    else\n" +
                    "        fy = 7.787037*yr + 4.0/29.0;\n" +
                    "\n" +
                    "    if ( zr > eps )\n" +
                    "        fz =  pow(zr, 1.0/3.0);\n" +
                    "    else\n" +
                    "        fz = 7.787037*zr + 4.0/29.0;\n" +
                    "\n" +
                    "    float ls = (116.0 * fy) - 16.0;\n" +
                    "    float as = 500.0 * (fx-fy);\n" +
                    "    float bs = 200.0 * (fy-fz);\n" +
                    "\n" +
                    "    vec3 labPixel;\n" +
                    "    labPixel.r = ls;\n" +
                    "    labPixel.g = as;\n" +
                    "    labPixel.b = bs;\n" +
                    "\n" +
                    "    return vec4(labPixel,1.0);\n" +
                    "}\n";

    private static final String gVS_Header_Uniforms =
            "uniform mat4 " + SHADER_VAR_MVPMATRIX + ";\n";

    private static final String gVS_Main =
            "void main() {\n";

    private static final String gVS_Main_Core =
            ""
            ;

    private static final String gVS_Main_OutColors =
            SHADER_VAR_OUTCOLOR + " = " + SHADER_VAR_COLOR + ";\n";

    private static final String gVS_Main_Position =
            "    vec4 transformedPosition = " + SHADER_VAR_MVPMATRIX + " * vec4(" +
                    SHADER_VAR_POSITION + ", 1.0);\n" +
            "    gl_Position = transformedPosition;\n";

    private static final String gVS_Footer =
            "}\n";

    private static final String gFS_Header_Extension_ExternalTexture =
            "#extension GL_OES_EGL_image_external : require\n";

    private static final String gFS_Header =
            "precision highp float;\n";

    private static final String gFS_Uniforms_BlurCoeffSampler =
            "uniform " + SHADER_TYPE_SAMPLER + " blurCoeffSampler;\n";

    private static final String gFS_Uniforms_Textures_BlendFactor =
            "uniform float " + SHADER_VAR_BLENDFACTOR + ";\n";

    private static final String gFS_Uniforms_Texture_AlphaFactor =
            "uniform float " + SHADER_VAR_ALPHAFACTOR + ";\n";

    static final String gFS_Uniforms_Orientation =
            "uniform int orientation;\n";

    private static final String gFS_Uniforms_Texture_TexelSize =
            "uniform vec2 " + SHADER_VAR_TEXELSIZE + ";\n";

    private static final String gFS_Main =
            "void main(void) {\n";

    // General case
    private static final String gFS_Main_FetchColor =
            "    " + SHADER_VAR_FRAGCOLOR + " = " + SHADER_VAR_OUTCOLOR + ";\n";

    private static final String gFS_Main_Blend_Textures =
            "    float mixFactor = 0.5;\n" +
            "    " + SHADER_VAR_FRAGCOLOR + " = " + SHADER_VAR_FRAGCOLOR2 + " * vec4(mixFactor, mixFactor, mixFactor, mixFactor) " +
                    "+ " +
                    SHADER_VAR_FRAGCOLOR + " * vec4(1.0-mixFactor, 1.0-mixFactor, 1.0-mixFactor, 1.0-mixFactor)" +
            ";" +
            "\n";

    private static final String gFS_Main_FetchA8Texture =
        "    " + SHADER_VAR_FRAGCOLOR + " = texture2D(" + SHADER_VAR_SAMPLER + ", outTexCoords);\n";

    /*
     * Implementation of Filter List which we support START
     */

    /** Assistant functions for filters START **/
    static final String gFS_Main_VoronoiFcn =
            "vec2 getCoordFromColor(vec4 color)\n" +
                    "{\n" +
                    "    float z = (color.r * 256.0 + color.g * 256.0 + color.b * 256.0) / 3.0;\n" +
                    "    float yoff = float(floor(z / 8.0));\n" + //32
                    "    float xoff = float(mod(z, 8.0));\n" + //8
                    "    float x = color.x*256.0 + xoff*256.0;\n" +
                    "    float y = color.y*256.0 + yoff*256.0;\n" +
                    "    return vec2(x,y) / vec2(8.0 * 256.0, 32.0 * 256.0) / 10.0;\n" +
                    "}\n";

    private static final String gFS_Main_ColorFilter_BW =
            "   float pixelLuminance = " + SHADER_VAR_FRAGCOLOR + ".r * 0.3 + " + SHADER_VAR_FRAGCOLOR + ".g * 0.59 + " + SHADER_VAR_FRAGCOLOR + ".b * " +
                    "0.11;" +
                    "\n" +
                    "   " + SHADER_VAR_FRAGCOLOR + " = vec4(pixelLuminance, pixelLuminance, pixelLuminance, " + SHADER_VAR_FRAGCOLOR + ".a);     \n";

    private static final String gFS_Main_ColorFilter_Lively =
            "       " + SHADER_VAR_FRAGCOLOR + " = vec4(log2(" + SHADER_VAR_FRAGCOLOR + " + vec4(1.0, 1.0, 1.0, 1.0)));\n";

    /**
     * TODO
     * Contrast should generally be set based on the average pixel luminance of the image
     * Assuming it's 0.4, for now.
     */
    private static final String gFS_Main_ColorFilter_Contrast =
            "       float blendFactorContrast = -0.13;\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " = vec4(blendFactorContrast * vec3(0.4, 0.4, 0.4) " +
                    "+ (1.0-blendFactorContrast) * " + SHADER_VAR_FRAGCOLOR + ".rgb," + SHADER_VAR_FRAGCOLOR + ".a);\n" //Higher Contrast
            ;

    private static final String gFS_Main_ColorFilter_Saturation =
            "       float blendFactorSaturation = -0.20;\n" +
                    "       float pixelLuminance = " + SHADER_VAR_FRAGCOLOR + ".r * 0.3 + " + SHADER_VAR_FRAGCOLOR + ".g * 0.59 + " + SHADER_VAR_FRAGCOLOR
                    + ".b" +
            " * " +
                    "0.11;" +
                    "\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " = vec4(blendFactorSaturation * vec3(pixelLuminance, " +
            "pixelLuminance" +
                    ", pixelLuminance) + (1.0-blendFactorSaturation) * " + SHADER_VAR_FRAGCOLOR + ".rgb," + SHADER_VAR_FRAGCOLOR + ".a)" +
                    ";\n"
            //Higher
            // Saturation
            ;

    static final String gFS_Main_ColorFilter_Hot =
            "        vec3 layer = vec3(228.0/255.0, 43.0/255.0, 43.0/255.0);\n" +
                    "        " + SHADER_VAR_FRAGCOLOR + ".rgb = mix(" + SHADER_VAR_FRAGCOLOR + ".rgb, layer, 0.16);\n";

    static final String gFS_Main_ColorFilter_Sepia =
            "    const vec3 sepia = vec3(1.2, 1.0, 0.8);\n" +
                    "    " + SHADER_VAR_FRAGCOLOR + ".rgb = mix(" + SHADER_VAR_FRAGCOLOR + ".rgb, sepia, 0.6);\n";

    static final String gFS_Main_ColorFilter_Pixellate =
            "highp vec2 sampleDivisor = vec2(0.012, 0.012);\n" +
                    "highp vec2 samplePos = outTexCoords.xy - mod(outTexCoords.xy, sampleDivisor) + 0.5 * sampleDivisor;\n" + SHADER_VAR_FRAGCOLOR + " = texture2D(baseSampler, samplePos);\n";

    private static final String gFS_Main_ColorFilter_Bulge =
            "    vec2 focusCoord = vec2(0.0, 0.0);\n" +
                    "    float focusBoundary = 0.5;\n" +
                    "    vec2 texCoord = 2.0 * outTexCoords - 1.0;\n" +
                    "    vec2 diffVector = texCoord - focusCoord;\n" +
                    "    float r = length(diffVector); // to polar coords \n" +
                    "    float phi = atan(diffVector.y, diffVector.x); // to polar coords \n" +
                    "    r = r * smoothstep(-0.05-focusBoundary/5.0, focusBoundary, 0.5 * r);\n" + // Bulge
                    "    texCoord.x = r * cos(phi); \n" +
                    "    texCoord.y = r * sin(phi); \n" +
                    "    texCoord = texCoord / 2.0 + 0.5;\n" +
                    "    " + SHADER_VAR_FRAGCOLOR + " = texture2D(baseSampler, texCoord);\n";

    static final String gFS_Main_ColorFilter_Voronoi =
            "       " + SHADER_VAR_FRAGCOLOR + " = texture2D(baseSampler, getCoordFromColor(" + SHADER_VAR_FRAGCOLOR + "));\n";

    private static final String gFS_Main_Fetch8Texels =
            "float bottomLeftIntensity = (texture2D(baseSampler, bottomLeftTextureCoordinate).r + texture2D(baseSampler, bottomLeftTextureCoordinate).g) / 2.0;\n" +
                    "float topRightIntensity = (texture2D(baseSampler, topRightTextureCoordinate).r + texture2D(baseSampler, topRightTextureCoordinate).g) / 2.0;\n" +
                    "float topLeftIntensity = (texture2D(baseSampler, topLeftTextureCoordinate).r + texture2D(baseSampler, topLeftTextureCoordinate).g) / 2.0;\n" +
                    "float bottomRightIntensity = (texture2D(baseSampler, bottomRightTextureCoordinate).r + texture2D(baseSampler, bottomRightTextureCoordinate).g) / 2.0;\n" +
                    "float leftIntensity = (texture2D(baseSampler, leftTextureCoordinate).r + texture2D(baseSampler, leftTextureCoordinate).g) / 2.0;\n" +
                    "float rightIntensity = (texture2D(baseSampler, rightTextureCoordinate).r + texture2D(baseSampler, rightTextureCoordinate).g) / 2.0;\n" +
                    "float bottomIntensity = (texture2D(baseSampler, bottomTextureCoordinate).r + texture2D(baseSampler, bottomTextureCoordinate).g) / 2.0;\n" +
                    "float topIntensity = (texture2D(baseSampler, topTextureCoordinate).r + texture2D(baseSampler, topTextureCoordinate).g) / 2.0;\n";

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

    private static final String gFS_Main_Fetch_Variable_BlurTexels =
                    "       " + SHADER_VAR_FRAGCOLOR + " = vec4(0.0, 0.0, 0.0, 0.0);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord0x0, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(-2.0, -2.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord0x1, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(-2.0, -1.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord0x2, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(-2.0, 0.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord0x3, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(-2.0, 1.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord0x4, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(-2.0, 2.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord1x0, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(-1.0, -2.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord1x1, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(-1.0, -1.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord1x2, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(-1.0, 0.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord1x3, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(-1.0, 1.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord1x4, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(-1.0, 2.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord2x0, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(0.0, -2.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord2x1, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(0.0, -1.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord2x2, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(0.0, 0.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord2x3, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(0.0, 1.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord2x4, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(0.0, 2.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord3x0, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(1.0, -2.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord3x1, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(1.0, -1.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord3x2, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(1.0, 0.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord3x3, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(1.0, 1.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord3x4, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(1.0, 2.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord4x0, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(2.0, -2.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord4x1, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(2.0, -1.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord4x2, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(2.0, 0.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord4x3, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(2.0, 1.0) * texelSize);\n" +
                    "       " + SHADER_VAR_FRAGCOLOR + " += texture2D(blurCoeffSampler, vec2(blurCoord4x4, 0.5)).g *" + " " + "texture2D(baseSampler, outTexCoords + vec2(2.0, 2.0) * texelSize);\n";

    private static final String gFS_Main_ColorFilter_Sketch =
            "float h = -topLeftIntensity - 2.0 * topIntensity - topRightIntensity + bottomLeftIntensity + 2.0 * bottomIntensity + bottomRightIntensity;\n" +
                    "float v = -bottomLeftIntensity - 2.0 * leftIntensity - topLeftIntensity + bottomRightIntensity + 2.0 * rightIntensity + topRightIntensity;\n" +
                    "float mag = 1.0 - (length(vec2(h, v)) * 3.1);\n" +
                    SHADER_VAR_FRAGCOLOR + " = vec4(vec3(mag, mag, mag), 1.0);\n";

    // * edgeStrength
    private static final String gFS_Main_ColorFilter_Toon =
            "float h = -topLeftIntensity - 2.0 * topIntensity - topRightIntensity + bottomLeftIntensity + 2.0 * bottomIntensity + bottomRightIntensity;\n" +
                    "float v = -bottomLeftIntensity - 2.0 * leftIntensity - topLeftIntensity + bottomRightIntensity + 2.0 * rightIntensity + topRightIntensity;\n" +
                    "float mag = length(vec2(h, v));\n" +
                    "vec3 hsv = rgb2hsv(" + SHADER_VAR_FRAGCOLOR + ".rgb);\n" +
                    "hsv.r = floor((hsv.r * 17.0) + 0.5) / 17.0;\n" +
                    "hsv.g = floor((hsv.g * 7.0) + 0.9) / 7.0;\n" +
                    "hsv.b = floor((hsv.b * 9.0) + 0.5) / 9.0;\n" +
                    "float thresholdTest = 1.0 - step(0.47, mag);\n" +
                    SHADER_VAR_FRAGCOLOR + ".rgb = hsv2rgb(hsv);\n" +
                    SHADER_VAR_FRAGCOLOR + " = vec4(" + SHADER_VAR_FRAGCOLOR + ".rgb * thresholdTest, " + SHADER_VAR_FRAGCOLOR + ".a);\n";

    private static final String gFS_Main_ColorFilter_Night =
            "    vec3 hsv = rgb2hsv(" + SHADER_VAR_FRAGCOLOR + ".rgb);\n" +
                    "   hsv.r = mix(hsv.r, 0.55, 0.9);\n" +
                    "   lowp float d = distance(outTexCoords, vec2(0.5, 0.5));\n" +
                    "   lowp float percent = smoothstep(0.2, 0.475, d);\n" +
                    "   " + SHADER_VAR_FRAGCOLOR + ".rgb = hsv2rgb(hsv);\n" +
                    "   " + SHADER_VAR_FRAGCOLOR + " = vec4(mix(" + SHADER_VAR_FRAGCOLOR + ".rgb, vec3(0.0, 0.0, 0.0), percent), (" + SHADER_VAR_FRAGCOLOR + ".a);\n";

    private static final String gFS_Main_ColorFilter_Vignette =
            "   float blendFactorContrast = 0.3;\n" +
                    "   " + SHADER_VAR_FRAGCOLOR + " = vec4(blendFactorContrast * vec3(0.4, 0.4, 0.4) + " +
                    "(1.0-blendFactorContrast) * " + SHADER_VAR_FRAGCOLOR + ".rgb," + SHADER_VAR_FRAGCOLOR + ".a);\n" + "   lowp float d = distance(outTexCoords, vec2(0.5, 0.5));\n" + "   lowp float percent = smoothstep(0.15, 0.62, d);\n" + "   " + SHADER_VAR_FRAGCOLOR + " = vec4(mix(" + SHADER_VAR_FRAGCOLOR + ".rgb, vec3(0.0, 0.0, 0.0), percent), " + SHADER_VAR_FRAGCOLOR + " + " + ".a);\n";
    /**
     * Implementation of Filter List which we support END
     */

    private static final String gFS_Main_FragColor_Color =
            "    " + SHADER_VAR_FRAGCOLOR + " = vec4(" + SHADER_VAR_FRAGCOLOR + ".rgba);\n";

    private static final String gFS_Main_FragColor_Set =
            "    gl_FragColor = " + SHADER_VAR_FRAGCOLOR + ";\n";

    private static final String gFS_Footer =
            "}\n\n";

    public static String gFS_Edge_Gradient = "\n" +
            "//return black, white and grey\n" +
            "vec3 sobel(float stepx, float stepy, vec2 center){\n" +
            "    // get samples around pixel\n" +
            "    float tleft = length(texture2D(baseSampler,center + vec2(-stepx,stepy)));\n" +
            "    float left = length(texture2D(baseSampler,center + vec2(-stepx,0)));\n" +
            "    float bleft = length(texture2D(baseSampler,center + vec2(-stepx,-stepy)));\n" +
            "    float top = length(texture2D(baseSampler,center + vec2(0,stepy)));\n" +
            "    float bottom = length(texture2D(baseSampler,center + vec2(0,-stepy)));\n" +
            "    float tright = length(texture2D(baseSampler,center + vec2(stepx,stepy)));\n" +
            "    float right = length(texture2D(baseSampler,center + vec2(stepx,0)));\n" +
            "    float bright = length(texture2D(baseSampler,center + vec2(stepx,-stepy)));\n" +
            "\n" +
            "    float x = tleft + 2.0*left + bleft - tright - 2.0*right - bright;\n" +
            "    float y = -tleft - 2.0*top - tright + bleft + 2.0 * bottom + bright;\n" +
            "    float color = sqrt((x*x) + (y*y));\n" +
            "    return vec3(color,color,color);\n" +
            " }\n";

    private LruProgramCache mCache;
    private final static HashMap<String, String> mFragArgs = new HashMap<>();
    private final static HashMap<String, String> mFragMain = new HashMap<>();

    private final static HashMap<String, String> mVertArgs = new HashMap<>();
    private final static HashMap<String, String> mVertMain = new HashMap<>();
    public ProgramCache() {
        mCache = new LruProgramCache(10);
    }

    public Program get(String vertexShader, String fragmentShader) {
        ProgramDescription desc = new ProgramDescription();
        desc.vertexShader = vertexShader;
        desc.fragmentShader = fragmentShader;
        return get(desc);
    }

    public synchronized Program get(final ProgramDescription description) {
        Program program = mCache.get(description.toString());
        if (program == null) {
            Log.w(TAG, "generating program");
            program = generateProgram(description);
            if (program.mProgramHandle < 0) {
                return null;
            }
            mCache.put(description.toString(), program);
        } else {
            Log.d(TAG, "returning program from cache");
        }
        return program;
    }

    void clear() {
        if (mCache == null) {
            return;
        }
        int size = mCache.size();

        mCache.evictAll();
        mCache = null;
    }

    void delete() {
        clear();
        mCache = null;
    }

    private static class LruProgramCache extends LruCache<String, Program> {
        /**
         * @param maxSize for caches that do not override {@link #sizeOf}, this is
         *                the maximum number of entries in the cache. For all other caches,
         *                this is the maximum sum of the sizes of the entries in this cache.
         */
        public LruProgramCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected void entryRemoved(boolean evicted, String handle, Program programObject, Program newProgramObject) {
            if (VERBOSE)
                Log.d(TAG, "Evicting ProgramObject " + programObject + "  handle: " + handle);
            programObject.release();
        }
    }

    private Program generateProgram(ProgramDescription description) {
        String vertexShader = generateVertexShader(description);
        String fragmentShader = generateFragmentShader(description);

        if (VERBOSE) {
            Log.w(TAG, "vertex shader :");
            Log.w(TAG, vertexShader);

            Log.w(TAG, "fragment shader :");
            Log.w(TAG, fragmentShader);
        }
        return generateProgram(vertexShader, fragmentShader);
    }

    private Program generateProgram(String vertexShader, String fragmentShader) {
        if (VERBOSE) {
            Log.w(TAG, "vertex shader :");
            Log.w(TAG, vertexShader);

            Log.w(TAG, "fragment shader :");
            Log.w(TAG, fragmentShader);
        }
        return new Program(vertexShader, fragmentShader);
    }

    private String generateVertexShader(ProgramDescription description) {
        StringBuilder shader = new StringBuilder();
        shader.append(gFS_Header);
        shader.append(gVS_Header_Attributes);
        shader.append(gVS_Header_Locals);
        shader.append(gVS_Header_Varyings_Position);

        for (int i = 0; i < description.imageSources.size(); i++) {
            ImageSource imageSource = description.imageSources.get(i);
            if (imageSource.hasTexture()) {
                shader.append("attribute vec2 ")
                        .append(getIndexedVariable(SHADER_VAR_TEXCOORDS, i)).append(";\n");
            }
        }

        // Uniforms
        shader.append(gVS_Header_Uniforms);
        shader.append(gVS_Header_Uniforms_TexelSize);

        for (int i = 0; i < description.imageSources.size(); i++) {
            ImageSource imageSource = description.imageSources.get(i);
            // ideally, the check should have been hasTextureTransform instead of hasExternalTexture
            if (imageSource.hasExternalTexture()) {
                shader.append("uniform mat4 ")
                        .append(getIndexedVariable(SHADER_VAR_TEXTURETRANSFORM, i)).append(";\n");
            }

            if (imageSource.hasTexture()) {
                shader.append("varying vec2 ")
                        .append(getIndexedVariable(SHADER_VAR_OUTTEXCOORDS, i)).append(";\n");
            }
            if (imageSource.hasTexture()) {
                shader.append("varying vec2 ")
                        .append(getIndexedVariable(SHADER_VAR_ORIGINALTEXCOORDS, i)).append(";\n");
            }
        }

        List<String> filterMode = description.colorFilterMode;
        if (mVertArgs.containsKey(String.valueOf(filterMode))) {
            shader.append(mVertArgs.get(String.valueOf(filterMode)));
        }

        // Begin the shader
        shader.append(gVS_Main);
        shader.append(gVS_Main_Core);
        if (mVertMain.containsKey(String.valueOf(filterMode))) {
            shader.append(mVertMain.get(String.valueOf(filterMode)));
        }

        for (int i = 0; i < description.imageSources.size(); i++) {
            ImageSource imageSource = description.imageSources.get(i);
            // ideally, the check should have been hasTextureTransform instead of hasExternalTexture
            if (imageSource.hasExternalTexture()) {
                shader.append("    ").append(getIndexedVariable(SHADER_VAR_OUTTEXCOORDS, i))
                        .append(" = (").append(getIndexedVariable(SHADER_VAR_TEXTURETRANSFORM, i))
                        .append(" * vec4(").append(getIndexedVariable(SHADER_VAR_TEXCOORDS, i))
                        .append(", 0.0, 1.0)).xy;\n");

                shader.append("    ").append(getIndexedVariable(SHADER_VAR_ORIGINALTEXCOORDS, i))
                        .append(" = ").append(getIndexedVariable(SHADER_VAR_TEXCOORDS, i))
                        .append(";\n");
            }
            else if (imageSource.hasTexture()) {
                shader.append("    ").append(getIndexedVariable(SHADER_VAR_OUTTEXCOORDS, i))
                        .append(" = ").append(getIndexedVariable(SHADER_VAR_TEXCOORDS, i))
                        .append(";\n");

                shader.append("    ").append(getIndexedVariable(SHADER_VAR_ORIGINALTEXCOORDS, i))
                        .append(" = ").append(getIndexedVariable(SHADER_VAR_TEXCOORDS, i))
                        .append(";\n");
            }
        }

        shader.append(gVS_Main_Position);

        // End the shader
        shader.append(gVS_Footer);
        return shader.toString();
    }

    private String generateFragmentShader(ProgramDescription description) {
        StringBuilder shader = new StringBuilder();

        if (description.hasExternalTexture()) {
            shader.append(gFS_Header_Extension_ExternalTexture);
        }

        shader.append(gFS_Header);

        for (int i = 0; i < description.imageSources.size(); i++) {
            ImageSource imageSource = description.imageSources.get(i);

            if (imageSource.hasTexture()) {
                // Varyings
                shader.append("varying vec2 ")
                        .append(getIndexedVariable(SHADER_VAR_OUTTEXCOORDS, i)).append(";\n");

                shader.append("varying vec2 ")
                        .append(getIndexedVariable(SHADER_VAR_ORIGINALTEXCOORDS, i)).append(";\n");

                // Texture samplers
                shader.append("uniform ").append(imageSource.hasExternalTexture() ? SHADER_TYPE_EXTERNALSAMPLER : SHADER_TYPE_SAMPLER)
                        .append(" ").append(getIndexedVariable(SHADER_VAR_SAMPLER, i)).append(";\n");
            }
        }

        shader.append(gVS_Header_Uniforms_TexelSize);
        shader.append(gFS_Uniforms_Texture_AlphaFactor);

        List<String> filterMode = description.colorFilterMode;
        if (mFragArgs.containsKey(String.valueOf(filterMode))) {
            shader.append(mFragArgs.get(String.valueOf(filterMode)));
        }

        for (int i = 0; i < description.imageSources.size(); i++) {
            ImageSource imageSource = description.imageSources.get(i);
            if (imageSource.hasTexture()) {
                shader.append("vec4 ").append(getIndexedVariable(SHADER_FUN_TEX_SAMPLER, i))
                        .append("(vec2 coord) {\n")
                        .append("   vec4 sampledColor = texture2D(").append(getIndexedVariable(SHADER_VAR_SAMPLER, i)).append(", coord);\n")
                        .append("   sampledColor = vec4(sampledColor.").append(imageSource.isBGRA() ? "bgra" : "rgba").append(");\n")
                        .append("   return sampledColor; \n")
                        .append("}\n");

                // lut
            }
        }

        int lutTextureIndex = -1;
        for (int i = 0; i < description.imageSources.size(); i++) {
            ImageSource imageSource = description.imageSources.get(i);
            if (imageSource.hasTexture() && imageSource.isLUT()) {
                lutTextureIndex += 1;
                shader.append("vec4 ").append(getIndexedVariable(SHADER_LUT_TEX_SAMPLER, lutTextureIndex))
                      .append("(vec2 coord) {\n")
                      .append(" return ").append(getIndexedVariable(SHADER_FUN_TEX_SAMPLER, i)).append("(coord)").append(";\n")
                      .append("}\n")
                      ;
            }
        }

        // Begin the shader
        shader.append(gFS_Main);

        for (int i = 0; i < description.imageSources.size(); i++) {
            ImageSource imageSource = description.imageSources.get(i);
            shader.append("    vec4 ").append(getIndexedVariable(SHADER_VAR_FRAGCOLOR, i)).append(";\n");
        }

        // Stores the result in fragColor directly
        for (int i = 0; i < description.imageSources.size(); i++) {
            ImageSource imageSource = description.imageSources.get(i);
            shader.append("    ").append(getIndexedVariable(SHADER_VAR_FRAGCOLOR, i)).append(" = ")
                    .append(getIndexedVariable("sampleTexture", i))
                    .append("(").append(getIndexedVariable(SHADER_VAR_OUTTEXCOORDS, i)).append(");\n");
        }

        if (description.hasColorFilter) {
//            for (int i = 0; i < filterMode.size(); i++) {
                if (mFragMain.containsKey(String.valueOf(filterMode))) {
                    shader.append(mFragMain.get(String.valueOf(filterMode)));
                }
//            }
        }

        // set the alpha just before ending
        shader.append("fragColor.a *= ").append(SHADER_VAR_ALPHAFACTOR).append(";\n");

        // End the shader
        shader.append(gFS_Main_FragColor_Set);
        shader.append(gFS_Footer);
        return shader.toString();
    }

    public synchronized void release(Program program) {
        if (program == null) return;
        Map<String, Program> map = mCache.snapshot();
        Iterator<Map.Entry<String, Program>> itr = map.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, Program> entry = itr.next();
            if (program.equals(entry.getValue())) {
                mCache.remove(entry.getKey());
                program.release();
                break;
            }
        }
    }

    public static void registerFragMain(List<String> filters, String fragShaderString) {
        mFragMain.put(filters.toString(), fragShaderString);
    }

    public static void registerFragArgs(List<String> filters, String args) {
        mFragArgs.put(filters.toString(), args);
    }

    public static void registerVertMain(List<String> filters, String fragShaderString) {
        mVertMain.put(filters.toString(), fragShaderString);
    }

    public static void registerVertArgs(List<String> filters, String args) {
        mVertArgs.put(filters.toString(), args);
    }

    /**
     * Describe the features required for a given program. The features
     * determine the generation of both the vertex and fragment shaders.
     * A ProgramDescription must be used in conjunction with a ProgramCache.
     */
    @SuppressWarnings("WeakerAccess")
    public static class ProgramDescription {
        ProgramDescription() {
            reset();
        }

        public OpenGLRenderer.Fuzzy renderTargetType;

        // Texturing
        public List<ImageSource> imageSources = new ArrayList<>();

        public boolean hasColorFilter;
        List<String> colorFilterMode = new ArrayList<>();

        String vertexShader;
        String fragmentShader;
        /**
         * Resets this description. All fields are reset back to the default
         * values they hold after building a new instance.
         */
        void reset() {
            renderTargetType = null;
            imageSources.clear();

            hasColorFilter = false;
            colorFilterMode.clear();

            vertexShader = null;
            fragmentShader = null;
        }

        public void setFilterMode(List<String> filterMode) {
            if (filterMode.size() > 0) {
                hasColorFilter = true;
            } else {
                hasColorFilter = false;
            }
            colorFilterMode.clear();
            colorFilterMode.addAll(filterMode);
        }

        public boolean hasExternalTexture() {
            if (imageSources != null) {
                for (ImageSource imageSource : imageSources) {
                    if (imageSource.hasExternalTexture()) {
                        return true;
                    }
                }
            }

            return false;
        }

        public static ProgramDescription clone(ProgramDescription programDescription) {
            ProgramDescription newProgramDescription = new ProgramDescription();

            newProgramDescription.renderTargetType = programDescription.renderTargetType;

            newProgramDescription.imageSources.addAll(programDescription.imageSources);

            newProgramDescription.hasColorFilter = programDescription.hasColorFilter;

            newProgramDescription.colorFilterMode.addAll(programDescription.colorFilterMode);

            newProgramDescription.vertexShader = programDescription.vertexShader;
            newProgramDescription.fragmentShader = programDescription.fragmentShader;

            return newProgramDescription;
        }

        @Override
        public String toString() {
            return " vertShader: " + vertexShader +
                    " fragShader: " + fragmentShader +
                    " imageSource: " + imageSources +
                    " hasColorFilter:" + hasColorFilter +
                    " colorFilterMode: " + colorFilterMode +
                    " renderTargetType: " + renderTargetType;
        }

//        @Override
//        public int hashCode() {
//            return toString().hashCode();
//        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof ProgramDescription) {
                return this.toString().equals(obj.toString());
            }

            return false;
        }
    } // struct ProgramDescription

    static String getIndexedVariable(String variableName, int index) {
        return variableName + ((index == 0) ? "" : (index + 1));
    }
} // class ProgramCache
