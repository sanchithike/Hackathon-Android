#include "AndroidImageCapture.h"
#include <android/log.h>

#define  LOG_TAG    "libandroid-opencv"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

namespace VisageSDK
{

AndroidImageCapture::AndroidImageCapture()
{
	pts = 0;
}

AndroidImageCapture::AndroidImageCapture(int width, int height, int format)
{
	int nChannels;
	switch (format) {
		case VISAGE_FRAMEGRABBER_FMT_RGB:
			// fall-through
		case VISAGE_FRAMEGRABBER_FMT_BGR:
			nChannels = 3;
			break;
		case VISAGE_FRAMEGRABBER_FMT_LUMINANCE:
			// fall-through
		default:
			nChannels = 1;
			break;
	}
	// Test Code starts
    sourceImageBuffer = vsCreateImage(vsSize(width, height), VS_DEPTH_8U,nChannels);
    destinationImageBuffer = vsCreateImage(vsSize(width, height), VS_DEPTH_8U,nChannels);
    sourcePts = 0;
	destinationPts = 0;
	// Test Code ends
	buffer = vsCreateImage(vsSize(width, height), VS_DEPTH_8U,nChannels);
	pts = 0;
	this->width = width;
	this->height = height;
	this->nChannels = nChannels;
}

AndroidImageCapture::~AndroidImageCapture(void)
{
	// cleaning up
	    vsReleaseImage(&buffer);
	    vsReleaseImage(&sourceImageBuffer);
	    vsReleaseImage(&destinationImageBuffer);
}
 
void AndroidImageCapture::WriteFrame(unsigned char* imageData, int width, int height)
{
	this->width = width;
	this->height = height;
	memcpy(buffer->imageData, imageData, buffer->imageSize);
}


void AndroidImageCapture::WriteSourceFrame(unsigned char* imageData, int width, int height){
    this->sourceWidth = width;
    this->sourceHeight = height;
    if (width != this->width || height != this->height){
        vsReleaseImage(&sourceImageBuffer);
        sourceImageBuffer = vsCreateImage(vsSize(width, height), VS_DEPTH_8U,this->nChannels);
    }
    memcpy(sourceImageBuffer->imageData, imageData, sourceImageBuffer->imageSize);
}


void AndroidImageCapture::WriteDestinationFrame(unsigned char* imageData, int width, int height){
    this->destinationWidth = width;
    this->destinationHeight = height;
    if (width != this->width || height != this->height){
        vsReleaseImage(&destinationImageBuffer);
        destinationImageBuffer = vsCreateImage(vsSize(width, height), VS_DEPTH_8U,this->nChannels);
    }
    memcpy(destinationImageBuffer->imageData, imageData, destinationImageBuffer->imageSize);
}

VsImage *AndroidImageCapture::GrabFrame(long &timeStamp)
{
	timeStamp = pts++;
	return buffer;
}

VsImage *AndroidImageCapture::GrabSourceFrame(long &timeStamp){
    timeStamp = sourcePts++;
    return sourceImageBuffer;
}

VsImage *AndroidImageCapture::GrabDestinationFrame(long &timeStamp){
    timeStamp = destinationPts++;
    return destinationImageBuffer;
}
}
