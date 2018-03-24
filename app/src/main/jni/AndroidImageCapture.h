
#ifndef __AndroidImageCapture_h__
#define __AndroidImageCapture_h__

#include <pthread.h>
#include <cerrno>
#include "WrapperOpenCV.h"

#define VISAGE_FRAMEGRABBER_FMT_RGB 0
#define VISAGE_FRAMEGRABBER_FMT_BGR 1 
#define VISAGE_FRAMEGRABBER_FMT_LUMINANCE 2
#define VISAGE_FRAMEGRABBER_FMT_RGBA 3
#define VISAGE_FRAMEGRABBER_FMT_BGRA 4

#define VISAGE_FRAMEGRABBER_ORIGIN_TL 0
#define VISAGE_FRAMEGRABBER_ORIGIN_BL 1


namespace VisageSDK
{

/** AndroidImageCapture is class that implements a simple frame grabber that serves a single image to the tracker.
 * The image data is passed by calling @ref WriteFrame method.
 */
class AndroidImageCapture {
		
public:
		
	/** Constructor.
	 *	
	 */
	AndroidImageCapture();
	
	/** Constructor.
	* 
	* @param width width of image
	* @param height height of image
	* @param format format of image
	*/
	AndroidImageCapture(int width, int height, int format=VISAGE_FRAMEGRABBER_FMT_LUMINANCE);

	/** Destructor.
	 *	
	 */
	~AndroidImageCapture(void);

	/**
	 * 
	 * This function is called to get the frame to process.
	 */
	VsImage *GrabFrame(long &timeStamp);
	
	/**
	* Method for writing imageData to buffer object.
	* @param imageData raw pixel data of image used for tracking
	* @param width width of the frame
	* @param height height of the frame
	*/
	void WriteFrame(unsigned char *imageData, int width, int height);

	// Test Code starts
	void WriteSourceFrame(unsigned char *imageData, int width, int height);
	void WriteDestinationFrame(unsigned char *imageData, int width, int height);
    VsImage *GrabSourceFrame(long &timeStamp);
    VsImage *GrabDestinationFrame(long &timeStamp);
	// Test Code ends


private:

	VsImage* buffer;
	VsImage* sourceImageBuffer;
	VsImage* destinationImageBuffer;
	int pts, sourcePts, destinationPts;
	int width, height,nChannels;
	int sourceWidth, sourceHeight, destinationWidth, destinationHeight;
};

}
	
#endif // __AndroidImageCapture_h__
