package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;

/**
 * Created by admin on 14/11/17.
 */

public class TVFilter extends ImageFilter {

    private static final String FRAG_SHADER_ARGS_STRING = " uniform highp vec2 mCenter;\n" +
            " uniform highp float mRadius;\n" +
            " uniform highp float aspectRatio;\n" +
            " uniform highp float refractiveIndex;";

    //BARREL DISTORTION
    /*private static final String FRAG_SHADER_ARGS_STRING = "precision mediump float;\n" +
            "uniform float alphax;\n" +
            "uniform float alphay;\n";*/

    //BARREL DISTORTION
    /*private static final String FRAG_SHADER_STRING = "  float x = (2.0 * outTexCoords.x - 1.0) / 1.0;\n" +
            "  float y = (2.0 * outTexCoords.y - 1.0) / 1.0;\n" +
            "  // Calculate l2 norm\n" +
            "  float r = x*x + y*y;\n" +
            "  // Calculate the deflated or inflated new coordinate (reverse transform)\n" +
            "  float x3 = x / (1.0 - alphax * r);\n" +
            "  float y3 = y / (1.0 - alphay * r); \n" +
            "  float x2 = x / (1.0 - alphax * (x3 * x3 + y3 * y3));\n" +
            "  float y2 = y / (1.0 - alphay * (x3 * x3 + y3 * y3));\t\n" +
            "  // Forward transform\n" +
            "  // float x2 = x * (1.0 - alphax * r);\n" +
            "  // float y2 = y * (1.0 - alphay * r);\n" +
            "  // De-normalize to the original range\n" +
            "  float i2 = (x2 + 1.0) * 1.0 / 2.0;\n" +
            "  float j2 = (y2 + 1.0) * 1.0 / 2.0;\n" +
            "  if(i2 >= 0.0 && i2 <= 1.0 && j2 >= 0.0 && j2 <= 1.0)\n" +
            "    fragColor = texture2D(baseSampler, vec2(i2, j2));\n" +
            "  else\n" +
            "    fragColor = vec4(0.0, 1.0, 0.0, 1.0);\n";*/

    //SPHERE
    private static final String FRAG_SHADER_STRING = "vec2 xy = outTexCoords - vec2(0.5);\n" +
            "float distance = sqrt(xy.x*xy.x + xy.y*xy.y);\n" +
            "float mRadius = 1.0;\n" +
            "vec2 uv = vec2(0.0);\n" +
            "if (distance < mRadius) {\n" +
            "  float theta = atan(xy.y, xy.x);\n" +
            "  mRadius = sqrt(xy.x*xy.x + xy.y*xy.y);\n" +
            "  mRadius = pow(mRadius, 2.0);\n" +
            "  xy.x = mRadius*cos(theta);\n" +
            "  xy.y = mRadius*sin(theta);\n" +
            "  uv = xy * 3.0 + vec2(0.5);\n" +
            "}\n" +
            "else {\n" +
            "  uv = xy;\n" +
            "}\n" +
            "\n" +
            "if (distance > mRadius-0.01 && distance < mRadius+0.01) {\n" +
            "  fragColor = vec4(1.0, 1.0, 1.0, 1.0);\n" +
            "}\n" +
            "else {\n" +
            "  fragColor = texture2D(baseSampler, uv);\n" +
            "}";

//    private static final String FRAG_SHADER_STRING = "highp vec2 textureCoordinateToUse = vec2(outTexCoords.x, (outTexCoords.y * aspectRatio + 0.5 - 0.5 * aspectRatio));\n" +
//            "     highp float distanceFromCenter = distance(mCenter, textureCoordinateToUse);\n" +
//            "     highp float checkForPresenceWithinSphere = step(distanceFromCenter, mRadius);\n" +
//            "     distanceFromCenter = distanceFromCenter / mRadius;\n" +
//            "     highp float normalizedDepth = mRadius * sqrt(1.0 - distanceFromCenter * distanceFromCenter);\n" +
//            "     highp vec3 sphereNormal = normalize(vec3(outTexCoords - mCenter, normalizedDepth));\n" +
//            "     highp vec3 refractedVector = refract(vec3(0.0, 0.0, -1.0), sphereNormal, refractiveIndex);\n" +
//            "     fragColor = texture2D(baseSampler, (refractedVector.xy + 1.0) * 0.5) * checkForPresenceWithinSphere; ";



    private float mAlphaX = 0.5f, mAlphaY = 0.5f;
    private float[] mCenter = new float[]{0.5f, 0.5f};
    private float mRadius = 0.25f;

    public TVFilter() {
        super();
        mFilterMode.add(FilterManager.RGB_ANIMATED_FILTER);
//        FRAG_SHADER_MAIN += FRAG_SHADER_STRING;
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARGS_STRING);
        registerShader();
    }

    /*
    @Override
    public boolean onDraw() {
        if (!isInitialized()) return false;
//        uniform1f("alphax", mAlphaX);
//        uniform1f("alphay", mAlphaY);
        uniform2fv("mCenter", mCenter);
        uniform1f("mRadius", mRadius);
        uniform1f("aspectRatio", 0.6f);
        uniform1f("refractiveIndex", 1.1f);
        return super.onDraw();
    }
    */
}
