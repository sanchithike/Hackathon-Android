package com.roposo.core.util.graphics;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by bajaj on 23/12/16.
 */

public class GraphicsUtils {

    /**
     * @param src
     * @param target used to determine the aspect ratio of the final rect
     * @param result
     * @return
     */
    public static Rect transformMatrixToRect(Matrix src, Rect target, Rect result) {
        if (result == null) {
            result = new Rect();
        }

        float[] v = new float[9];
        src.getValues(v);

        float scalex = v[Matrix.MSCALE_X];
        float skewy = v[Matrix.MSKEW_Y];

        float rScale = (float) Math.sqrt(scalex * scalex + skewy * skewy);

        int posX = (int) v[Matrix.MTRANS_X];
        int posY = (int) v[Matrix.MTRANS_Y];

        int width = (int) (rScale * target.width());
        int height = (int) (((float) target.height() / target.width()) * width);

        result.set(posX, posY, width + posX, height + posY);

        return result;
    }

    /**
     * Creates new Rect and returns that, if result passed is null.
     *
     * @param src
     * @param refCoordinateSystem
     * @param result
     * @return
     */
    public static RectF transformRectAbsoluteToRelative(Rect src, Rect refCoordinateSystem, RectF result) {
        if (result == null) {
            result = new RectF();
        }
        float posX, posY; // as a fraction of refCoordinateSystem's width/height
        float scaleX, scaleY; // as a fraction of refCoordinateSystem's width/height

        posX = (float) src.left / refCoordinateSystem.width() - 0.5f;
        posY = (float) src.top / refCoordinateSystem.height() - 0.5f;

        scaleX = (float) src.width() / refCoordinateSystem.width();
        scaleY = (float) src.height() / refCoordinateSystem.height();

        result.set(posX, posY, scaleX + posX, scaleY + posY);
        return result;
    }

    /**
     * Transforms a rect's scale from src to target coordinate system with pivot as the center (in place)
     *
     * @param src
     * @param srcCoordinateSystem
     * @param targetCoordinateSystem
     * @return
     */
    public static void transformMatrixScaletoCoordinateSystem(Rect src, Rect srcCoordinateSystem, Rect targetCoordinateSystem) {
        RectF srcF = new RectF(src);
        transformMatrixScaletoCoordinateSystem(srcF, srcCoordinateSystem, targetCoordinateSystem);
        srcF.round(src);
    }

    /**
     * Transforms a PointF from src to target coordinate system with pivot as the center (in place)
     *
     * @param src
     * @param srcCoordinateSystem
     * @param targetCoordinateSystem
     * @return
     */
    public static void transformPointtoCoordinateSystem(PoseF2D src, Rect srcCoordinateSystem, Rect targetCoordinateSystem) {

        PointF posF = new PointF();
        PointF pos = src.pos;

        posF.x = pos.x * (float) srcCoordinateSystem.width() / targetCoordinateSystem.width();
        posF.y = pos.y * (float) srcCoordinateSystem.height() / targetCoordinateSystem.height();

        PointF sizeF = new PointF();
        PointF size = src.size;

        sizeF.x = size.x * (float) srcCoordinateSystem.width() / targetCoordinateSystem.width();
        sizeF.y = size.y * (float) srcCoordinateSystem.height() / targetCoordinateSystem.height();

        pos.set(posF);
        size.set(sizeF);
    }

    /**
     * Transforms a rect's scale from src to target coordinate system with pivot as the center (in place)
     *
     * @param src
     * @param srcCoordinateSystem
     * @param targetCoordinateSystem
     * @return
     */
    public static void transformMatrixScaletoCoordinateSystem(RectF src, Rect srcCoordinateSystem, Rect targetCoordinateSystem) {
        float posX = src.left;
        float posY = src.top;

        src.offset(-posX, -posY);

        int srcWidth = srcCoordinateSystem.width();
        int srcHeight = srcCoordinateSystem.height();

        int targetWidth = targetCoordinateSystem.width();
        int targetHeight = targetCoordinateSystem.height();

        float xScale = (float) targetWidth / srcWidth;
        float yScale = (float) targetHeight / srcHeight;

        // All this because we have the top left of coordinate system as origin.
        // So, transform the origin to center of coordinate system.
        posX -= srcWidth / 2;
        posY -= srcHeight / 2;

        posX *= xScale;
        posY *= yScale;

        // All this because we need the coordinate with origin as top left instead of center.
        // Offset back to top left.
        posX += targetCoordinateSystem.width() / 2;
        posY += targetCoordinateSystem.height() / 2;

        src.offset(posX, posY);
    }

    public static Matrix transformMatrixScaletoCoordinateSystem(Matrix src, Rect srcCoordinateSystem, Rect targetCoordinateSystem) {
        float[] v = new float[9];
        src.getValues(v);

        Matrix target = new Matrix(src);
        float posX = v[Matrix.MTRANS_X];
        float posY = v[Matrix.MTRANS_Y];

        int srcWidth = srcCoordinateSystem.width();
        int srcHeight = srcCoordinateSystem.height();

        int targetWidth = targetCoordinateSystem.width();
        int targetHeight = targetCoordinateSystem.height();

        // All this because we're getting the coordinate as top left instead of center.
        // Offset to left.
        posX -= srcWidth / 2;
        posY -= srcHeight / 2;

        float xScale = (float) targetWidth / srcWidth;
        float yScale = (float) targetHeight / srcHeight;

        posX *= xScale;
        posY *= xScale;

        // Because we need to pass the coordinate with origin as top left instead of center.
        // So, offset back to top left.
        posX += targetWidth / 2;
        posY += targetHeight / 2;

        float[] w = new float[9];
        target.getValues(w);

        w[Matrix.MTRANS_X] = posX;
        w[Matrix.MTRANS_Y] = posY;

        w[Matrix.MSCALE_X] *= xScale;
        w[Matrix.MSCALE_Y] *= xScale;

        w[Matrix.MSKEW_X] *= xScale;
        w[Matrix.MSKEW_Y] *= xScale;

        target.setValues(w);

        return target;
    }

    /**
     * Transforms a rect's translation from src to target coordinate system.
     *
     * @param src
     * @param srcCoordinateSystem
     * @param targetCoordinateSystem
     * @return
     */
    public static void transformMatrixTranslationtoCoordinateSystem(Rect src, Rect srcCoordinateSystem, Rect targetCoordinateSystem) {
        int originOffsetX = -(targetCoordinateSystem.left - srcCoordinateSystem.left);
        int originOffsetY = -(targetCoordinateSystem.top - srcCoordinateSystem.top);

        src.offset(originOffsetX, originOffsetY);
    }

    public static Matrix transformMatrixTranslationtoCoordinateSystem(Matrix src, Rect srcCoordinateSystem, Rect targetCoordinateSystem) {
        float[] v = new float[9];
        src.getValues(v);

        Matrix target = new Matrix(src);
        float posX = v[Matrix.MTRANS_X];
        float posY = v[Matrix.MTRANS_Y];

        int originOffsetX = -(targetCoordinateSystem.left - srcCoordinateSystem.left);
        int originOffsetY = -(targetCoordinateSystem.top - srcCoordinateSystem.top);

        posX += originOffsetX;
        posY += originOffsetY;

        float[] w = new float[9];
        target.getValues(w);

        w[Matrix.MTRANS_X] = posX;
        w[Matrix.MTRANS_Y] = posY;

        target.setValues(w);

        return target;
    }

    public static float getRotationAngle(Matrix src) {
        float[] v = new float[9];
        src.getValues(v);

        return (float) Math.atan2(v[Matrix.MSKEW_X], v[Matrix.MSCALE_X]);
    }

    // Relative to top left of the coordinate system
    public static class Pose2D {
        public Rect pos; // x, y, width, height
        public int angle; // degrees

        public Pose2D(Rect pos, int angle) {
            this.pos = pos;
            this.angle = angle;
        }
    }

    // Relative to top left of the coordinate system
    public static class PoseF2D {
        public PointF pos;
        public PointF size;
        public float angle;

        public PoseF2D(PointF pos, PointF size, float angle) {
            this.pos = pos;
            this.size = size;
            this.angle = angle;
        }

        public PoseF2D(PoseF2D poseF2D) {
            this(poseF2D.pos, poseF2D.size, poseF2D.angle);
        }

        public static PoseF2D uiToGL(PoseF2D pose) {
            PoseF2D glPose = new PoseF2D(new PointF(pose.pos.x, -pose.pos.y), pose.size, pose.angle);
            return glPose;
        }
    }

    public static class FrameF {
        public float x;
        public float y;
        public float size;
        public float pivotX;
        public float pivotY;

        public FrameF() {
        }

        public FrameF(float x, float y, float size, float pivotX, float pivotY) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.pivotX = pivotX;
            this.pivotY = pivotY;
        }

        public FrameF(JSONObject json) {
            if (json == null) return;
            x = (float) json.optDouble("x", 0);
            y = (float) json.optDouble("y", 0);
            size = (float) json.optDouble("size", 0.5);
            pivotX = (float) json.optDouble("pivot_x", 0.5);
            pivotY = (float) json.optDouble("pivot_y", 0.5);
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("x", x);
                json.put("y", y);
                json.put("size", size);
                json.put("pivot_x", pivotX);
                json.put("pivot_y", pivotY);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json;
        }
    }

    public static class Eye2DFrame {
        public float time;
        public Eye2D eye2D;

        public Eye2DFrame(float time, Eye2D eye2D) {
            this.time = time;
            this.eye2D = eye2D;
        }
    }

    public static class Eye2D {
        public float posX, posY;
        public float zoom;

        public Eye2D(float x, float y, float zoom) {
            this.posX = x;
            this.posY = y;
            this.zoom = zoom;
        }
    }

    /**
     * @param source source coordinate
     * @param angle  in radians
     * @return new coordinate after transformation of axes
     */
    public static PointF transformAxesRotation(PointF source, float angle) {
        double[][] transform = new double[][]{{Math.cos(angle), -Math.sin(angle)}, {Math.sin(angle), Math.cos(angle)}};
        double[] target = com.roposo.core.util.graphics.Matrix.multiply(transform, new double[]{source.x, source.y});
        return new PointF((float) target[0], (float) target[1]);
    }
}
