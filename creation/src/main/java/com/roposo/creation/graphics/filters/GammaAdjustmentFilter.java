package com.roposo.creation.graphics.filters;

import com.roposo.creation.graphics.gles.Drawable;
import com.roposo.creation.graphics.gles.Program;
import com.roposo.creation.graphics.gles.ProgramCache;

/**
 * @author by Muddassir on 16/10/17.
 */

public class GammaAdjustmentFilter extends ImageFilter {
    private float mPowerFactor = 0.75f;

    private static final String FRAG_SHADER_ARG = "uniform float powerFactor;";
    private static final String FRAG_SHADER_COLOR = "" +
            "    " + ProgramCache.SHADER_VAR_FRAGCOLOR + " = texture2D(baseSampler, " + ProgramCache.SHADER_VAR_OUTTEXCOORDS + ");\n" +
            "    float redPow = pow(" + ProgramCache.SHADER_VAR_FRAGCOLOR + ".r, powerFactor);\n" +
            "    float greenPow = pow(" + ProgramCache.SHADER_VAR_FRAGCOLOR + ".g, powerFactor);\n" +
            "    float bluePow = pow(" + ProgramCache.SHADER_VAR_FRAGCOLOR + ".b, powerFactor);\n" +
            "    " + ProgramCache.SHADER_VAR_FRAGCOLOR + " = blendFactor * vec4(redPow, greenPow, bluePow, "
            + ProgramCache.SHADER_VAR_FRAGCOLOR + ".a);\n" +
            "    gl_FragColor = vec4(" + ProgramCache.SHADER_VAR_FRAGCOLOR + ".rgba);";

    public GammaAdjustmentFilter() {
        super();
        FRAG_SHADER_ARGS.add(FRAG_SHADER_ARG);
        FRAG_SHADER_MAIN += FRAG_SHADER_COLOR;
        mFilterMode.add("GammaAdjustmentFilter");
        registerShader();
        mPowerFactor = 0.75f;
    }

    @Override
    public void exportVariableValues(Program program) {
        super.exportVariableValues(program);
        program.uniform1f("powerFactor", mPowerFactor);
    }

    public void setPowerFactor(float mPowerFactor) {
        this.mPowerFactor = mPowerFactor;
    }
}
