package com.visagetechnologies.visagetrackerdemo;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Created by sanchitsharma on 13/03/18.
 */

public class FaceData implements Serializable {

    private float[] faceTranslation;
    private float[] faceRotation;
    private float[] faceModelVertices;
    private float[] faceVerticesProjected;
    private float[] faceModelTextureCoords;
    private int faceScale;
    private float cameraFocus;
    private int[] correctedTriangles;
//    private Rect faceRect;
    private Bitmap bitmap;
    private int[] medianColor;
    private float[] leftEyeVertices;
    private float[] rightEyeVertices;
    private float[] leftEyeTextureCoordinates;
    private float[] rightEyeTextureCoordinates;
    private int[] eyeTriangles;

    public FaceData(float[] faceTranslation, float[] faceRotation, float[] faceModelVertices, float[] faceModelTextureCoords, int faceScale, float cameraFocus, int[] correctedTriangles,float[] leftEyeVertices, float[] rightEyeVertices, float[] leftEyeTextureCoordinates, float[] rightEyeTextureCoordinates,int[] eyeTriangles){
        this.cameraFocus = cameraFocus;
        this.faceScale = faceScale;
        this.faceTranslation = faceTranslation;
        this.faceRotation = faceRotation;
        this.faceModelVertices = faceModelVertices;
        this.faceModelTextureCoords = faceModelTextureCoords;
        this.correctedTriangles = correctedTriangles;
        this.leftEyeTextureCoordinates = leftEyeTextureCoordinates;
        this.rightEyeTextureCoordinates = rightEyeTextureCoordinates;
        this.leftEyeVertices = leftEyeVertices;
        this.rightEyeVertices = rightEyeVertices;
        this.eyeTriangles = eyeTriangles;
        reverseTexX();
        //calculateRect();
        //calculateMedian();
    }

    public FaceData(){

    }

//    public void calculateMedian(){
//        this.setMedianColor(Utils.getMedian(bitmap,faceRect));
//    }

//    public void calculateRect(){
//        Rect rect = new Rect();
//        float minX = 1.0f;
//        float maxX = 0.0f;
//        float minY = 1.0f;
//        float maxY = 0.0f;
//        int numFaceContourPoints = this.faceContourVertices.length/3;
//        for(int i = 0; i < numFaceContourPoints; i++){
//            if(minX > this.faceContourVertices[3 * i]){
//                minX = this.faceContourVertices[3 * i];
//            }
//            if(maxX < this.faceContourVertices[3 * i]){
//                maxX = this.faceContourVertices[3 * i];
//            }
//            if(minY > this.faceContourVertices[3 * i + 1]){
//                minY = this.faceContourVertices[3 * i + 1];
//            }
//            if(maxY < this.faceContourVertices[3 * i + 1]){
//                maxY = this.faceContourVertices[3 * i + 1];
//            }
//        }
//        rect.left = (int)(minX * bitmap.getWidth());
//        rect.top = (int) (minY * bitmap.getHeight());
//        rect.right = (int) (maxX * bitmap.getWidth());
//        rect.bottom = (int) (maxY * bitmap.getHeight());
//        this.faceRect = rect;
//    }

    public float[] getFaceTranslation() {
        return faceTranslation;
    }

    public void setFaceTranslation(float[] faceTranslation) {
        this.faceTranslation = faceTranslation;
    }

    public float[] getFaceRotation() {
        return faceRotation;
    }

    public void setFaceRotation(float[] faceRotation) {
        this.faceRotation = faceRotation;
    }

    public float[] getFaceModelVertices() {
        return faceModelVertices;
    }

    public void setFaceModelVertices(float[] faceModelVertices) {
        this.faceModelVertices = faceModelVertices;
    }

    public float[] getFaceModelTextureCoords() {
        return faceModelTextureCoords;
    }

    public void setFaceModelTextureCoords(float[] faceModelTextureCoords) {
        this.faceModelTextureCoords = faceModelTextureCoords;
    }

    private void reverseTexX(){
        float[] texCoords = this.getFaceModelTextureCoords();
        for(int i = 0; i < this.getFaceModelVertices().length/3; i++){
            texCoords[2 * i] = (1 - texCoords[2 * i]);
        }
        this.setFaceModelTextureCoords(texCoords);
    }

    public int getFaceScale() {
        return faceScale;
    }

    public void setFaceScale(int faceScale) {
        this.faceScale = faceScale;
    }

    public float getCameraFocus() {
        return cameraFocus;
    }

    public void setCameraFocus(float cameraFocus) {
        this.cameraFocus = cameraFocus;
    }

    public float[] getFaceVerticesProjected() {
        return faceVerticesProjected;
    }

    public void setFaceVerticesProjected(float[] faceVerticesProjected) {
        this.faceVerticesProjected = faceVerticesProjected;
    }

//    public int[] getFaceModelTriangles() {
//        return faceModelTriangles;
//    }
//
//    public void setFaceModelTriangles(int[] faceModelTriangles) {
//        this.faceModelTriangles = faceModelTriangles;
//    }
//
//    public float[] getFaceContourVertices() {
//        return faceContourVertices;
//    }
//
//    public void setFaceContourVertices(float[] faceContourVertices) {
//        this.faceContourVertices = faceContourVertices;
//    }
//
//    public float[] getFaceContourTextureCoordinates() {
//        return faceContourTextureCoordinates;
//    }
//
//    public void setFaceContourTextureCoordinates(float[] faceContourTextureCoordinates) {
//        this.faceContourTextureCoordinates = faceContourTextureCoordinates;
//    }

//    public Rect getFaceRect() {
//        return faceRect;
//    }

//    public void setFaceRect(Rect faceRect) {
//        this.faceRect = faceRect;
//    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

//    @Override
//    public int compare(FaceData lhs, FaceData rhs) {
//        return lhs.faceRect.left-rhs.faceRect.left;
//    }

    public int[] getMedianColor() {
        return medianColor;
    }

    public void setMedianColor(int[] medianColor) {
        this.medianColor = medianColor;
    }

    public int[] getCorrectedTriangles() {
        return correctedTriangles;
    }

    public void setCorrectedTriangles(int[] correctedTriangles) {
        this.correctedTriangles = correctedTriangles;
    }

    public float[] getLeftEyeVertices() {
        return leftEyeVertices;
    }

    public void setLeftEyeVertices(float[] leftEyeVertices) {
        this.leftEyeVertices = leftEyeVertices;
    }

    public float[] getRightEyeVertices() {
        return rightEyeVertices;
    }

    public void setRightEyeVertices(float[] rightEyeVertices) {
        this.rightEyeVertices = rightEyeVertices;
    }

    public float[] getLeftEyeTextureCoordinates() {
        return leftEyeTextureCoordinates;
    }

    public void setLeftEyeTextureCoordinates(float[] leftEyeTextureCoordinates) {
        this.leftEyeTextureCoordinates = leftEyeTextureCoordinates;
    }

    public float[] getRightEyeTextureCoordinates() {
        return rightEyeTextureCoordinates;
    }

    public void setRightEyeTextureCoordinates(float[] rightEyeTextureCoordinates) {
        this.rightEyeTextureCoordinates = rightEyeTextureCoordinates;
    }

    public int[] getEyeTriangles() {
        return eyeTriangles;
    }

    public void setEyeTriangles(int[] eyeTriangles) {
        this.eyeTriangles = eyeTriangles;
    }
}
