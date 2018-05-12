#include <jni.h>
#include <EGL/egl.h> 
#include <GLES/gl.h>
#include <vector>
#include <stdio.h>
#include <unistd.h>
#include "VisageTracker.h"
#include "VisageRendering.h"
#include "AndroidImageCapture.h"
#include "AndroidCameraCapture.h"
#include "WrapperOpenCV.h"
#include "cv.h"
#include "highgui.h"

#include <android/log.h>
#define  LOG_TAG    "TrackerWrapper"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

using namespace std;
using namespace VisageSDK;

static AndroidImageCapture *a_cap_image = 0;
static AndroidCameraCapture *a_cap_camera = 0;
static VsImage *drawImageBuffer = 0;
static VsImage *renderImage = 0;

static VsImage *drawSourceImageBuffer = 0;
static VsImage *renderSourceImage = 0;

static VsImage *drawDestinationImageBuffer = 0;
static VsImage *renderDestinationImage = 0;

static float m_fps = 0;

void Sleep(int ms) {usleep(ms*1000);}

// neccessary prototype declaration for licensing
namespace VisageSDK
{
void initializeLicenseManager(JNIEnv* env, jobject obj, const char *licenseKeyFileName, void (*alertFunction)(const char*) = 0);
}

/** \file AndroidWrapper.cpp
 * Implementation of simple interface around visage|SDK VisageTracker functionality.
 *
 * In order for Android application, which uses Java as its primary programming language, to communicate with visage|SDK functionality, 
 * which uses C++ as its primary language it is necessary to use Java Native Interface as a framework between the two.  
 *
 * Key members of wrapper are:
 * - m_Tracker: the VisageTracker object
 * - trackingData: the TrackingData object used for retrieving and holding tracking data
 * - displayTrackingResults: method that demonstrates how to acquire, use and display tracking data and 3D face model
 * 
 */

extern "C" {

// ********************************
// Variables used in tracking thread
// ********************************
const int MAX_FACES = 4;
static VisageTracker *m_Tracker = 0;
static FaceData trackingData[MAX_FACES];

static FaceData sourceImageTrackingData[MAX_FACES];
int *sourceTrackingStatus = 0;

static FaceData destinationImageTrackingData[MAX_FACES];
int *destinationTrackingStatus = 0;

int *trackingStatus = 0;
float displayFramerate = 0;
int timestamp = 0;
int trackingTime;

// ********************************
// Buffers for track->render communication
// ********************************
static FaceData trackingDataBuffer[MAX_FACES];
int trackingStatusBuffer[MAX_FACES];

// ********************************
// Variables used in rendering thread
// ********************************
static FaceData trackingDataRender[MAX_FACES];
int trackingStatusRender[MAX_FACES];
// Logo image
VsImage* logo = 0;

// ********************************
// Control flow variables
// ********************************
bool trackingOk = false;
bool isTracking = false;
bool orientationChanged = false;
bool trackerPaused = false;
bool trackerStopped;
//
int camOrientation;
int camHeight;
int camWidth;
int camFlip;
//
pthread_mutex_t displayRes_mutex;
pthread_mutex_t guardFrame_mutex;
//

GLuint texIds[3];

/**
* Texture ID for displaying frames from the tracker.
*/
GLuint frameTexId = 0;

GLuint instructionsTexId = 0;

/**
* Texture coordinates for displaying frames from the tracker.
*/
float xTexCoord;

/**
* Texture coordinates for displaying frames from the tracker.
*/
float yTexCoord;

/**
* Size of the texture for displaying frames from the tracker.
*/
int xTexSize;

/**
* Size of the texture for displaying frames from the tracker.
*/
int yTexSize;

/**
* Aspect of the video.
*/
float videoAspect;

/**
* Size of the OpenGL view.
*/
int glWidth;

/**
* Size of the OpenGL view.
*/
int glHeight;

// ********************************
// JNI variables
// ********************************
JNIEnv* _env;
jobject _obj;


// ********************************
// Helper functions
// ********************************

/**
 * Callback method for license notification.
 *
 * Alerts the user that the license is not valid
 */
void AlertCallback(const char* warningMessage)
{
	jclass dataClass = _env->FindClass("com/visagetechnologies/visagetrackerdemo/TrackerActivity");
	if (_env->ExceptionCheck())
			_env->ExceptionClear();
	if (dataClass != NULL)
	{
		jclass javaClassRef = (jclass) _env->NewGlobalRef(dataClass);
		jmethodID javaMethodRef = _env->GetMethodID(javaClassRef, "AlertDialogFunction", "(Ljava/lang/String;)V");
		if (_env->ExceptionCheck())
			_env->ExceptionClear();
		jstring message = _env->NewStringUTF(warningMessage);
		if (javaMethodRef != 0)
			_env->CallVoidMethod(_obj, javaMethodRef, message);

		_env->DeleteGlobalRef(javaClassRef);
		_env->DeleteLocalRef(message);
	}
}



/**
 * Simple timer function
 */
long getTimeNsec() {
	struct timespec now;
	clock_gettime(CLOCK_REALTIME, &now);
	return (long) ((now.tv_sec*1000000000LL + now.tv_nsec)/1000000LL);
}

/**
 * Loads logo using OpenCV
 */
IplImage* loadLogo(std::string logoPath)
{
	//load logo image
	IplImage* originalLogo = cvLoadImage(logoPath.c_str(), CV_LOAD_IMAGE_UNCHANGED);
	if (!originalLogo)
		return 0;
	cvCvtColor(originalLogo, originalLogo, CV_BGRA2RGBA);
	return originalLogo;
}

// ********************************

// ********************************
// Wrapper function
// ********************************

/**
 * Method for initializing the tracker.
 *
 * This method creates a new VisageTracker objects and initializes the tracker.
 * @param configFilename - name of the configuration, along with the full path, to be used in tracking
 */
void Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_TrackerInit(JNIEnv *env, jobject obj, jstring configFilename)
{
	_env = env;
	_obj = obj;
	const char *_configFilename = env->GetStringUTFChars(configFilename, 0);
	trackerStopped = false;

	//initialize licensing
	//example how to initialize license key
	initializeLicenseManager(env, obj, "/data/data/com.visagetechnologies.visagetrackerdemo/files/license-key-name.vlc", AlertCallback);

	logo = (VsImage*)loadLogo("/data/data/com.visagetechnologies.visagetrackerdemo/files/logo.png");
	if (!logo)
		LOGE("Logo was not successfully loaded");

	//Set up mutex for track->render thread synchronization
	pthread_mutex_destroy(&displayRes_mutex);
	pthread_mutex_init(&displayRes_mutex, NULL);
	//Set up mutex for tracking->stopping tracking synchronization
	pthread_mutex_destroy(&guardFrame_mutex);
	pthread_mutex_init(&guardFrame_mutex, NULL);

	//Delete previously allocated objects
	delete a_cap_camera;
	a_cap_camera = 0;
	a_cap_image = 0;

	//Initialize tracker
	m_Tracker = new VisageTracker(_configFilename);

	LOGI("%s", _configFilename);
	env->ReleaseStringUTFChars(configFilename, _configFilename);
}
/**
    Method to send face data to java
**/
JNIEXPORT jobjectArray JNICALL
Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_getFaces(JNIEnv *env, jobject obj) {
    _env = env;
    _obj = obj;
    int j = 0;
    int numFaces = 0;
    for(j = 0; j < MAX_FACES; j++){
        if (trackingStatus[j] == TRACK_STAT_OK){
            numFaces++;
        }
    }
    jclass faceDataClass = (_env)->FindClass("com/visagetechnologies/visagetrackerdemo/FaceData");
    jobjectArray faceObjArray = (_env)->NewObjectArray(numFaces,faceDataClass,NULL);
    int k = 0;
    for(j=0; j < MAX_FACES; j++){
        if (trackingStatus[j] == TRACK_STAT_OK){
            jmethodID methodId = (_env)->GetMethodID(faceDataClass, "<init>", "([F[F[F[F[FIF)V");
            int numVertices = trackingData[j].faceModelVertexCount;
            float vertexDataTemp[numVertices*3];
            float textureDataTemp[numVertices*2];
            float projectedDataTemp[numVertices*2];
            jfloatArray translationData = (_env)->NewFloatArray(3);
            jfloatArray rotationData = (_env)->NewFloatArray(3);
            jfloatArray vertexData = (_env)->NewFloatArray(3 * numVertices);
            jfloatArray textureData = (_env)->NewFloatArray(2 * numVertices);
            jfloatArray projectedData = (_env)->NewFloatArray(2 * numVertices);
            int i = 0;

            for(i=0;i<numVertices;i++)
            {
                // Vertex Data
                vertexDataTemp[3 * i] = trackingData[j].faceModelVertices[3 * i];
                vertexDataTemp[3 * i + 1] = trackingData[j].faceModelVertices[3 * i + 1];
                vertexDataTemp[3 * i + 2] = trackingData[j].faceModelVertices[3 * i + 2];

                // Texture Data
                textureDataTemp[2 * i] = trackingData[j].faceModelTextureCoords[2 * i];
                textureDataTemp[2 * i + 1] = trackingData[j].faceModelTextureCoords[2 * i + 1];
                textureDataTemp[2 * i + 2] = trackingData[j].faceModelTextureCoords[2 * i + 2];

                // 2D Projected Data
                projectedDataTemp[2 * i] = trackingData[j].faceModelVerticesProjected[2 * i];
                projectedDataTemp[2 * i + 1] = trackingData[j].faceModelVerticesProjected[2 * i + 1];
                projectedDataTemp[2 * i + 2] = trackingData[j].faceModelVerticesProjected[2 * i + 2];
            }
            (_env)->SetFloatArrayRegion(vertexData,0,3 * numVertices,vertexDataTemp);
            (_env)->SetFloatArrayRegion(textureData,0,2 * numVertices,textureDataTemp);
            (_env)->SetFloatArrayRegion(projectedData,0,2 * numVertices,projectedDataTemp);
            (_env)->SetFloatArrayRegion(translationData,0,3,trackingData[j].faceTranslation);
            (_env)->SetFloatArrayRegion(rotationData,0,3,trackingData[j].faceRotation);
            jobject faceData = (_env)->NewObject(faceDataClass, methodId,translationData,rotationData,vertexData,textureData,projectedData,trackingData[0].faceScale,trackingData[0].cameraFocus);
            (_env)->SetObjectArrayElement(faceObjArray , k, faceData);
            k++;
        }
    }
    return faceObjArray;
 }

/**
 * Method for "pausing" tracking
 *
 * Causes the tracking thread to run without doing any work - blocks the calls to the track function
 */
void Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_PauseTracker(JNIEnv *env, jobject obj)
{
	trackerPaused = true;
}

/**
 * Method for starting tracking in image
 *
 * Initiates a while loop. Image is grabbed and track() function is called every iteration.
 * Copies data to the buffers for rendering.
 */
void Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_TrackFromImage(JNIEnv *env, jobject obj, jint width, jint height )
{
	while(!trackerStopped)
	{
		if (m_Tracker && a_cap_image && !trackerStopped)
		{
			pthread_mutex_lock(&guardFrame_mutex);
			long ts;
			drawImageBuffer = a_cap_image->GrabFrame(ts);
			if (!renderImage)
				renderImage = vsCloneImage(drawImageBuffer);
			if (trackerStopped || drawImageBuffer == 0)
			{
				pthread_mutex_unlock(&guardFrame_mutex);
				LOGI("%s","Tracking completed 1");
				return;
			}
			long startTime = getTimeNsec();
			trackingStatus = m_Tracker->track(width, height,  (const char*)drawImageBuffer->imageData, trackingData, VISAGE_FRAMEGRABBER_FMT_RGB, VISAGE_FRAMEGRABBER_ORIGIN_TL, 0, -1, MAX_FACES);
			long endTime = getTimeNsec();
			trackingTime = (int)(endTime - startTime);
			pthread_mutex_unlock(&guardFrame_mutex);
			pthread_mutex_lock(&displayRes_mutex);
			for (int i=0; i<MAX_FACES; i++)
			{
				if (trackingStatus[i] == TRACK_STAT_OFF)
					continue;
				std::swap(trackingDataBuffer[i], trackingData[i]);
				trackingStatusBuffer[i] = trackingStatus[i];
				trackingOk = true;

			}
			isTracking = true;
			pthread_mutex_unlock(&displayRes_mutex);
		}
		else
			Sleep(100);
        LOGI("%s","Tracking going on");
	}
	return;
}


jobjectArray Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_TrackSource(JNIEnv *env, jobject obj, jint width, jint height){
    int numIterations = 30;
    int numFramesProcessed = 0;
    while ((numFramesProcessed < numIterations)){
        long ts;
        drawSourceImageBuffer = a_cap_image->GrabSourceFrame(ts);
        sourceTrackingStatus = m_Tracker->track(width,height,(const char*)drawSourceImageBuffer->imageData, sourceImageTrackingData,VISAGE_FRAMEGRABBER_FMT_RGB, VISAGE_FRAMEGRABBER_ORIGIN_TL, 0, -1, MAX_FACES);
        numFramesProcessed++;
    }
    int numFaces = 0;
    int j = 0;
    for(j = 0; j < MAX_FACES; j++){
        if (sourceTrackingStatus[j] == TRACK_STAT_OK){
            numFaces++;
        }
    }
    jclass faceDataClass = (_env)->FindClass("com/visagetechnologies/visagetrackerdemo/FaceData");
    jobjectArray faceObjArray = (_env)->NewObjectArray(numFaces,faceDataClass,NULL);
    int k = 0;
    float* uvBounds = new float[2];
    renderSourceImage = vsCloneImage(drawSourceImageBuffer);
    VisageRendering::getTextureUnwrapDimensions(renderSourceImage,uvBounds);
    for(j=0; j < MAX_FACES; j++){
        if (sourceTrackingStatus[j] == TRACK_STAT_OK){
            jmethodID methodId = (_env)->GetMethodID(faceDataClass, "<init>", "([F[F[F[F[F[IIF[F[F[I[F[F[F[F[I)V");
            int numVertices = sourceImageTrackingData[j].faceModelVertexCount;
            int numTriangles = sourceImageTrackingData[j].faceModelTriangleCount;
            float vertexDataTemp[numVertices*3];
            float textureDataTemp[numVertices*2];
            float projectedDataTemp[numVertices*2];
            int triangleDataTemp[numTriangles*3];
            jfloatArray translationData = (_env)->NewFloatArray(3);
            jfloatArray rotationData = (_env)->NewFloatArray(3);
            jfloatArray vertexData = (_env)->NewFloatArray(3 * numVertices);
            jfloatArray textureData = (_env)->NewFloatArray(2 * numVertices);
            jfloatArray projectedData = (_env)->NewFloatArray(2 * numVertices);
            jintArray triangleData = (_env)->NewIntArray(3 * numTriangles);


            int *length = new int[1];
            int* correctedTriangles = VisageRendering::getCorrectedTriangleData(&sourceImageTrackingData[j], length);
            jintArray correctedTriangleData = (_env)->NewIntArray(*length);

            int numContourVertices = VisageRendering::getVerticesLength(&sourceImageTrackingData[j]);
            jfloatArray faceContourVertices = (_env)->NewFloatArray(3 * numContourVertices);
            jfloatArray faceContourTextureCoordinates = (_env)->NewFloatArray(2 * numContourVertices);


            float* contourVerticesTemp = new float[3 * numContourVertices];
            VisageRendering::getVertexArray(&sourceImageTrackingData[j],contourVerticesTemp);


            float* contourTextureTemp = new float[2 * numContourVertices];
            VisageRendering::getTextureCoordinateArray(&sourceImageTrackingData[j],uvBounds,contourTextureTemp);

            vector<float> rightSpline3D;
            vector<float> rightSpline2D;
            VisageRendering::getRightEyeSpline3D(&sourceImageTrackingData[j],rightSpline3D);
            VisageRendering::getRightEyeSpline2D(&sourceImageTrackingData[j],rightSpline2D);
            int rightEyeSpline3DVertexCount = (int)rightSpline3D.size() / 3;
            int rightEyeSpline2DVertexCount = (int)rightSpline2D.size() / 2;
            LOGI("%s - %d","rightEyeSpline3DVertexCount is ",rightEyeSpline3DVertexCount);
            LOGI("%s - %d","rightEyeSpline2DVertexCount is ",rightEyeSpline2DVertexCount);
            float* rightEyeVertices = new float[3 * rightEyeSpline3DVertexCount];
            VisageRendering::getRightEyeModel(rightSpline3D,rightEyeVertices);
            float* rightEyeTextureCoordinates = new float[2 * rightEyeSpline2DVertexCount];
            VisageRendering::getRightEyeTexture(rightSpline2D,rightEyeTextureCoordinates);
            jfloatArray rightEyeVerticesArray = (_env)->NewFloatArray(3 * rightEyeSpline3DVertexCount);
            jfloatArray rightEyeTextureCoordinatesArray = (_env)->NewFloatArray(2 * rightEyeSpline2DVertexCount);



            vector<float> leftSpline3D;
            vector<float> leftSpline2D;
            VisageRendering::getLeftEyeSpline3D(&sourceImageTrackingData[j],leftSpline3D);
            VisageRendering::getLeftEyeSpline2D(&sourceImageTrackingData[j],leftSpline2D);
            int leftEyeSpline3DVertexCount = (int)leftSpline3D.size() / 3;
            int leftEyeSpline2DVertexCount = (int)leftSpline2D.size() / 2;
            float* leftEyeVertices = new float[3 * leftEyeSpline3DVertexCount];
            VisageRendering::getLeftEyeModel(leftSpline3D,leftEyeVertices);
            float* leftEyeTextureCoordinates = new float[2 * leftEyeSpline2DVertexCount];
            VisageRendering::getLeftEyeTexture(leftSpline2D,leftEyeTextureCoordinates);
            jfloatArray leftEyeVerticesArray = (_env)->NewFloatArray(3 * leftEyeSpline3DVertexCount);
            jfloatArray leftEyeTextureCoordinatesArray = (_env)->NewFloatArray(2 * leftEyeSpline2DVertexCount);


            jintArray eyeTriangleArray = (_env)->NewIntArray(18);
            int* triangles = new int[18];
            VisageRendering::getEyeTriangles(triangles);
            int i = 0;

            for(i=0;i<numVertices;i++)
            {
                // Vertex Data
                vertexDataTemp[3 * i] = sourceImageTrackingData[j].faceModelVertices[3 * i];
                vertexDataTemp[3 * i + 1] = sourceImageTrackingData[j].faceModelVertices[3 * i + 1];
                vertexDataTemp[3 * i + 2] = sourceImageTrackingData[j].faceModelVertices[3 * i + 2];

                // Texture Data
                textureDataTemp[2 * i] = sourceImageTrackingData[j].faceModelTextureCoords[2 * i];
                textureDataTemp[2 * i + 1] = sourceImageTrackingData[j].faceModelTextureCoords[2 * i + 1];
                textureDataTemp[2 * i + 2] = sourceImageTrackingData[j].faceModelTextureCoords[2 * i + 2];

                // 2D Projected Data
                projectedDataTemp[2 * i] = sourceImageTrackingData[j].faceModelVerticesProjected[2 * i];
                projectedDataTemp[2 * i + 1] = sourceImageTrackingData[j].faceModelVerticesProjected[2 * i + 1];
                projectedDataTemp[2 * i + 2] = sourceImageTrackingData[j].faceModelVerticesProjected[2 * i + 2];
            }
            int n = 0;
            for(n = 0; n < numTriangles; n++){
                triangleDataTemp[3 * n] = sourceImageTrackingData[j].faceModelTriangles[3 * n];
                triangleDataTemp[3 * n + 1] = sourceImageTrackingData[j].faceModelTriangles[3 * n + 1];
                triangleDataTemp[3 * n + 2] = sourceImageTrackingData[j].faceModelTriangles[3 * n + 2];
            }
            (_env)->SetFloatArrayRegion(vertexData,0,3 * numVertices,vertexDataTemp);
            (_env)->SetFloatArrayRegion(textureData,0,2 * numVertices,textureDataTemp);
            (_env)->SetFloatArrayRegion(projectedData,0,2 * numVertices,projectedDataTemp);
            (_env)->SetIntArrayRegion(triangleData,0,2 * numTriangles,triangleDataTemp);
            (_env)->SetFloatArrayRegion(translationData,0,3,sourceImageTrackingData[j].faceTranslation);
            (_env)->SetFloatArrayRegion(rotationData,0,3,sourceImageTrackingData[j].faceRotation);
            (_env)->SetIntArrayRegion(correctedTriangleData,0,*length,correctedTriangles);


            (_env)->SetFloatArrayRegion(faceContourVertices,0,3 * numContourVertices,contourVerticesTemp);
            (_env)->SetFloatArrayRegion(faceContourTextureCoordinates,0,2 * numContourVertices,contourTextureTemp);


            (_env)->SetFloatArrayRegion(rightEyeVerticesArray,0,3 * rightEyeSpline3DVertexCount,rightEyeVertices);
            (_env)->SetFloatArrayRegion(leftEyeVerticesArray,0,3 * leftEyeSpline3DVertexCount,leftEyeVertices);

            (_env)->SetFloatArrayRegion(rightEyeTextureCoordinatesArray,0,2 * rightEyeSpline2DVertexCount,rightEyeTextureCoordinates);
            (_env)->SetFloatArrayRegion(leftEyeTextureCoordinatesArray,0,2 * leftEyeSpline2DVertexCount,leftEyeTextureCoordinates);
            (_env)->SetIntArrayRegion(eyeTriangleArray,0,18,triangles);

            jobject faceData = (_env)->NewObject(faceDataClass, methodId,translationData,rotationData,vertexData,textureData,projectedData,triangleData,sourceImageTrackingData[0].faceScale,sourceImageTrackingData[0].cameraFocus,faceContourVertices,faceContourTextureCoordinates,correctedTriangleData,leftEyeVerticesArray,rightEyeVerticesArray,leftEyeTextureCoordinatesArray,rightEyeTextureCoordinatesArray,eyeTriangleArray);
            (_env)->SetObjectArrayElement(faceObjArray , k, faceData);
            k++;
        }
    }
    return faceObjArray;
}


jobjectArray Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_TrackDestination(JNIEnv *env, jobject obj, jint width, jint height){
    int numIterations = 30;
    int numFramesProcessed = 0;
    while ((numFramesProcessed < numIterations)){
        long ts;
        drawDestinationImageBuffer = a_cap_image->GrabDestinationFrame(ts);
        destinationTrackingStatus = m_Tracker->track(width,height,(const char*)drawDestinationImageBuffer->imageData, destinationImageTrackingData,VISAGE_FRAMEGRABBER_FMT_RGB, VISAGE_FRAMEGRABBER_ORIGIN_TL, 0, -1, MAX_FACES);
        numFramesProcessed++;
    }
    int numFaces = 0;
    int j = 0;
    for(j = 0; j < MAX_FACES; j++){
        if (destinationTrackingStatus[j] == TRACK_STAT_OK){
            numFaces++;
        }
    }
    jclass faceDataClass = (_env)->FindClass("com/visagetechnologies/visagetrackerdemo/FaceData");
    jobjectArray faceObjArray = (_env)->NewObjectArray(numFaces,faceDataClass,NULL);
    int k = 0;
    float* uvBounds = new float[2];
    renderDestinationImage = vsCloneImage(drawDestinationImageBuffer);
    VisageRendering::getTextureUnwrapDimensions(renderDestinationImage,uvBounds);
    for(j=0; j < MAX_FACES; j++){
        if (destinationTrackingStatus[j] == TRACK_STAT_OK){
            jmethodID methodId = (_env)->GetMethodID(faceDataClass, "<init>", "([F[F[F[F[F[IIF[F[F[I[F[F[F[F[I)V");
            int numVertices = destinationImageTrackingData[j].faceModelVertexCount;
            int numTriangles = destinationImageTrackingData[j].faceModelTriangleCount;
            float vertexDataTemp[numVertices*3];
            float textureDataTemp[numVertices*2];
            float projectedDataTemp[numVertices*2];
            int triangleDataTemp[numTriangles*3];
            jfloatArray translationData = (_env)->NewFloatArray(3);
            jfloatArray rotationData = (_env)->NewFloatArray(3);
            jfloatArray vertexData = (_env)->NewFloatArray(3 * numVertices);
            jfloatArray textureData = (_env)->NewFloatArray(2 * numVertices);
            jfloatArray projectedData = (_env)->NewFloatArray(2 * numVertices);
            jintArray triangleData = (_env)->NewIntArray(3 * numTriangles);
            int i = 0;

            int *length = new int[1];
            int* correctedTriangles = VisageRendering::getCorrectedTriangleData(&destinationImageTrackingData[j], length);
            jintArray correctedTriangleData = (_env)->NewIntArray(*length);

            int numContourVertices = VisageRendering::getVerticesLength(&destinationImageTrackingData[j]);
            jfloatArray faceContourVertices = (_env)->NewFloatArray(3 * numContourVertices);
            jfloatArray faceContourTextureCoordinates = (_env)->NewFloatArray(2 * numContourVertices);


            float* contourVerticesTemp = new float[3 * numContourVertices];
            VisageRendering::getVertexArray(&destinationImageTrackingData[j],contourVerticesTemp);


            float* contourTextureTemp = new float[2 * numContourVertices];
            VisageRendering::getTextureCoordinateArray(&destinationImageTrackingData[j],uvBounds,contourTextureTemp);

            vector<float> rightSpline3D;
            vector<float> rightSpline2D;
            VisageRendering::getRightEyeSpline3D(&destinationImageTrackingData[j],rightSpline3D);
            VisageRendering::getRightEyeSpline2D(&destinationImageTrackingData[j],rightSpline2D);
            int rightEyeSpline3DVertexCount = (int)rightSpline3D.size() / 3;
            int rightEyeSpline2DVertexCount = (int)rightSpline2D.size() / 2;
            float* rightEyeVertices = new float[3 * rightEyeSpline3DVertexCount];
            VisageRendering::getRightEyeModel(rightSpline3D,rightEyeVertices);
            float* rightEyeTextureCoordinates = new float[2 * rightEyeSpline2DVertexCount];
            VisageRendering::getRightEyeTexture(rightSpline2D,rightEyeTextureCoordinates);
            jfloatArray rightEyeVerticesArray = (_env)->NewFloatArray(3 * rightEyeSpline3DVertexCount);
            jfloatArray rightEyeTextureCoordinatesArray = (_env)->NewFloatArray(2 * rightEyeSpline2DVertexCount);



            vector<float> leftSpline3D;
            vector<float> leftSpline2D;
            VisageRendering::getLeftEyeSpline3D(&destinationImageTrackingData[j],leftSpline3D);
            VisageRendering::getLeftEyeSpline2D(&destinationImageTrackingData[j],leftSpline2D);
            int leftEyeSpline3DVertexCount = (int)leftSpline3D.size() / 3;
            int leftEyeSpline2DVertexCount = (int)leftSpline2D.size() / 2;
            float* leftEyeVertices = new float[3 * leftEyeSpline3DVertexCount];
            VisageRendering::getLeftEyeModel(leftSpline3D,leftEyeVertices);
            float* leftEyeTextureCoordinates = new float[2 * leftEyeSpline2DVertexCount];
            VisageRendering::getLeftEyeTexture(leftSpline2D,leftEyeTextureCoordinates);
            jfloatArray leftEyeVerticesArray = (_env)->NewFloatArray(3 * leftEyeSpline3DVertexCount);
            jfloatArray leftEyeTextureCoordinatesArray = (_env)->NewFloatArray(2 * leftEyeSpline2DVertexCount);


            jintArray eyeTriangleArray = (_env)->NewIntArray(18);
            int* triangles = new int[18];
            VisageRendering::getEyeTriangles(triangles);
            for(i=0;i<numVertices;i++)
            {
                // Vertex Data
                vertexDataTemp[3 * i] = destinationImageTrackingData[j].faceModelVertices[3 * i];
                vertexDataTemp[3 * i + 1] = destinationImageTrackingData[j].faceModelVertices[3 * i + 1];
                vertexDataTemp[3 * i + 2] = destinationImageTrackingData[j].faceModelVertices[3 * i + 2];

                // Texture Data
                textureDataTemp[2 * i] = destinationImageTrackingData[j].faceModelTextureCoords[2 * i];
                textureDataTemp[2 * i + 1] = destinationImageTrackingData[j].faceModelTextureCoords[2 * i + 1];
                textureDataTemp[2 * i + 2] = destinationImageTrackingData[j].faceModelTextureCoords[2 * i + 2];

                // 2D Projected Data
                projectedDataTemp[2 * i] = destinationImageTrackingData[j].faceModelVerticesProjected[2 * i];
                projectedDataTemp[2 * i + 1] = destinationImageTrackingData[j].faceModelVerticesProjected[2 * i + 1];
                projectedDataTemp[2 * i + 2] = destinationImageTrackingData[j].faceModelVerticesProjected[2 * i + 2];
            }
            int n = 0;
            for(n = 0; n < numTriangles; n++){
                triangleDataTemp[3 * n] = destinationImageTrackingData[j].faceModelTriangles[3 * n];
                triangleDataTemp[3 * n + 1] = destinationImageTrackingData[j].faceModelTriangles[3 * n + 1];
                triangleDataTemp[3 * n + 2] = destinationImageTrackingData[j].faceModelTriangles[3 * n + 2];
            }
            (_env)->SetFloatArrayRegion(vertexData,0,3 * numVertices,vertexDataTemp);
            (_env)->SetFloatArrayRegion(textureData,0,2 * numVertices,textureDataTemp);
            (_env)->SetFloatArrayRegion(projectedData,0,2 * numVertices,projectedDataTemp);
            (_env)->SetIntArrayRegion(triangleData,0,2 * numTriangles,triangleDataTemp);
            (_env)->SetFloatArrayRegion(translationData,0,3,destinationImageTrackingData[j].faceTranslation);
            (_env)->SetFloatArrayRegion(rotationData,0,3,destinationImageTrackingData[j].faceRotation);
            (_env)->SetFloatArrayRegion(faceContourVertices,0,3 * numContourVertices,contourVerticesTemp);
            (_env)->SetFloatArrayRegion(faceContourTextureCoordinates,0,2 * numContourVertices,contourTextureTemp);
            (_env)->SetIntArrayRegion(correctedTriangleData,0,*length,correctedTriangles);

            (_env)->SetFloatArrayRegion(rightEyeVerticesArray,0,3 * rightEyeSpline3DVertexCount,rightEyeVertices);
            (_env)->SetFloatArrayRegion(leftEyeVerticesArray,0,3 * leftEyeSpline3DVertexCount,leftEyeVertices);

            (_env)->SetFloatArrayRegion(rightEyeTextureCoordinatesArray,0,2 * rightEyeSpline2DVertexCount,rightEyeTextureCoordinates);
            (_env)->SetFloatArrayRegion(leftEyeTextureCoordinatesArray,0,2 * leftEyeSpline2DVertexCount,leftEyeTextureCoordinates);
            (_env)->SetIntArrayRegion(eyeTriangleArray,0,18,triangles);

            jobject faceData = (_env)->NewObject(faceDataClass, methodId,translationData,rotationData,vertexData,textureData,projectedData,triangleData,destinationImageTrackingData[0].faceScale,destinationImageTrackingData[0].cameraFocus,faceContourVertices,faceContourTextureCoordinates,correctedTriangleData,leftEyeVerticesArray,rightEyeVerticesArray,leftEyeTextureCoordinatesArray,rightEyeTextureCoordinatesArray,eyeTriangleArray);
            (_env)->SetObjectArrayElement(faceObjArray , k, faceData);
            k++;
        }
    }
    return faceObjArray;
}



jobjectArray Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_Track(JNIEnv *env, jobject obj, jint width, jint height){
    int numIterations = 30;
    int numFramesProcessed = 0;
    while ((numFramesProcessed < numIterations)){
        long ts;
        drawImageBuffer = a_cap_image->GrabFrame(ts);
        trackingStatus = m_Tracker->track(width,height,(const char*)drawImageBuffer->imageData, trackingData,VISAGE_FRAMEGRABBER_FMT_RGB, VISAGE_FRAMEGRABBER_ORIGIN_TL, 0, -1, MAX_FACES);
        numFramesProcessed++;
    }
    int numFaces = 0;
    int j = 0;
    for(j = 0; j < MAX_FACES; j++){
        if (trackingStatus[j] == TRACK_STAT_OK){
            numFaces++;
        }
    }
    jclass faceDataClass = (_env)->FindClass("com/visagetechnologies/visagetrackerdemo/FaceData");
    jobjectArray faceObjArray = (_env)->NewObjectArray(numFaces,faceDataClass,NULL);
    int k = 0;
    for(j=0; j < MAX_FACES; j++){
        if (trackingStatus[j] == TRACK_STAT_OK){
            jmethodID methodId = (_env)->GetMethodID(faceDataClass, "<init>", "([F[F[F[F[F[IIF)V");
            int numVertices = trackingData[j].faceModelVertexCount;
            int numTriangles = trackingData[j].faceModelTriangleCount;
            float vertexDataTemp[numVertices*3];
            float textureDataTemp[numVertices*2];
            float projectedDataTemp[numVertices*2];
            int triangleDataTemp[numTriangles*3];
            jfloatArray translationData = (_env)->NewFloatArray(3);
            jfloatArray rotationData = (_env)->NewFloatArray(3);
            jfloatArray vertexData = (_env)->NewFloatArray(3 * numVertices);
            jfloatArray textureData = (_env)->NewFloatArray(2 * numVertices);
            jfloatArray projectedData = (_env)->NewFloatArray(2 * numVertices);
            jintArray triangleData = (_env)->NewIntArray(3 * numTriangles);
            int i = 0;

            for(i=0;i<numVertices;i++)
            {
                // Vertex Data
                vertexDataTemp[3 * i] = trackingData[j].faceModelVertices[3 * i];
                vertexDataTemp[3 * i + 1] = trackingData[j].faceModelVertices[3 * i + 1];
                vertexDataTemp[3 * i + 2] = trackingData[j].faceModelVertices[3 * i + 2];

                // Texture Data
                textureDataTemp[2 * i] = trackingData[j].faceModelTextureCoords[2 * i];
                textureDataTemp[2 * i + 1] = trackingData[j].faceModelTextureCoords[2 * i + 1];
                textureDataTemp[2 * i + 2] = trackingData[j].faceModelTextureCoords[2 * i + 2];

                // 2D Projected Data
                projectedDataTemp[2 * i] = trackingData[j].faceModelVerticesProjected[2 * i];
                projectedDataTemp[2 * i + 1] = trackingData[j].faceModelVerticesProjected[2 * i + 1];
                projectedDataTemp[2 * i + 2] = trackingData[j].faceModelVerticesProjected[2 * i + 2];
            }
            int n = 0;
            for(n = 0; n < numTriangles; n++){
                triangleDataTemp[3 * n] = trackingData[j].faceModelTriangles[3 * n];
                triangleDataTemp[3 * n + 1] = trackingData[j].faceModelTriangles[3 * n + 1];
                triangleDataTemp[3 * n + 2] = trackingData[j].faceModelTriangles[3 * n + 2];
            }
            (_env)->SetFloatArrayRegion(vertexData,0,3 * numVertices,vertexDataTemp);
            (_env)->SetFloatArrayRegion(textureData,0,2 * numVertices,textureDataTemp);
            (_env)->SetFloatArrayRegion(projectedData,0,2 * numVertices,projectedDataTemp);
            (_env)->SetIntArrayRegion(triangleData,0,2 * numTriangles,triangleDataTemp);
            (_env)->SetFloatArrayRegion(translationData,0,3,trackingData[j].faceTranslation);
            (_env)->SetFloatArrayRegion(rotationData,0,3,trackingData[j].faceRotation);
            jobject faceData = (_env)->NewObject(faceDataClass, methodId,translationData,rotationData,vertexData,textureData,projectedData,triangleData,trackingData[0].faceScale,trackingData[0].cameraFocus);
            (_env)->SetObjectArrayElement(faceObjArray , k, faceData);
            k++;
        }
    }
    return faceObjArray;
}

/**
 * Method that sets frame parameters
 *
 * Called initially before tracking starts and every time orientation changes. Creates buffer of
 * correct sizes and sets orientationChanged to true
 *
 * @param width - width of the received frame
 * @param height - height of the received frame
 * @param orientation - orientation of the frame derived from camera and screen orientation
 * @param flip - 1 if frame is mirrored, 0 if not
 */
int Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_setParameters(JNIEnv *env, jobject obj, jint width, jint height, jint orientation, jint flip)
{
	camOrientation = orientation;
	camHeight = height;
	camWidth = width;
	camFlip = flip;

	//Dispose of the previous drawImageBuffer
	if (!drawImageBuffer)
	{
		vsReleaseImage(&drawImageBuffer);
		drawImageBuffer = 0;
	}

	//Depending on the camera orientation (landscape or portrait), create a drawImageBuffer buffer for storing pixels that will be used in the tracking thread
	if (camOrientation == 90 || camOrientation == 270)
		drawImageBuffer = vsCreateImage(vsSize(height, width), VS_DEPTH_8U, 3);
	else
		drawImageBuffer = vsCreateImage(vsSize(width, height), VS_DEPTH_8U, 3);

	//Dispose of the previous drawImage
	vsReleaseImage(&renderImage);
	renderImage = 0;

	//Create a renderImage buffer based on the drawImageBuffer which will be used in the rendering thread
	//NOTE: Copying imageData between track and draw buffers is protected with mutexes
	renderImage = vsCloneImage(drawImageBuffer);

	orientationChanged = true;

	return 0;
}

/**
 * Method for starting tracking from camera
 *
 * Initiates a while loop. Image is grabbed and track() function is called every iteration.
 * Copies data to the buffers for rendering.
 */
void Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_TrackFromCam(JNIEnv *env, jobject obj)
{
	while (!trackerStopped)
	{
		if (m_Tracker && a_cap_camera && !trackerStopped && !trackerPaused)
		{
			pthread_mutex_lock(&guardFrame_mutex);
			long ts;
			VsImage *trackImage = a_cap_camera->GrabFrame(ts);
			if (trackerStopped || trackImage == 0)
			{
				pthread_mutex_unlock(&guardFrame_mutex);
				return;
			}
			long startTime = getTimeNsec();
			if (camOrientation == 90 || camOrientation == 270)
				trackingStatus = m_Tracker->track(camHeight, camWidth, trackImage->imageData, trackingData, VISAGE_FRAMEGRABBER_FMT_RGB, VISAGE_FRAMEGRABBER_ORIGIN_TL, 0, -1, MAX_FACES);
			else
				trackingStatus = m_Tracker->track(camWidth, camHeight, trackImage->imageData, trackingData, VISAGE_FRAMEGRABBER_FMT_RGB, VISAGE_FRAMEGRABBER_ORIGIN_TL, 0, -1, MAX_FACES);
			long endTime = getTimeNsec();
			trackingTime = (int)endTime - startTime;

			pthread_mutex_unlock(&guardFrame_mutex);

			//***
			//*** LOCK render thread while copying data for rendering ***
			//***
			pthread_mutex_lock(&displayRes_mutex);
			for (int i=0; i<MAX_FACES; i++)
			{
				if (trackingStatus[i] == TRACK_STAT_OFF)
					continue;
				std::swap(trackingDataBuffer[i], trackingData[i]);
				trackingStatusBuffer[i] = trackingStatus[i];
				//Signalize that at least one face was tracked
				trackingOk = true;

			}

			isTracking = true;

			if (trackingOk)
				vsCopy(trackImage, drawImageBuffer);

			//***
			//*** UNLOCK render thread ***
			//***
			pthread_mutex_unlock(&displayRes_mutex);
		}
		else
			Sleep(1);
	}
	return;
}


/**
 * Stops the tracker and cleans memory
 */
void Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_TrackerStop(JNIEnv *env, jobject obj)
{
	if (m_Tracker)
	{
		trackerStopped = true;
		trackingOk =false;
		pthread_mutex_lock(&guardFrame_mutex);
		for (int i=0; i<MAX_FACES; i++)
			{
				trackingStatusRender[i] = TRACK_STAT_OFF;
				trackingStatusBuffer[i] = TRACK_STAT_OFF;
				trackingStatus[i] = TRACK_STAT_OFF;
			}
		m_Tracker->stop();
		delete m_Tracker;
		m_Tracker = 0;
		vsReleaseImage(&drawImageBuffer);
		drawImageBuffer = 0;
		vsReleaseImage(&renderImage);
		renderImage = 0;
		VisageRendering::Reset();
		pthread_mutex_unlock(&guardFrame_mutex);

	}
}


/**
* Writes raw image data into @ref VisageSDK::AndroidImageCapture object. VisageTracker reads this image and performs tracking.
* @param frame byte array with image data
* @param width image width
* @param height image height
*/
void Java_com_visagetechnologies_visagetrackerdemo_ImageTrackerView_WriteFrameImage(JNIEnv *env, jobject obj, jbyteArray frame, jint width, jint height) 
{
	if (!a_cap_image)
		a_cap_image = new AndroidImageCapture(width, height, VISAGE_FRAMEGRABBER_FMT_RGB);
	jbyte *f = env->GetByteArrayElements(frame, 0);
	a_cap_image->WriteFrame((unsigned char *)f, (int)width, (int)height);
	env->ReleaseByteArrayElements(frame, f, 0);
}

/**
* Writes raw image data into @ref VisageSDK::AndroidImageCapture object. VisageTracker reads this image and performs tracking.
* @param frame byte array with image data
* @param width image width
* @param height image height
*/
void Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_WriteFrameImage(JNIEnv *env, jobject obj, jbyteArray frame, jint width, jint height)
{
	if (!a_cap_image)
		a_cap_image = new AndroidImageCapture(width, height, VISAGE_FRAMEGRABBER_FMT_RGB);
	jbyte *f = env->GetByteArrayElements(frame, 0);
	a_cap_image->WriteFrame((unsigned char *)f, (int)width, (int)height);
	env->ReleaseByteArrayElements(frame, f, 0);
}


/**
* Writes raw image data into @ref VisageSDK::AndroidImageCapture object. VisageTracker reads this image and performs tracking.
* @param frame byte array with image data
* @param width image width
* @param height image height
*/
void Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_WriteSouceFrameImage(JNIEnv *env, jobject obj, jbyteArray frame, jint width, jint height)
{
	if (!a_cap_image)
		a_cap_image = new AndroidImageCapture(width, height, VISAGE_FRAMEGRABBER_FMT_RGB);
	jbyte *f = env->GetByteArrayElements(frame, 0);
	a_cap_image->WriteSourceFrame((unsigned char *)f, (int)width, (int)height);
	env->ReleaseByteArrayElements(frame, f, 0);
}


/**
* Writes raw image data into @ref VisageSDK::AndroidImageCapture object. VisageTracker reads this image and performs tracking.
* @param frame byte array with image data
* @param width image width
* @param height image height
*/
void Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_WriteDestinationFrameImage(JNIEnv *env, jobject obj, jbyteArray frame, jint width, jint height)
{
	if (!a_cap_image)
		a_cap_image = new AndroidImageCapture(width, height, VISAGE_FRAMEGRABBER_FMT_RGB);
	jbyte *f = env->GetByteArrayElements(frame, 0);
	a_cap_image->WriteDestinationFrame((unsigned char *)f, (int)width, (int)height);
	env->ReleaseByteArrayElements(frame, f, 0);
}


/**
* Writes raw image data into @ref VisageSDK::AndroidCameraCapture object. VisageTracker reads this image and performs tracking. User should call this 
* function whenever new frame from camera is available. Data inside frame should be in Android NV21 (YUV420sp) format and @ref VisageSDK::AndroidCameraCapture
* will perform conversion to RGB.
*
* This function will reinitialize AndroidCameraCapture wrapper in case setParameter function was called, signaled by the orientationChanged flag. After creation,
* tracking will be resumed, signaled by trackerPaused flag.
* @param frame byte array with image data
*/
void Java_com_visagetechnologies_visagetrackerdemo_JavaCamTrackerView_WriteFrameCamera(JNIEnv *env, jobject obj, jbyteArray frame) 
{
	if (trackerStopped)
		return;

	//Reinitialize if the parameters changed or initialize if it is the first time
	if (!a_cap_camera || orientationChanged)
	{
		delete a_cap_camera;
		a_cap_camera = new AndroidCameraCapture(camWidth, camHeight, camOrientation, camFlip);
		orientationChanged = false;
		trackerPaused = false;
	}
	//
	jbyte *f = env->GetByteArrayElements(frame, 0);
	//Write frames from Java to native
	a_cap_camera->WriteFrameYUV((unsigned char *)f);
	env->ReleaseByteArrayElements(frame, f, 0);
}

// test Code starts

void Java_com_visagetechnologies_visagetrackerdemo_FaceRenderer_displayResults(JNIEnv* env, jobject obj, jint width, jint height, jint sourceIndex, jint destinationIndex,jintArray medianColor){
    long tsSource;
    drawSourceImageBuffer = a_cap_image->GrabSourceFrame(tsSource);
    long tsDestination;
    drawDestinationImageBuffer = a_cap_image->GrabDestinationFrame(tsDestination);
    renderSourceImage = vsCloneImage(drawSourceImageBuffer);
    //renderDestinationImage = vsCloneImage(drawDestinationImageBuffer);
    jint* color = env->GetIntArrayElements(medianColor, 0);
    int i = 0;
    for (i=0; i < 3; i++){
      env->ReleaseIntArrayElements(medianColor, color, 0);
    }
    LOGI("%s - %d - %d -%d ","color is ",color[0],color[1],color[2]);
    //int* destinationViewportDimensions = new int[2];
    int* sourceViewportDimensions = new int[2];
    //VisageRendering::calculateViewportDimensions(renderDestinationImage,width,height,destinationViewportDimensions);
    VisageRendering::calculateViewportDimensions(renderSourceImage,width,height,sourceViewportDimensions);
    //VisageRendering::DisplayDestinationImage(renderDestinationImage,destinationViewportDimensions[0],destinationViewportDimensions[1]);

    //VisageRendering::DisplaySourceFace(renderSourceImage,sourceViewportDimensions[0],sourceViewportDimensions[1],&sourceImageTrackingData[sourceIndex],&destinationImageTrackingData[destinationIndex],color);
    // Calculate destination Frame Viewport height and width
    //int i = 0;
    for(i = 0; i < MAX_FACES; i++){
        if(sourceTrackingStatus[i] == TRACK_STAT_OK){
            VisageRendering::DisplayWireFrame(&sourceImageTrackingData[i],sourceViewportDimensions[0],sourceViewportDimensions[1]);
        }
    }
}


void Java_com_visagetechnologies_visagetrackerdemo_FaceRenderer_displayFace(JNIEnv* env, jobject obj, jint width, jint height){
    glWidth = width;
    glHeight = height;
    pthread_mutex_lock(&displayRes_mutex);
    long ts;
    drawImageBuffer = a_cap_image->GrabFrame(ts);
    //copy image for rendering
    renderImage = vsCloneImage(drawImageBuffer);
    pthread_mutex_unlock(&displayRes_mutex);
    //calculate aspect corrected width and height
    videoAspect = renderImage->width / (float) renderImage->height;
    float tmp;
    if(renderImage->width < renderImage->height)
    {
    	tmp = glHeight;
    	glHeight = glWidth / videoAspect;
    	if (glHeight > tmp)
    	{
    	    glWidth  = glWidth*tmp/glHeight;
    		glHeight = tmp;
    	}
    }
    else
    {
    	tmp = glWidth;
    	glWidth = glHeight * videoAspect;
    	if (glWidth > tmp)
    	{
    		glHeight  = glHeight*tmp/glWidth;
    		glWidth = tmp;
    	}
    }
    //VisageRendering::DisplayImage(renderImage,glWidth,glHeight);
    VisageRendering::ClearGL();
    for (int i=0; i<MAX_FACES; i++)
    {

        if(trackingStatus[i] == TRACK_STAT_OK){
            VisageRendering::DisplayFace(&trackingData[i],renderImage, glWidth, glHeight);
        }
    }
}


/**
 * Method for displaying tracking results.
 *
 * This method is periodically called by the application rendering thread to get and display tracking results.
 * The results are retrieved using VisageSDK::TrackingData structure and displayed OpenGL ES for visual data (frames from camera and 3D face model).
 * It shows how to properly interpret tracking data and setup the OpenGL scene to display 3D face model retrieved from the tracker correctly aligned to the video frame.
 *
 * @param width width of GLSurfaceView used for rendering.
 * @param height height of GLSurfaceView used for rendering.
 */
bool Java_com_visagetechnologies_visagetrackerdemo_TrackerRenderer_displayTrackingStatus(JNIEnv* env, jobject obj, jint width, jint height)
{
	glWidth = width;
	glHeight = height;

	if (!m_Tracker || trackerStopped || !isTracking || !drawImageBuffer || trackerPaused)
		return false;

	//***
	//*** LOCK track thread to copy data for rendering ***
	//***
	pthread_mutex_lock(&displayRes_mutex);
	//copy image for rendering
	vsCopy(drawImageBuffer, renderImage);
	//copy faceData and statuses
	for (int i=0; i<MAX_FACES; i++)
	{
		if (trackingStatusBuffer[i] == TRACK_STAT_OFF)
			continue;
		trackingDataRender[i] = trackingDataBuffer[i];
		trackingStatusRender[i] = trackingStatusBuffer[i];
	}
	//***
	//*** UNLOCK track thread ***
	//***
	pthread_mutex_unlock(&displayRes_mutex);

	//calculate aspect corrected width and height
	//videoAspect = renderImage->width / (float) renderImage->height;

	//float tmp;
	//if(renderImage->width < renderImage->height)
	//{
	//	tmp = glHeight;
	//	glHeight = glWidth / videoAspect;
	//	if (glHeight > tmp)
	//	{
	//		glWidth  = glWidth*tmp/glHeight;
	//		glHeight = tmp;
	//	}
	//}
	//else
	//{
	//	tmp = glWidth;
	//	glWidth = glHeight * videoAspect;
	//	if (glWidth > tmp)
	//	{
	//		glHeight  = glHeight*tmp/glWidth;
	//		glWidth = tmp;
	//	}
	//}

	//Render tracking results for the first face and display frame
	//VisageRendering::DisplayResults(&trackingDataRender[0], trackingStatusRender[0], glWidth, glHeight, renderImage, DISPLAY_FRAME);
	//if (logo)
		//VisageRendering::DisplayLogo(logo, glWidth, glHeight);
	//Render tracking results for rest of the faces without rendering the frame
	//for (int i=0; i<MAX_FACES; i++)
	//{
		//if(trackingStatusRender[i] == TRACK_STAT_OK)
			//VisageRendering::DisplayResults(&trackingDataRender[i], trackingStatusRender[i], glWidth, glHeight, renderImage, DISPLAY_DEFAULT - DISPLAY_FRAME);
	//}

	return true;
}



/**
* Method for getting frame rate information from the tracker.
*
* @return float value of frame rate obtained from the tracker.
*/
float Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_GetFps(JNIEnv* env, jobject obj)
{
	return trackingData[0].frameRate;
}

/**
* Method for getting frame rate information from the tracker.
*
* @return float value of frame rate obtained from the tracker.
*/
int Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_GetTrackTime(JNIEnv* env, jobject obj)
{
	return trackingTime;
}


/**
* Method for getting the tracking status information from tracker.
*
* @return tracking status information as string.
*/
JNIEXPORT jstring JNICALL Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_GetStatus(JNIEnv* env, jobject obj)
{
	char* msg;

	for (int i=0; i<MAX_FACES; i++)
	{
		if (trackingStatusBuffer[i] == TRACK_STAT_OK)
			return env->NewStringUTF("OK");
	}

	for (int i=0; i<MAX_FACES; i++)
	{
		if (trackingStatusBuffer[i] == TRACK_STAT_RECOVERING)
			return env->NewStringUTF("RECOVERING");
	}

	for (int i=0; i<MAX_FACES; i++)
	{
		if (trackingStatusBuffer[i] == TRACK_STAT_INIT)
			return env->NewStringUTF("INITIALIZING");
	}

	return env->NewStringUTF("OFF");
}

float Java_com_visagetechnologies_visagetrackerdemo_TrackerRenderer_getTrackerFps( JNIEnv*  env )
{
	return m_fps;
}


float Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_GetDisplayFps(JNIEnv* env)
{
	return displayFramerate;
}


bool Java_com_visagetechnologies_visagetrackerdemo_TrackerView_IsAutoStopped(JNIEnv* env)
{
	//RESET VARIABLES IN WRAPPER
	if (m_Tracker)
   {

		for (int i=0; i<MAX_FACES; i++)
		{
			if (trackingStatus[i] != TRACK_STAT_OK)
				return true;
		}
		return false;

   }
   else
		return false;
}

}
