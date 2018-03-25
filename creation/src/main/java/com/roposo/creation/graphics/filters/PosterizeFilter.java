package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.FilterManager;
import com.roposo.creation.graphics.gles.Program;

/**
 * Created by Tanvi on 20/12/17.
 */

public class PosterizeFilter extends ImageFilter {
    private volatile float gamma = 0.65f, regions = 5.0f, lines = 0.7f, base = 3.0f, greenBias = 1f;

    private static final String ARGS = "uniform float GAMMA;\n" +
            "uniform float REGIONS;\n" +
            "uniform float LINES;\n" +
            "uniform float BASE;\n" +
            "uniform float GREEN_BIAS;\n" +
            "\n" +
            "vec3 recolor(vec3 color)\n" +
            "{\n" +
            "\tif(color.g > (color.r + color.b)*GREEN_BIAS)\n" +
            "\t{\n" +
            "\t\tcolor.rgb = vec3(0.,0.,0.);\n" +
            "\t}\n" +
            "\n" +
            "\t\n" +
            "\tcolor.rgb = 0.2126*color.rrr + 0.7152*color.ggg + 0.0722*color.bbb;\n" +
            "\t\n" +
            "\tif(color.r > 0.95)\n" +
            "\t{\n" +
            "\t\t\n" +
            "\t}\n" +
            "\telse if(color .r > 0.75)\n" +
            "\t{\n" +
            "\t\tcolor.r *= 0.9;\n" +
            "\t}\n" +
            "\telse if(color.r > 0.5)\n" +
            "\t{\n" +
            "\t\tcolor.r *= 0.7;\n" +
            "\t\tcolor.g *=0.9;\n" +
            "\t}\n" +
            "\telse if (color.r > 0.25)\n" +
            "\t{\n" +
            "\t\tcolor.r *=0.5;\n" +
            "\t\tcolor.g *=0.75;\n" +
            "\t}\n" +
            "\telse\n" +
            "\t{\n" +
            "\t\tcolor.r *= 0.25;\n" +
            "\t\tcolor.g *= 0.5;\n" +
            "\t}\n" +
            "\t\n" +
            "\t\n" +
            "\treturn color;\n" +
            "}\n" +
            "\n" +
            "\n" +
            "vec3 posterize(vec3 color)\n" +
            "{\n" +
            "\tcolor = pow(color, vec3(GAMMA, GAMMA, GAMMA));\n     //increases the saturation" +
            "\tcolor = floor(color * REGIONS)/REGIONS;\n" +
            "\tcolor = pow(color, vec3(GAMMA));\n" +
            "\treturn color.rgb;\n" +
            "}\n" +
            "\n" +
            "vec3 edge(vec2 uv)\n" +
            "{\n" +
            "  \tvec4 lines= vec4(0.30, 0.59, 0.11, 1.0);\n" +
            "\n" +
            "\tlines.rgb = lines.rgb * LINES;\n" +
            "\tlines *= 1.5;\n" +
            "\t\n" +
            " \n" +
            "  \tfloat s11 = dot(texture2D(baseSampler, uv + vec2(-1.0*texelSize.x , -1.0*texelSize.y )), lines);   // LEFT\n" +
            "  \tfloat s12 = dot(texture2D(baseSampler, uv + vec2(0, -1.0*texelSize.y )), lines);             // MIDDLE\n" +
            "  \tfloat s13 = dot(texture2D(baseSampler, uv + vec2(texelSize.x , -1.0*texelSize.y )), lines);    // RIGHT\n" +
            " \n" +
            "\n" +
            "  \tfloat s21 = dot(texture2D(baseSampler, uv + vec2(-1.0*texelSize.x , 0.0)), lines);                // LEFT\n" +
            "  \t// Omit center\n" +
            "  \tfloat s23 = dot(texture2D(baseSampler, uv + vec2(-1.0*texelSize.x , 0.0)), lines);                // RIGHT\n" +
            " \n" +
            "  \tfloat s31 = dot(texture2D(baseSampler, uv + vec2(-1.0*texelSize.x , texelSize.y )), lines);    // LEFT\n" +
            "  \tfloat s32 = dot(texture2D(baseSampler, uv + vec2(0, texelSize.y )), lines);              // MIDDLE\n" +
            "  \tfloat s33 = dot(texture2D(baseSampler, uv + vec2(texelSize.x , texelSize.y )), lines); // RIGHT\n" +
            " \n" +
            "  \tfloat t1 = s13 + s33 + (2.0 * s23) - s11 - (2.0 * s21) - s31;\n" +
            "  \tfloat t2 = s31 + (2.0 * s32) + s33 - s11 - (2.0 * s12) - s13;\n" +
            " \n" +
            "  \tvec3 col;\n" +
            " \n" +
            "\tif (((t1 * t1) + (t2* t2)) > 0.04) \n" +
            "\t{\n" +
            "  \t\tcol = vec3(-1.,-1.,-1.);\n" +
            "  \t}\n" +
            "\telse\n" +
            "\t{\n" +
            "    \tcol = vec3(0.,0.,0.);\n" +
            "  \t}\n" +
            " \n" +
            "  \treturn col;\n" +
            "}";
    private static final String SHADER = "vec2 uv = outTexCoords.xy;\n" +
            "\tvec3 color = normalize(texture2D(baseSampler,uv)).rgb*BASE;\t\n" +
            "\tcolor = posterize(color);\n" +
            "\tcolor.rgb += edge(uv);\n" +
            "\tcolor = recolor(color);\n" +
            "\t//color = texture2D(baseSampler,vec2(uv.x, uv.y)).rgb;\n" +
            "\tfragColor = vec4(color,1.);";

    public PosterizeFilter() {
        super();
        mFilterMode.add(FilterManager.POSTERIZE_FILTER);
        FRAG_SHADER_MAIN += SHADER;
        FRAG_SHADER_ARGS.add(ARGS);
        registerShader();
    }

    @Override
    public void exportVariableValues(Program program) {
        //Log.d(TAG, String.format(Locale.getDefault(), "Gamma(%f), Regions(%f), Lines(%f), Base(%f), Green Bias(%f)", gamma, regions, lines, base, greenBias));
        program.uniform1f("GAMMA", gamma);
        program.uniform1f("REGIONS", regions);
        program.uniform1f("LINES", lines);
        program.uniform1f("BASE", base);
        program.uniform1f("GREEN_BIAS", greenBias);
    }

}
