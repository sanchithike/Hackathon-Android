package com.roposo.creation.graphics.filters;

import android.graphics.Rect;
import android.graphics.RectF;

import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.Program;

/**
 * Created by sanchitsharma on 26/03/18.
 */

public class FaceFilter extends ImageFilter {

    private RectF mRect = new RectF();


    public FaceFilter() {
        super();
        mFilterMode.add(FilterManager.FACE_FILTER);
        FRAG_SHADER_ARGS.add(
        "uniform float minX;\n" +
        "uniform float minY;\n" +
        "uniform float maxX;\n" +
        "uniform float maxY;\n");
        FRAG_SHADER_MAIN +=
//                "float alpha = 1.0 - smoothstep(1, 1.2, sqrt(outTexCoords.x * outTexCoords.x + outTexCoords.y * outTexCoords.y));\n" +
                "float s1 = smoothstep(minY + (maxY - minY)* 0.2, minY + (maxY - minY)* 0.6, outTexCoords.y);\n" +
                "float s2 = 1.0 - smoothstep(maxY - (maxY - minY)* 0.6, maxY - (maxY - minY)* 0.2, outTexCoords.y);\n" +
                "float alpha = 1.0;\n" +
                "if (outTexCoords.y < (maxY + minY)/2.0) {\n" +
                "\tfragColor = sampleTexture(outTexCoords) * vec4(1.0, 1.0, 1.0, s1);\n" +
                "} else {\n" +
                "\tfragColor = sampleTexture(outTexCoords) * vec4(1.0, 1.0, 1.0, s2);\n" +
                "}\n"
                ;
        registerShader();
    }

    public void setRect(RectF rect) {
      mRect.set(rect);
    }

    @Override
    public void exportVariableValues(Program program) {
        super.exportVariableValues(program);
        program.uniform1f("minX", mRect.left);
        program.uniform1f("maxX", mRect.right);
        program.uniform1f("minY", mRect.top);
        program.uniform1f("minY", mRect.bottom);
    }
}
