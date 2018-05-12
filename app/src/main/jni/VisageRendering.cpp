///////////////////////////////////////////////////////////////////////////////
// 
// (c) Visage Technologies AB 2002 - 2016  All rights reserved. 
// 
// This file is part of visage|SDK(tm). 
// Unauthorized copying of this file, via any medium is strictly prohibited. 
// 
// No warranty, explicit or implicit, provided. 
// 
// This is proprietary software. No part of this software may be used or 
// reproduced in any form or by any means otherwise than in accordance with
// any written license granted by Visage Technologies AB. 
// 
/////////////////////////////////////////////////////////////////////////////

#include "VisageRendering.h"
#include "MathMacros.h"
#include <android/log.h>
#include <EGL/egl.h>
#include <GLES/gl.h>
#define  LOG_TAG    "TrackerWrapper"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

namespace VisageSDK
{

#if defined (IOS) || defined(ANDROID)
void gluLookAt(GLfloat eyex, GLfloat eyey, GLfloat eyez,
				GLfloat centerx, GLfloat centery, GLfloat centerz,
				GLfloat upx, GLfloat upy, GLfloat upz)
{
	GLfloat m[16];
	GLfloat x[3], y[3], z[3];
	GLfloat mag;
	GLfloat invmag;

	/* Make rotation matrix */

	/* Z vector */
	z[0] = eyex - centerx;
	z[1] = eyey - centery;
	z[2] = eyez - centerz;
	mag = sqrt(z[0] * z[0] + z[1] * z[1] + z[2] * z[2]);
	if (mag) {			/* mpichler, 19950515 */
		invmag = 1.0f / mag;
		z[0] *= invmag;
		z[1] *= invmag;
		z[2] *= invmag;
	}

	/* Y vector */
	y[0] = upx;
	y[1] = upy;
	y[2] = upz;

	/* X vector = Y cross Z */
	x[0] = y[1] * z[2] - y[2] * z[1];
	x[1] = -y[0] * z[2] + y[2] * z[0];
	x[2] = y[0] * z[1] - y[1] * z[0];

	/* Recompute Y = Z cross X */
	y[0] = z[1] * x[2] - z[2] * x[1];
	y[1] = -z[0] * x[2] + z[2] * x[0];
	y[2] = z[0] * x[1] - z[1] * x[0];

	/* mpichler, 19950515 */
	/* cross product gives area of parallelogram, which is < 1.0 for
	* non-perpendicular unit-length vectors; so normalize x, y here
	*/

	mag = sqrt(x[0] * x[0] + x[1] * x[1] + x[2] * x[2]);
	if (mag) {
		invmag = 1.0f / mag;
		x[0] *= invmag;
		x[1] *= invmag;
		x[2] *= invmag;
	}

	mag = sqrt(y[0] * y[0] + y[1] * y[1] + y[2] * y[2]);
	if (mag) {
		invmag = 1.0f / mag;
		y[0] *= invmag;
		y[1] *= invmag;
		y[2] *= invmag;
	}

#define M(row,col)	m[col*4+row]
	M(0, 0) = x[0];
	M(0, 1) = x[1];
	M(0, 2) = x[2];
	M(0, 3) = 0.0;
	M(1, 0) = y[0];
	M(1, 1) = y[1];
	M(1, 2) = y[2];
	M(1, 3) = 0.0;
	M(2, 0) = z[0];
	M(2, 1) = z[1];
	M(2, 2) = z[2];
	M(2, 3) = 0.0;
	M(3, 0) = 0.0;
	M(3, 1) = 0.0;
	M(3, 2) = 0.0;
	M(3, 3) = 1.0;
#undef M
	glMultMatrixf(m);

	/* Translate Eye to Origin */
	glTranslatef(-eyex, -eyey, -eyez);
}

#endif

static GLuint frame_tex_id = 0;
static GLuint logo_tex_id = -1;
static float tex_x_coord  = 0;
static float tex_y_coord  = 0;
static bool video_texture_inited = false;
static int video_texture_width	= 0;
static int video_texture_height = 0;
static int numberOfVertices = 0;

static GLuint destination_frame_tex_id = 2;
static GLuint source_frame_tex_id = 3;
static bool source_tex_initiated = false;
static bool destination_tex_initiated = false;

static std::vector<GLushort> output;

typedef struct CubicPoly
{
	float c0, c1, c2, c3;
	float eval(float t)
	{
		float t2 = t*t;
		float t3 = t2 * t;
		return c0 + c1*t + c2*t2 + c3*t3;
	}
} CubicPoly;

typedef struct Vec2D
{
	Vec2D(float _x, float _y) : x(_x), y(_y) {}
	float x, y;
} Vec2D;

static void InitCubicPoly(float x0, float x1, float t0, float t1, CubicPoly &p)
{
	p.c0 = x0;
	p.c1 = t0;
	p.c2 = -3*x0 + 3*x1 - 2*t0 - t1;
	p.c3 = 2*x0 - 2*x1 + t0 + t1;
}

static void InitCatmullRom(float x0, float x1, float x2, float x3, CubicPoly &p)
{
	InitCubicPoly(x1, x2, 0.5f*(x2-x0), 0.5f*(x3-x1), p);
}

static void InitNonuniformCatmullRom(float x0, float x1, float x2, float x3, float dt0, float dt1, float dt2, CubicPoly &p)
{
	float t1 = (x1 - x0) / dt0 - (x2 - x0) / (dt0 + dt1) + (x2 - x1) / dt1;
	float t2 = (x2 - x1) / dt1 - (x3 - x1) / (dt1 + dt2) + (x3 - x2) / dt2;

	t1 *= dt1;
	t2 *= dt1;

	InitCubicPoly(x1, x2, t1, t2, p);
}

static float VecDistSquared(const Vec2D& p, const Vec2D& q)
{
	float dx = q.x - p.x;
	float dy = q.y - p.y;
	return dx*dx + dy*dy;
}

static void InitCentripetalCR(const Vec2D& p0, const Vec2D& p1, const Vec2D& p2, const Vec2D& p3, CubicPoly &px, CubicPoly &py)
{
	float dt0 = powf(VecDistSquared(p0, p1), 0.25f);
	float dt1 = powf(VecDistSquared(p1, p2), 0.25f);
	float dt2 = powf(VecDistSquared(p2, p3), 0.25f);

	if (dt1 < 1e-4f)	dt1 = 1.0f;
	if (dt0 < 1e-4f)	dt0 = dt1;
	if (dt2 < 1e-4f)	dt2 = dt1;

	InitNonuniformCatmullRom(p0.x, p1.x, p2.x, p3.x, dt0, dt1, dt2, px);
	InitNonuniformCatmullRom(p0.y, p1.y, p2.y, p3.y, dt0, dt1, dt2, py);
}

static void SetupCamera(int width, int height, float f)
{
	GLfloat x_offset = 1;
	GLfloat y_offset = 1;
	if (width > height)
		x_offset = ((GLfloat)width)/((GLfloat)height);
	else if (width < height)
		y_offset = ((GLfloat)height)/((GLfloat)width);

	//Note:
	// FOV in radians is: fov*0.5 = arctan ((top-bottom)*0.5 / near)
	// In this case: FOV = 2 * arctan(frustum_y / frustum_near)
	//set frustum specs
	GLfloat frustum_near = 0.001f;
	GLfloat frustum_far = 30; //hard to estimate face too far away
	GLfloat frustum_x = x_offset*frustum_near/f;
	GLfloat frustum_y = y_offset*frustum_near/f;
	//set frustum
	glMatrixMode(GL_PROJECTION);
	glLoadIdentity();
	#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
	glFrustum(-frustum_x,frustum_x,-frustum_y,frustum_y,frustum_near,frustum_far);
	#else
	glFrustumf(-frustum_x,frustum_x,-frustum_y,frustum_y,frustum_near,frustum_far);
	#endif
	glMatrixMode(GL_MODELVIEW);
	//clear matrix
	glLoadIdentity();
	//camera in (0,0,0) looking at (0,0,1) up vector (0,1,0)
	gluLookAt(0,0,0,0,0,1,0,1,0);

}





static void DrawFaceContourWithTexture(FaceData* trackingData, const VsImage *image, int width, int height){
    glPointSize(3);
    glLineWidth(3);
    //glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    //glEnable(GL_BLEND);
    glColor4ub(176,196,222,160);
    // Face Contour Points
    static int points[] = {
            12, 1,
            13,	1,
            13,	3,
            13,	5,
            13,	7,
            13,	9,
            13,	11,
            13,	13,
            13,	15,
            13,	17,
            13,	16,
            13,	14,
            13,	12,
            13,	10,
            13,	8,
            13,	6,
            13,	4,
            13,	2,
            11, 2,
            11, 5,
            11, 3,
            13, 1
    };

    vector<float> pointCoords;
    int n = 0;
    FDP *fdp = trackingData->featurePoints2D;

    for (int i = 0; i < 22; i++)
    {
        const FeaturePoint &fp = fdp->getFP(points[2*i],points[2*i+1]);
        if(fp.defined && fp.pos[0]!=0 && fp.pos[1]!=0)
        {
            pointCoords.push_back(fp.pos[0]);
            pointCoords.push_back(fp.pos[1]);
            n++;
        }
    }

    if(pointCoords.size() == 0 || n <= 2)
        return;

    int factor = 10;
    vector<float> pointsToDraw;
    VisageRendering::CalcSpline(pointCoords, factor, pointsToDraw);
    int nVert = (int)pointsToDraw.size() / 2;
    float *vertices = new float[nVert*3];
    float *textureCoordinates = new float[nVert * 2];
    int cnt = 0;
    float minX = 1.0;
    float maxX = 0.0;
    float minY = 1.0;
    float maxY = 0.0;
    for (int i = 0; i < nVert; ++i)
    {
        vertices[3*i] = pointsToDraw.at(cnt++);
        vertices[3*i+1] = pointsToDraw.at(cnt++);
        vertices[3*i+2] = 0.0f;
        textureCoordinates[2 * i ] = vertices[3*i+0] * tex_x_coord;
        textureCoordinates[2 * i + 1] = tex_x_coord - vertices[3*i+1] * tex_y_coord;
    }
    glEnable(GL_TEXTURE_2D);
    VisageRendering::enableTexture(image,width,height);
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);


    // Test Code starts here

    glEnableClientState(GL_VERTEX_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, vertices);
    glTexCoordPointer(2, GL_FLOAT, 0, textureCoordinates);
    glColor4f(0.1f, 1.0f, 1.0f, 1.0f);      // Set the current color (NEW)
    glViewport(0, 0, width, height);
    glDrawArrays(GL_TRIANGLE_FAN, 0, nVert);
    glDisableClientState(GL_VERTEX_ARRAY);
    // Test code ends

    /*
    glEnableClientState(GL_VERTEX_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, vertices);
    //glTexCoordPointer(2, GL_FLOAT, 0, textureCoordinates);
    glColor4f(0.1f, 1.0f, 1.0f, 1.0f);      // Set the current color (NEW)
    glDrawArrays(GL_TRIANGLE_FAN, 0, nVert);
    glDisableClientState(GL_VERTEX_ARRAY);

    */
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    glDisable(GL_TEXTURE_2D);
    //glBindTexture(GL_TEXTURE_2D, -1);
    //clean-up
    delete[] vertices;
    delete[] textureCoordinates;
}

static void DrawSpline2D(int *points, int num, FaceData* trackingData)
{
	if (num < 2)
		return;

	vector<float> pointCoords;
	int n = 0;
	FDP *fdp = trackingData->featurePoints2D;

	for (int i = 0; i < num; i++)
	{
		const FeaturePoint &fp = fdp->getFP(points[2*i],points[2*i+1]);
		if(fp.defined && fp.pos[0]!=0 && fp.pos[1]!=0)
		{
			pointCoords.push_back(fp.pos[0]);
			pointCoords.push_back(fp.pos[1]);
			n++;
		}
	}

	if(pointCoords.size() == 0 || n <= 2)
		return;

	int factor = 10;
	vector<float> pointsToDraw;
	VisageRendering::CalcSpline(pointCoords, factor, pointsToDraw);
	int nVert = (int)pointsToDraw.size() / 2;
	float *vertices = new float[nVert*3];
	int cnt = 0;
	for (int i = 0; i < nVert; ++i)
	{
		vertices[3*i+0] = pointsToDraw.at(cnt++);
		vertices[3*i+1] = pointsToDraw.at(cnt++);
		vertices[3*i+2] = 0.0f;
	}
	glEnableClientState(GL_VERTEX_ARRAY);
	glVertexPointer(3, GL_FLOAT, 0, vertices);
	glDrawArrays(GL_LINE_STRIP, 0, nVert);
	glDisableClientState(GL_VERTEX_ARRAY);

	//clean-up
	delete[] vertices;
}

static void DrawElipse(float x, float y, float radiusX, float radiusY)
{

#ifdef WIN32
	glMatrixMode(GL_MODELVIEW);
	glPushMatrix();
	glLoadIdentity();

	glTranslatef(x, y, 0.0f);
	static const int circle_points = 100;
	static const float angle = 2.0f * 3.1416f / circle_points;

	glBegin(GL_POLYGON);
	double angle1=0.0;
	glVertex2d(radiusX * cos(0.0) , radiusY * sin(0.0));
	for (int i=0; i<circle_points; i++)
	{
		glVertex2d(radiusX * cos(angle1), radiusY * sin(angle1));
		angle1 += angle;
	}
	glEnd();
	glPopMatrix();
#endif

}

static void DrawPoints2D(int *points, int num, bool singleColor, FaceData* trackingData, VsImage* frame, bool drawQuality = true)
{

#ifdef IOS
	float radius = (trackingData->faceScale / (float)frame->width)*10;
#elif MAC_OS_X
	float radius = (trackingData->faceScale / (float)frame->width)*20;
#else
	float radius = (trackingData->faceScale / (float)frame->width)*30;
#endif

	float radiusX = (trackingData->faceScale / (float)frame->width) * 0.017f;
	float radiusY = (trackingData->faceScale / (float)frame->height) * 0.017f;

	FDP *fdp = trackingData->featurePoints2D;

#ifdef WIN32
	for (int i = 0; i < num; i++)
	{
		const FeaturePoint &fp =  fdp->getFP(points[2*i],points[2*i+1]);

		if(fp.defined && fp.pos[0]!=0 && fp.pos[1]!=0)
		{
			float x = fp.pos[0];
			float y = fp.pos[1];

			glColor4ub(0,0,0,255);
			DrawElipse(x, y, radiusX, radiusY);

			if(!singleColor)
			{
				if (fp.quality >= 0 && drawQuality)
					glColor4ub((1 - fp.quality) * 255, fp.quality * 255, 0, 255);
				else
					glColor4ub(0, 255, 255, 255);

				DrawElipse(x, y, 0.6f * radiusX, 0.6f * radiusY);
			}
		}
	}
#else
	float *vertices = new float[num*2];
	int n = 0;
	for (int i = 0; i < num; i++)
	{
		const FeaturePoint &fp =  fdp->getFP(points[2*i],points[2*i+1]);
		if(fp.defined && fp.pos[0]!=0 && fp.pos[1]!=0)
		{
			vertices[2*n+0] = fp.pos[0];
			vertices[2*n+1] = fp.pos[1];
			n++;
		}
		
		glEnable(GL_POINT_SMOOTH);
		glEnableClientState(GL_VERTEX_ARRAY);
		glVertexPointer(2, GL_FLOAT, 0, vertices);
		glPointSize(radius);
		glColor4ub(0,0,0,255);
		glDrawArrays(GL_POINTS, 0, n);
		if (!singleColor)
		{
			glPointSize(0.8f*radius);
			if (fp.quality >= 0 && drawQuality)
				glColor4ub((1 - fp.quality) * 255, fp.quality * 255, 0, 255);
			else
				glColor4ub(0,255,255,255);
			glDrawArrays(GL_POINTS, 0, n);
		}
		glDisableClientState(GL_VERTEX_ARRAY);
	}
	
	//clean-up
	delete[] vertices;

#endif
}

static void DrawPoints3D(int *points, int num, bool singleColor, FaceData* trackingData, VsImage* frame, bool relative)
{

#ifdef IOS
	float radius = (trackingData->faceScale / (float)frame->width)*10;
#elif MAC_OS_X
	float radius = (trackingData->faceScale / (float)frame->width)*20;
#else
	float radius = (trackingData->faceScale / (float)frame->width)*30;
#endif

	FDP *fdp = relative ? trackingData->featurePoints3DRelative : trackingData->featurePoints3D;

	float *vertices = new float[num*3];
	int n = 0;
	for (int i = 0; i < num; i++)
	{
		const FeaturePoint &fp =  fdp->getFP(points[2*i],points[2*i+1]);
		if(fp.defined && fp.pos[0]!=0 && fp.pos[1]!=0)
		{
			vertices[3*n+0] = fp.pos[0];
			vertices[3*n+1] = fp.pos[1];
			vertices[3*n+2] = fp.pos[2];
			n++;
		}
	}

	glEnable(GL_POINT_SMOOTH);
	glEnableClientState(GL_VERTEX_ARRAY);
	glVertexPointer(3, GL_FLOAT, 0, vertices);
	glPointSize(radius);
	glColor4ub(0,0,0,255);
	glDrawArrays(GL_POINTS, 0, n);
	if (!singleColor)
	{
		glPointSize(0.8f*radius);
		glColor4ub(0,255,255,255);
		glDrawArrays(GL_POINTS, 0, n);
	}
	glDisableClientState(GL_VERTEX_ARRAY);

	//clean-up
	delete[] vertices;
}

static int NearestPow2(int n)
{
	unsigned int v; // compute the next highest power of 2 of 32-bit v

	v = n;
	v--;
	v |= v >> 1;
	v |= v >> 2;
	v |= v >> 4;
	v |= v >> 8;
	v |= v >> 16;
	v++;

	return v;
}

static void InitFrameTex(int x_size, int y_size, const VsImage *image)
{
	//Create The Texture
	glGenTextures(1, &frame_tex_id);

	//Typical Texture Generation Using Data From The Bitmap
	glBindTexture(GL_TEXTURE_2D, frame_tex_id);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

	//Creating pow2 texture
	#ifdef IOS
	switch (image->nChannels) {
		case 1:
			glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, x_size, y_size, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, 0);
			break;
		case 3:
		case 4:
		default:
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, x_size, y_size, 0, GL_BGRA, GL_UNSIGNED_BYTE, 0);
			break;
	}
	#elif ANDROID
	switch (image->nChannels)
		{
			case 1:
				glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, x_size, y_size, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, 0);
				break;
			case 3:
			case 4:
			default:
				glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, x_size, y_size, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
				break;
		}
	#else
	glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, x_size, y_size, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, 0);
	#endif
    //LOGI("%s - %d - %d","Image Width and height",image->width,image->height);
    //LOGI("%s - %d - %d","Nearest Power of two Width and height",x_size,y_size);
	tex_x_coord = (float) image->width / (float) x_size;
	tex_y_coord = (float) image->height / (float) y_size;
}
	
void VisageRendering::DisplayLogo(const VsImage *logo, int width, int height)
{
	//Create the texture if not inited
	if(logo_tex_id == -1)
	{
		glGenTextures(1, &logo_tex_id);
	
		//Bind the newly created texture
		glBindTexture(GL_TEXTURE_2D, logo_tex_id);
		
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	
		//Creating texture
#ifdef IOS
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, logo->width, logo->height, 0, GL_BGRA, GL_UNSIGNED_BYTE, logo->imageData);
#elif ANDROID
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, logo->width, logo->height, 0, GL_RGBA, GL_UNSIGNED_BYTE, logo->imageData);
#else
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, logo->width, logo->height, 0, GL_BGRA_EXT, GL_UNSIGNED_BYTE, logo->imageData);
#endif
	}
	
	 glBindTexture(GL_TEXTURE_2D, logo_tex_id);
	
#if defined(WIN32) || defined(LINUX)
	glPushAttrib(GL_DEPTH_BUFFER_BIT | GL_VIEWPORT_BIT | GL_ENABLE_BIT | GL_FOG_BIT | GL_STENCIL_BUFFER_BIT | GL_TRANSFORM_BIT | GL_TEXTURE_BIT );
#endif
	
	glDisable(GL_ALPHA_TEST);
	glDisable(GL_DEPTH_TEST);
	glDisable(GL_DITHER);
	glDisable(GL_FOG);
	glDisable(GL_SCISSOR_TEST);
	glDisable(GL_STENCIL_TEST);
	glDisable(GL_LIGHTING);
	glDisable(GL_LIGHT0);
	
	//transparency
	glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	glEnable(GL_BLEND);
	
	//use texture
	glEnable(GL_TEXTURE_2D);
	
	glMatrixMode(GL_MODELVIEW);
	glPushMatrix();
	glLoadIdentity();
	
	glMatrixMode(GL_PROJECTION);
	glPushMatrix();
	glLoadIdentity();
	
#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
	glOrtho(0.0f,1.0f,0.0f,1.0f,-10.0f,10.0f);
#else
	glOrthof(0.0f,1.0f,0.0f,1.0f,-10.0f,10.0f);
#endif
	
	//logo aspect
	float logoAspect = logo->width / (float)logo->height;
	//viewport aspect
	float viewportAspect = width / (float)height;
	//set logo position to upper right corner, maintain logo aspect relative
	float x = 0.75f;
	float y = 1 - ((1 - x) * viewportAspect / logoAspect);

	GLfloat vertices[] = {
		x,y,-5.0f,
		1.0f,y,-5.0f,
		x,1.0f,-5.0f,
		1.0f,1.0f,-5.0f,
	};
	
	//tex coords are flipped upside down instead of an image
	GLfloat texcoords[] = {
		0.0f,1.0f,
		1.0f,1.0f,
		0.0f,0.0f,
		1.0f,0.0f,
	};
	
	glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
	
	glEnableClientState(GL_TEXTURE_COORD_ARRAY);
	glEnableClientState(GL_VERTEX_ARRAY);
	
	glVertexPointer(3, GL_FLOAT, 0, vertices);
	glTexCoordPointer(2, GL_FLOAT, 0, texcoords);
	
	glViewport(0, 0, width, height);
	
	//drawing vertices and texcoords
	glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
	
	glDisableClientState(GL_TEXTURE_COORD_ARRAY);
	glDisableClientState(GL_VERTEX_ARRAY);
	
	glDisable(GL_TEXTURE_2D);
	
	glPopMatrix();

	glMatrixMode(GL_MODELVIEW);
	glPopMatrix();
	
	//disable logo texture
	glBindTexture(GL_TEXTURE_2D, 0);
	
#if defined(WIN32) || defined(LINUX)
	glPopAttrib();
#endif
	
	//glClear(GL_DEPTH_BUFFER_BIT);
}


void VisageRendering::ClearGL()
{
	glClearColor(0,0,0,0);
	glClear(GL_COLOR_BUFFER_BIT);
}

void VisageRendering::DisplayDestinationImage(const VsImage *image, int width, int height){
    VisageRendering::SetCamera();
    if(!destination_tex_initiated){
        VisageRendering::loadDestinationTexture(image);
    }
    glBindTexture(GL_TEXTURE_2D, destination_frame_tex_id);
    switch (image->nChannels) {
        case 3:
            #if defined (IOS) || defined (ANDROID)
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_RGB, GL_UNSIGNED_BYTE, image->imageData);
            #else
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_BGR, GL_UNSIGNED_BYTE, image->imageData);
            #endif
            break;
        case 4:
            #if defined(IOS) || defined(MAC_OS_X)
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_BGRA, GL_UNSIGNED_BYTE, image->imageData);
            #else
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_RGBA, GL_UNSIGNED_BYTE, image->imageData);
            #endif
            break;
        case 1:
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_LUMINANCE, GL_UNSIGNED_BYTE, image->imageData);
            break;
        default:
            return;
    }

    static GLfloat vertices[] = {
        0.0f,0.0f,-5.0f,
        1.0f,0.0f,-5.0f,
        0.0f,1.0f,-5.0f,
        1.0f,1.0f,-5.0f,
    };
    float* uvBounds = new float[2];
    getTextureUnwrapDimensions(image,uvBounds);
    // tex coords are flipped upside down instead of an image
    GLfloat texcoords[] = {
        0.0f,			uvBounds[1],
        uvBounds[0],	uvBounds[1],
        0.0f,			0.0f,
        uvBounds[0],	0.0f,
    };
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    glEnableClientState(GL_VERTEX_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, vertices);
    glTexCoordPointer(2, GL_FLOAT, 0, texcoords);
    glViewport(0, 0, width, height);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    glDisable(GL_TEXTURE_2D);
    glBindTexture(GL_TEXTURE_2D, -1);
}

void VisageRendering::DisplaySourceFace(const VsImage *image, int width, int height, FaceData* sourceTrackingData, FaceData* destinationTrackingData, int* color){
    glEnable(GL_TEXTURE_2D);
    if(!source_tex_initiated){
        VisageRendering::loadSourceTexture(image);
    }
    glBindTexture(GL_TEXTURE_2D, source_frame_tex_id);
    switch (image->nChannels) {
        case 3:
            #if defined (IOS) || defined (ANDROID)
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_RGB, GL_UNSIGNED_BYTE, image->imageData);
            #else
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_BGR, GL_UNSIGNED_BYTE, image->imageData);
            #endif
            break;
        case 4:
            #if defined(IOS) || defined(MAC_OS_X)
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_BGRA, GL_UNSIGNED_BYTE, image->imageData);
            #else
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_RGBA, GL_UNSIGNED_BYTE, image->imageData);
            #endif
            break;
        case 1:
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_LUMINANCE, GL_UNSIGNED_BYTE, image->imageData);
            break;
        default:
            return;
    }
    float* uvBounds = new float[2];
    getTextureUnwrapDimensions(image,uvBounds);
    int numVertices = getVerticesLength(destinationTrackingData);
    float* vertices = new float[numVertices * 3];
    getVertexArray(destinationTrackingData,vertices);
    float* textureCoordinates = new float[numVertices * 2];
    getTextureCoordinateArray(sourceTrackingData,uvBounds,textureCoordinates);
    //glEnable(GL_TEXTURE_2D);
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    glEnableClientState(GL_VERTEX_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, vertices);
    glTexCoordPointer(2, GL_FLOAT, 0, textureCoordinates);
    glViewport(0, 0, width, height);
    //glColor4ub(color[0],color[1],color[2],255);
    glDrawArrays(GL_TRIANGLE_FAN, 0, numVertices);
    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    glDisable(GL_TEXTURE_2D);
    glPopMatrix();
    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();
    glBindTexture(GL_TEXTURE_2D, -1);
}

void VisageRendering::loadSourceTexture(const VsImage *image){
    glGenTextures(1, &source_frame_tex_id);
    glBindTexture(GL_TEXTURE_2D, source_frame_tex_id);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, NearestPow2(image->width), NearestPow2(image->height), 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
    glBindTexture(GL_TEXTURE_2D, source_frame_tex_id);
}

void VisageRendering::SetCamera(){
    VisageRendering::ClearGL();
    glDisable(GL_ALPHA_TEST);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_BLEND );
    glDisable(GL_DITHER);
    glDisable(GL_FOG);
    glDisable(GL_SCISSOR_TEST);
    glDisable(GL_STENCIL_TEST);
    glDisable(GL_LIGHTING);
    glDisable(GL_LIGHT0);

    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();

    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();

    #if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
    glOrtho(0.0f,1.0f,0.0f,1.0f,-10.0f,10.0f);
    #else
    glOrthof(0.0f,1.0f,0.0f,1.0f,-10.0f,10.0f);
    #endif

    glEnable(GL_TEXTURE_2D);
    //glColor4ub(255,255,255,255);
}

void VisageRendering::loadDestinationTexture(const VsImage *image){
    glGenTextures(1, &destination_frame_tex_id);
    glBindTexture(GL_TEXTURE_2D, destination_frame_tex_id);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, NearestPow2(image->width), NearestPow2(image->height), 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
    glBindTexture(GL_TEXTURE_2D, destination_frame_tex_id);
}


 void VisageRendering::getTextureCoordinateArray(FaceData* trackingData, float* unwrapDimensions, float* textureCoordinates){
    static int points[] = {
            12, 1,
            13,	1,
            13,	3,
            13,	5,
            13,	7,
            13,	9,
            13,	11,
            13,	13,
            13,	15,
            13,	17,
            13,	16,
            13,	14,
            13,	12,
            13,	10,
            13,	8,
            13,	6,
            13,	4,
            13,	2,
            11, 2,
            11, 5,
            11, 3,
            13, 1
    };

    vector<float> pointCoords;
    int n = 0;
    FDP *fdp = trackingData->featurePoints2D;

    for (int i = 0; i < 22; i++)
    {
        const FeaturePoint &fp = fdp->getFP(points[2*i],points[2*i+1]);
        if(fp.defined && fp.pos[0]!=0 && fp.pos[1]!=0)
        {
            pointCoords.push_back(fp.pos[0]);
            pointCoords.push_back(fp.pos[1]);
            n++;
        }
    }

    if(pointCoords.size() == 0 || n <= 2)
        return ;

    int factor = 10;
    vector<float> pointsToDraw;
    VisageRendering::CalcSpline(pointCoords, factor, pointsToDraw);
    int nVert = (int)pointsToDraw.size() / 2;
    int cnt = 0;
    for (int i = 0; i < nVert; ++i)
    {
        textureCoordinates[2 * i ] = pointsToDraw.at(2*i+0);
        textureCoordinates[2 * i + 1] = 1.0f - pointsToDraw.at(2*i+1);
    }
}

void VisageRendering::calculateViewportDimensions(VsImage *renderImage, int width, int height, int *dimensions){
    int glWidth = width;
    int glHeight = height;
    float aspectRatio = renderImage->width / (float) renderImage->height;
    float tmp;
    if(renderImage->width < renderImage->height)
    {
        tmp = glHeight;
        glHeight = glWidth / aspectRatio;
        if (glHeight > tmp)
        {
            glWidth  = glWidth*tmp/glHeight;
            glHeight = tmp;
        }
    }
    else
    {
        tmp = glWidth;
        glWidth = glHeight * aspectRatio;
        if (glWidth > tmp)
        {
            glHeight  = glHeight*tmp/glWidth;
            glWidth = tmp;
        }
    }
    dimensions[0] = glWidth;
    dimensions[1] = glHeight;
}

int VisageRendering::getVerticesLength(FaceData *trackingData){
    static int points[] = {
                12, 1,
                13,	1,
                13,	3,
                13,	5,
                13,	7,
                13,	9,
                13,	11,
                13,	13,
                13,	15,
                13,	17,
                13,	16,
                13,	14,
                13,	12,
                13,	10,
                13,	8,
                13,	6,
                13,	4,
                13,	2,
                11, 2,
                11, 5,
                11, 3,
                13, 1
        };

        vector<float> pointCoords;
        int n = 0;
        FDP *fdp = trackingData->featurePoints2D;

        for (int i = 0; i < 22; i++)
        {
            const FeaturePoint &fp = fdp->getFP(points[2*i],points[2*i+1]);
            if(fp.defined && fp.pos[0]!=0 && fp.pos[1]!=0)
            {
                pointCoords.push_back(fp.pos[0]);
                pointCoords.push_back(fp.pos[1]);
                n++;
            }
        }

        if(pointCoords.size() == 0 || n <= 2)
            return NULL;

        int factor = 10;
        vector<float> pointsToDraw;
        VisageRendering::CalcSpline(pointCoords, factor, pointsToDraw);
        int nVert = (int)pointsToDraw.size() / 2;
        return nVert;
}

void VisageRendering::getEyeTriangles(int* eyeTriangles){
    eyeTriangles[0] = 1;
    eyeTriangles[1] = 7;
    eyeTriangles[2] = 0;
    eyeTriangles[3] = 2;
    eyeTriangles[4] = 3;
    eyeTriangles[5] = 1;
    eyeTriangles[6] = 3;
    eyeTriangles[7] = 4;
    eyeTriangles[8] = 1;
    eyeTriangles[9] = 4;
    eyeTriangles[10] = 5;
    eyeTriangles[11] = 1;
    eyeTriangles[12] = 5;
    eyeTriangles[13] = 6;
    eyeTriangles[14] = 1;
    eyeTriangles[15] = 6;
    eyeTriangles[16] = 7;
    eyeTriangles[17] = 1;
}


void VisageRendering::getLeftEyeSpline3D(FaceData *trackingData, vector<float>& points){
    static int leftEye[] = {
            3,14,
            12,10,
            3,12,
            12,12,
            3,10,
            12,8,
            3,8,
            12,6
        };

        int n = 0;
        FDP *fdp = trackingData->featurePoints3DRelative;

        for (int i = 0; i < 8; i++)
        {
            const FeaturePoint &fp = fdp->getFP(leftEye[2*i],leftEye[2*i+1]);
            if(fp.defined && fp.pos[0]!=0 && fp.pos[1]!=0)
            {
                points.push_back(fp.pos[0]);
                points.push_back(fp.pos[1]);
                points.push_back(fp.pos[2]);
                n++;
            }
        }

        if(points.size() == 0 || n <= 2)
            return ;
}

void VisageRendering::getLeftEyeSpline2D(FaceData *trackingData, vector<float>& points){
    static int leftEye[] = {
        3,14,
        12,10,
        3,12,
        12,12,
        3,10,
        12,8,
        3,8,
        12,6
    };

    int n = 0;
    FDP *fdp = trackingData->featurePoints2D;

    for (int i = 0; i < 8; i++)
    {
        const FeaturePoint &fp = fdp->getFP(leftEye[2*i],leftEye[2*i+1]);
        if(fp.defined && fp.pos[0]!=0 && fp.pos[1]!=0)
        {
            points.push_back(fp.pos[0]);
            points.push_back(fp.pos[1]);
            n++;
        }
    }

    if(points.size() == 0 || n <= 2)
        return ;

    //int factor = 1;
    //VisageRendering::CalcSpline(pointCoords, factor, points);
}

void VisageRendering::getRightEyeSpline3D(FaceData *trackingData, vector<float>& points){
    static int rightEye[] = {
            3,13,
            12,9,
            3,11,
            12,11,
            3,9,
            12,7,
            3,7,
            12,5
        };

        int n = 0;
        FDP *fdp = trackingData->featurePoints3DRelative;
        for (int i = 0; i < 8; i++)
        {
            const FeaturePoint &fp = fdp->getFP(rightEye[2*i],rightEye[2*i+1]);
            if(fp.defined && fp.pos[0]!=0 && fp.pos[1]!=0)
            {
                points.push_back(fp.pos[0]);
                points.push_back(fp.pos[1]);
                points.push_back(fp.pos[2]);
                n++;
            }
        }

        if(points.size() == 0 || n <= 2)
            return ;

        //int factor = 1;
        //VisageRendering::CalcSpline(pointCoords, factor, points);
        LOGI("%s - %d","3d spline points are ",points.size());
}

void VisageRendering::getRightEyeSpline2D(FaceData *trackingData, vector<float>& points){
    static int rightEye[] = {
            3,13,
            12,9,
            3,11,
            12,11,
            3,9,
            12,7,
            3,7,
            12,5
        };

        int n = 0;
        FDP *fdp = trackingData->featurePoints2D;
        for (int i = 0; i < 8; i++)
        {
            const FeaturePoint &fp = fdp->getFP(rightEye[2*i],rightEye[2*i+1]);
            if(fp.defined && fp.pos[0]!=0 && fp.pos[1]!=0)
            {
                points.push_back(fp.pos[0]);
                points.push_back(fp.pos[1]);
                n++;
            }
        }

        if(points.size() == 0 || n <= 2)
            return ;

        //int factor = 1;
        //VisageRendering::CalcSpline(pointCoords, factor, points);
        LOGI("%s - %d","2d spline points are ",points.size());
}

void VisageRendering::getLeftEyeModel(vector<float>& points, float *eyeModel){
    int nVert = (int)points.size() / 3;
    int cnt = 0;
    for (int i = 0; i < nVert; ++i)
    {
        eyeModel[3*i] = points.at(cnt++);
        eyeModel[3*i+1] = points.at(cnt++);
        eyeModel[3*i+2] = points.at(cnt++);
    }
}

void VisageRendering::getRightEyeModel(vector<float>& points, float *eyeModel){
    int nVert = (int)points.size() / 3;
    int cnt = 0;
    for (int i = 0; i < nVert; ++i)
    {
        eyeModel[3*i] = points.at(cnt++);
        eyeModel[3*i+1] = points.at(cnt++);
        eyeModel[3*i+2] = points.at(cnt++);
    }
}

void VisageRendering::getLeftEyeTexture(vector<float>& points, float* textureCoordinates){
    int nVert = (int)points.size() / 2;
    for (int i = 0; i < nVert; ++i)
    {
        textureCoordinates[2 * i ] = points.at(2*i+0);
        textureCoordinates[2 * i + 1] = 1.0f - points.at(2*i+1);
    }
}

void VisageRendering::getRightEyeTexture(vector<float>& points, float* textureCoordinates){
    int nVert = (int)points.size() / 2;
    for (int i = 0; i < nVert; ++i)
    {
        textureCoordinates[2 * i ] = points.at(2*i+0);
        textureCoordinates[2 * i + 1] = 1.0f - points.at(2*i+1);
    }
}


void VisageRendering::getVertexArray(FaceData *trackingData, float *vertexCoordinates){
    static int points[] = {
            12, 1,
            13,	1,
            13,	3,
            13,	5,
            13,	7,
            13,	9,
            13,	11,
            13,	13,
            13,	15,
            13,	17,
            13,	16,
            13,	14,
            13,	12,
            13,	10,
            13,	8,
            13,	6,
            13,	4,
            13,	2,
            11, 2,
            11, 5,
            11, 3,
            13, 1
    };

    vector<float> pointCoords;
    int n = 0;
    FDP *fdp = trackingData->featurePoints2D;

    for (int i = 0; i < 22; i++)
    {
        const FeaturePoint &fp = fdp->getFP(points[2*i],points[2*i+1]);
        if(fp.defined && fp.pos[0]!=0 && fp.pos[1]!=0)
        {
            pointCoords.push_back(fp.pos[0]);
            pointCoords.push_back(fp.pos[1]);
            n++;
        }
    }

    if(pointCoords.size() == 0 || n <= 2)
        return ;

    int factor = 10;
    vector<float> pointsToDraw;
    VisageRendering::CalcSpline(pointCoords, factor, pointsToDraw);
    int nVert = (int)pointsToDraw.size() / 2;
    int cnt = 0;
    for (int i = 0; i < nVert; ++i)
    {
        vertexCoordinates[3*i] = pointsToDraw.at(cnt++);
        vertexCoordinates[3*i+1] = pointsToDraw.at(cnt++);
        vertexCoordinates[3*i+2] = 0.0f;
    }
}


void VisageRendering::getTextureUnwrapDimensions(const VsImage *image, float *unwrapDimensions){
    int x_size = NearestPow2(image->width);
    int y_size = NearestPow2(image->height);
    float tex_x_coord = (float) image->width / (float) x_size;
    float tex_y_coord = (float) image->height / (float) y_size;
    unwrapDimensions[0] = tex_x_coord;
    unwrapDimensions[1] = tex_y_coord;
}


void VisageRendering::enableTexture(const VsImage *image, int width, int height){
    glPixelStorei(GL_UNPACK_ALIGNMENT, (image->widthStep & 3) ? 1 : 4);

    if (video_texture_inited && (video_texture_width!=image->width || video_texture_height!=image->height))
    {
        glDeleteTextures(1, &frame_tex_id);
        video_texture_inited = false;
    }

    if (!video_texture_inited )
    {
        InitFrameTex(NearestPow2(image->width), NearestPow2(image->height), image);
        video_texture_width = image->width;
        video_texture_height = image->height;
        video_texture_inited = true;
    }

    glBindTexture(GL_TEXTURE_2D, frame_tex_id);

    switch (image->nChannels) {
        case 3:
            #if defined (IOS) || defined (ANDROID)
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_RGB, GL_UNSIGNED_BYTE, image->imageData);
            #else
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_BGR, GL_UNSIGNED_BYTE, image->imageData);
            #endif
            break;
        case 4:
            #if defined(IOS) || defined(MAC_OS_X)
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_BGRA, GL_UNSIGNED_BYTE, image->imageData);
            #else
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_RGBA, GL_UNSIGNED_BYTE, image->imageData);
            #endif
            break;
        case 1:
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_LUMINANCE, GL_UNSIGNED_BYTE, image->imageData);
            break;
        default:
            return;
    }
}

void VisageRendering::DisplayFrameCopy(const VsImage *image, int width, int height){
    /*glPixelStorei(GL_UNPACK_ALIGNMENT, (image->widthStep & 3) ? 1 : 4);

    if (video_texture_inited && (video_texture_width!=image->width || video_texture_height!=image->height))
    {
        glDeleteTextures(1, &frame_tex_id);
        video_texture_inited = false;
    }

    if (!video_texture_inited )
    {
        InitFrameTex(NearestPow2(image->width), NearestPow2(image->height), image);
        video_texture_width = image->width;
        video_texture_height = image->height;
        video_texture_inited = true;
    }*/

    //Test Code starts
    glGenTextures(1, &frame_tex_id);
    glBindTexture(GL_TEXTURE_2D, frame_tex_id);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    //glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, NearestPow2(image->width), NearestPow2(image->height), 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, image->width, image->height, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
    glBindTexture(GL_TEXTURE_2D, frame_tex_id);
    // Test Code ends


    //glBindTexture(GL_TEXTURE_2D, frame_tex_id);

    switch (image->nChannels) {
        case 3:
            #if defined (IOS) || defined (ANDROID)
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_RGB, GL_UNSIGNED_BYTE, image->imageData);
            #else
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_BGR, GL_UNSIGNED_BYTE, image->imageData);
            #endif
            break;
        case 4:
            #if defined(IOS) || defined(MAC_OS_X)
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_BGRA, GL_UNSIGNED_BYTE, image->imageData);
            #else
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_RGBA, GL_UNSIGNED_BYTE, image->imageData);
            #endif
            break;
        case 1:
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_LUMINANCE, GL_UNSIGNED_BYTE, image->imageData);
            break;
        default:
            return;
    }

    #if defined(WIN32) || defined(LINUX)
    glPushAttrib(GL_DEPTH_BUFFER_BIT | GL_VIEWPORT_BIT | GL_ENABLE_BIT | GL_FOG_BIT | GL_STENCIL_BUFFER_BIT | GL_TRANSFORM_BIT | GL_TEXTURE_BIT );
    #endif

    glDisable(GL_ALPHA_TEST);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_BLEND );
    glDisable(GL_DITHER);
    glDisable(GL_FOG);
    glDisable(GL_SCISSOR_TEST);
    glDisable(GL_STENCIL_TEST);
    glDisable(GL_LIGHTING);
    glDisable(GL_LIGHT0);
    glEnable(GL_TEXTURE_2D);

    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();

    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();

    #if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
    glOrtho(0.0f,1.0f,0.0f,1.0f,-10.0f,10.0f);
    #else
    glOrthof(0.0f,1.0f,0.0f,1.0f,-10.0f,10.0f);
    #endif

    static GLfloat vertices[] = {
        0.0f,0.0f,-5.0f,
        1.0f,0.0f,-5.0f,
        0.0f,1.0f,-5.0f,
        1.0f,1.0f,-5.0f,
    };
    //LOGI("%s = %f , %f","Tex x and Tex y are",tex_x_coord,tex_y_coord);
    // tex coords are flipped upside down instead of an image
    /*GLfloat texcoords[] = {
        0.0f,			tex_y_coord,
        tex_x_coord,	tex_y_coord,
        0.0f,			0.0f,
        tex_x_coord,	0.0f,
    };*/

    GLfloat texcoords[] = {
        0.0f,   1.0f,
        1.0f,	1.0f,
        0.0f,   0.0f,
        1.0f,	0.0f,
    };




    glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    glEnableClientState(GL_VERTEX_ARRAY);

    glVertexPointer(3, GL_FLOAT, 0, vertices);
    glTexCoordPointer(2, GL_FLOAT, 0, texcoords);

    glViewport(0, 0, width, height);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    glDisableClientState(GL_VERTEX_ARRAY);

    glDisable(GL_TEXTURE_2D);

    glPopMatrix();
    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();

    glBindTexture(GL_TEXTURE_2D, 0);

    #if defined(WIN32) || defined(LINUX)
    glPopAttrib();
    #endif

    glClear(GL_DEPTH_BUFFER_BIT);
}
	
	void VisageRendering::DisplayFrame(const VsImage *image, int width, int height)
{
	glPixelStorei(GL_UNPACK_ALIGNMENT, (image->widthStep & 3) ? 1 : 4);

	if (video_texture_inited && (video_texture_width!=image->width || video_texture_height!=image->height))
	{
		glDeleteTextures(1, &frame_tex_id);
		video_texture_inited = false;
	}

	if (!video_texture_inited )
	{
		InitFrameTex(NearestPow2(image->width), NearestPow2(image->height), image);
		video_texture_width = image->width;
		video_texture_height = image->height;
		video_texture_inited = true;
	}

	glBindTexture(GL_TEXTURE_2D, frame_tex_id);

	switch (image->nChannels) {
		case 3:
			#if defined (IOS) || defined (ANDROID)
			glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_RGB, GL_UNSIGNED_BYTE, image->imageData);
			#else
			glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_BGR, GL_UNSIGNED_BYTE, image->imageData);
			#endif
			break;
		case 4:
			#if defined(IOS) || defined(MAC_OS_X)
			glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_BGRA, GL_UNSIGNED_BYTE, image->imageData);
			#else
			glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_RGBA, GL_UNSIGNED_BYTE, image->imageData);
			#endif
			break;
		case 1:
			glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_LUMINANCE, GL_UNSIGNED_BYTE, image->imageData);
			break;
		default:
			return;
	}

	#if defined(WIN32) || defined(LINUX)
	glPushAttrib(GL_DEPTH_BUFFER_BIT | GL_VIEWPORT_BIT | GL_ENABLE_BIT | GL_FOG_BIT | GL_STENCIL_BUFFER_BIT | GL_TRANSFORM_BIT | GL_TEXTURE_BIT );
	#endif

	glDisable(GL_ALPHA_TEST);
	glDisable(GL_DEPTH_TEST);
	glDisable(GL_BLEND );
	glDisable(GL_DITHER);
	glDisable(GL_FOG);
	glDisable(GL_SCISSOR_TEST);
	glDisable(GL_STENCIL_TEST);
	glDisable(GL_LIGHTING);
	glDisable(GL_LIGHT0);
	glEnable(GL_TEXTURE_2D);

	glMatrixMode(GL_MODELVIEW);
	glPushMatrix();
	glLoadIdentity();

	glMatrixMode(GL_PROJECTION);
	glPushMatrix();
	glLoadIdentity();

	#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
	glOrtho(0.0f,1.0f,0.0f,1.0f,-10.0f,10.0f);
	#else
	glOrthof(0.0f,1.0f,0.0f,1.0f,-10.0f,10.0f);
	#endif

	static GLfloat vertices[] = {
		0.0f,0.0f,-5.0f,
		1.0f,0.0f,-5.0f,
		0.0f,1.0f,-5.0f,
		1.0f,1.0f,-5.0f,
	};
    //LOGI("%s = %f , %f","Tex x and Tex y are",tex_x_coord,tex_y_coord);
	// tex coords are flipped upside down instead of an image
	GLfloat texcoords[] = {
		0.0f,			tex_y_coord,
		tex_x_coord,	tex_y_coord,
		0.0f,			0.0f,
		tex_x_coord,	0.0f,
	};



	glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

	glEnableClientState(GL_TEXTURE_COORD_ARRAY);
	glEnableClientState(GL_VERTEX_ARRAY);

	glVertexPointer(3, GL_FLOAT, 0, vertices);
	glTexCoordPointer(2, GL_FLOAT, 0, texcoords);

	glViewport(0, 0, width, height);
	glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

	glDisableClientState(GL_TEXTURE_COORD_ARRAY);
	glDisableClientState(GL_VERTEX_ARRAY);

	glDisable(GL_TEXTURE_2D);

	glPopMatrix();
	glMatrixMode(GL_MODELVIEW);
	glPopMatrix();

	glBindTexture(GL_TEXTURE_2D, 0);

	#if defined(WIN32) || defined(LINUX)
	glPopAttrib();
	#endif

	glClear(GL_DEPTH_BUFFER_BIT);
}

void VisageRendering::DisplayFeaturePoints(FaceData* trackingData, int width, int height, VsImage* frame, bool _3D, bool relative, bool drawQuality)
{
	glViewport(0, 0, width, height);

	#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
	glPushAttrib(GL_DEPTH_BUFFER_BIT | GL_VIEWPORT_BIT | GL_ENABLE_BIT | GL_FOG_BIT | GL_STENCIL_BUFFER_BIT | GL_TRANSFORM_BIT | GL_TEXTURE_BIT );
	#endif

	glDisable(GL_ALPHA_TEST);
	glDisable(GL_DEPTH_TEST);
	glDisable(GL_DITHER);
	glDisable(GL_FOG);
	glDisable(GL_SCISSOR_TEST);
	glDisable(GL_STENCIL_TEST);
	glDisable(GL_BLEND);

	glMatrixMode(GL_MODELVIEW);
	glPushMatrix();
	glLoadIdentity();

	glMatrixMode(GL_PROJECTION);
	glPushMatrix();
	glLoadIdentity();

	#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
	glOrtho(0.0f,1.0f,0.0f,1.0f,-10.0f,10.0f);
	#else
	glOrthof(0.0f,1.0f,0.0f,1.0f,-10.0f,10.0f);
	#endif

	if (_3D) {
		SetupCamera(width, height, trackingData->cameraFocus);

		if (relative) {
			const float *r = trackingData->faceRotation;
			const float *t = trackingData->faceTranslation;

			glTranslatef(t[0], t[1], t[2]);
			glRotatef(V_RAD2DEG(r[1] + V_PI), 0.0f, 1.0f, 0.0f);
			glRotatef(V_RAD2DEG(r[0]), 1.0f, 0.0f, 0.0f);
			glRotatef(V_RAD2DEG(r[2]), 0.0f, 0.0f, 1.0f);
		}
	}

	static int chinPoints[] = {
		2,	1
	};

	if (_3D) DrawPoints3D(chinPoints, 1, false, trackingData, frame, relative);
	else DrawPoints2D(chinPoints, 1, false, trackingData, frame, drawQuality);

	static int innerLipPoints[] = {
		2,	2,
		2,	6,
		2,	4,
		2,	8,
		2,	3,
		2,	9,
		2,	5,
		2,	7,
	};
	if (_3D) DrawPoints3D(innerLipPoints, 8, false, trackingData, frame, relative);
	else DrawPoints2D(innerLipPoints, 8, false, trackingData, frame, drawQuality);

	static int outerLipPoints[] = {
		8,	1,
		8,	10,
		8,	5,
		8,	3,
		8,	7,
		8,	2,
		8,	8,
		8,	4,
		8,	6,
		8,	9,
	};
	if (_3D) DrawPoints3D(outerLipPoints, 10, false, trackingData, frame, relative);
	else DrawPoints2D(outerLipPoints, 10, false, trackingData, frame, drawQuality);

	static int nosePoints[] = {
		9,	5,
		9,	4,
		9,	3,
		9,	15,
		14, 22,
		14, 23,
		14, 24,
		14, 25
	};
	if (_3D) DrawPoints3D(nosePoints, 8, false, trackingData, frame, relative);
	else DrawPoints2D(nosePoints, 8, false, trackingData, frame, drawQuality);

	if(trackingData->eyeClosure[1] > 0.5f)
	{
		//if eye is open, draw the iris
		glColor4ub(200,80,0,255);
		static int irisPoints[] = {
			3,	6
		};
		if (_3D) DrawPoints3D(irisPoints, 1, false, trackingData, frame, relative);
		else DrawPoints2D(irisPoints, 1, false, trackingData, frame, drawQuality);
	}

	if(trackingData->eyeClosure[0] > 0.5f)
	{
		glColor4ub(200, 80, 0, 255);
		static int irisPoints[] = {
			3,	5
		};
		if (_3D) DrawPoints3D(irisPoints, 1, false, trackingData, frame, relative);
		else DrawPoints2D(irisPoints, 1, false, trackingData, frame, drawQuality);
	}

	static int eyesPointsR[] = {
		3,	2,
		3,	4,
		3,	8,
		3,	10,
		3,	12,
		3,	14,
		12, 6,
		12, 8,
		12, 10,
		12, 12
	};
	if (_3D) DrawPoints3D(eyesPointsR, 10, trackingData->eyeClosure[1] <= 0.5f, trackingData, frame, relative);
	else DrawPoints2D(eyesPointsR, 10, trackingData->eyeClosure[1] <= 0.5f, trackingData, frame, drawQuality);

	static int eyesPointsL[] = {
		3,	1,
		3,	3,
		3,	7,
		3,	9,
		3,	11,
		3,	13,
		12, 5,
		12, 7,
		12, 9,
		12, 11
	};
	if (_3D) DrawPoints3D(eyesPointsL, 10, trackingData->eyeClosure[0] <= 0.5f, trackingData, frame, relative);
	else DrawPoints2D(eyesPointsL, 10, trackingData->eyeClosure[0] <= 0.5f, trackingData, frame, drawQuality);

	static int eyebrowPoints[] = {
		4,	1,
		4,	2,
		4,	3,
		4,	4,
		4,	5,
		4,	6,
		14,	1,
		14,	2,
		14,	3,
		14,	4
	};
	if (_3D) DrawPoints3D(eyebrowPoints, 10, false, trackingData, frame, relative);
	else DrawPoints2D(eyebrowPoints, 10, false, trackingData, frame, drawQuality);


	// visible contour
	static int contourPointsVisible[] = {
		13,	1,
		13,	3,
		13,	5,
		13,	7,
		13,	9,
		13,	11,
		13,	13,
		13,	15,
		13,	17,
		13,	16,
		13,	14,
		13,	12,
		13,	10,
		13,	8,
		13,	6,
		13,	4,
		13,	2
	};
	if (_3D) DrawPoints3D(contourPointsVisible, 17, false, trackingData, frame, relative);
	else DrawPoints2D(contourPointsVisible, 17, false, trackingData, frame, drawQuality);

	glPopMatrix();
	glMatrixMode(GL_MODELVIEW);
	glPopMatrix();
	#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
	glPopAttrib();
	#endif
}

void VisageRendering::DisplaySplines(FaceData* trackingData, int width, int height)
{
	glViewport(0,0,width,height);

	#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
	glPushAttrib(GL_DEPTH_BUFFER_BIT | GL_VIEWPORT_BIT | GL_ENABLE_BIT | GL_FOG_BIT | GL_STENCIL_BUFFER_BIT | GL_TRANSFORM_BIT | GL_TEXTURE_BIT );
	#endif

	glDisable(GL_ALPHA_TEST);
	glDisable(GL_DEPTH_TEST);
	glDisable(GL_DITHER);
	glDisable(GL_FOG);
	glDisable(GL_SCISSOR_TEST);
	glDisable(GL_STENCIL_TEST);
	
	glMatrixMode(GL_MODELVIEW);
	glPushMatrix();
	glLoadIdentity();

	glMatrixMode(GL_PROJECTION);
	glPushMatrix();
	glLoadIdentity();

	#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
	glOrtho(0.0f,1.0f,0.0f,1.0f,-10.0f,10.0f);
	#else
	glOrthof(0.0f,1.0f,0.0f,1.0f,-10.0f,10.0f);
	#endif

	glPointSize(3);
	glLineWidth(2);

	glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	glEnable(GL_BLEND);

	glColor4ub(176,196,222,160);
	static int outerUpperLipPoints[] = {
		8, 4,
		8, 6,
		8, 9,
		8, 1,
		8, 10,
		8, 5,
		8, 3,
	};
	DrawSpline2D(outerUpperLipPoints, 7, trackingData);

	static int outerLowerLipPoints[] = {
		8, 4,
		8, 8,
		8, 2,
		8, 7,
		8, 3,
	};
	DrawSpline2D(outerLowerLipPoints, 5, trackingData);

	static int innerUpperLipPoints[] = {
		2, 5,
		2, 7,
		2, 2,
		2, 6,
		2, 4,
	};
	DrawSpline2D(innerUpperLipPoints, 5, trackingData);

	static int innerLowerLipPoints[] = {
		2, 5,
		2, 9,
		2, 3,
		2, 8,
		2, 4,
	};
	DrawSpline2D(innerLowerLipPoints, 5, trackingData);

	static int noseLinePoints[] = {
		9,	5,
		9,	3,
		9,	4
	};
	DrawSpline2D(noseLinePoints, 3, trackingData);

	static int noseLinePoints2[] = {
		9,	3,
		14,	22,
		14,	23,
		14, 24,
		14, 25
	};
	DrawSpline2D(noseLinePoints2, 5, trackingData);

	static int outerUpperEyePointsR[] = {
		3,	12,
		3,	14,
		3,	8
	};
	DrawSpline2D(outerUpperEyePointsR, 3, trackingData);

	static int outerLowerEyePointsR[] = {
		3,	8,
		3,	10,
		3,	12
	};
	DrawSpline2D(outerLowerEyePointsR, 3, trackingData);

	static int innerUpperEyePointsR[] = {
		3,	12,
		12, 10,
		3,	2,
		12, 6,
		3,	8
	};
	DrawSpline2D(innerUpperEyePointsR, 5, trackingData);

	static int innerLowerEyePointsR[] = {
		3,	8,
		12, 8,
		3,	4,
		12, 12,
		3,	12
	};
	DrawSpline2D(innerLowerEyePointsR, 5,trackingData);

	static int outerUpperEyePointsL[] = {
		3,	11,
		3,	13,
		3,	7
	};
	DrawSpline2D(outerUpperEyePointsL, 3, trackingData);

	static int outerLowerEyePointsL[] = {
		3,	7,
		3,	9,
		3,	11
	};
	DrawSpline2D(outerLowerEyePointsL, 3, trackingData);

	static int innerUpperEyePointsL[] = {
		3,	11,
		12, 9,
		3,	1,
		12, 5,
		3,	7
	};
	DrawSpline2D(innerUpperEyePointsL, 5, trackingData);

	static int innerLowerEyePointsL[] = {
		3,	7,
		12, 7,
		3,	3,
		12, 11,
		3,	11
	};
	DrawSpline2D(innerLowerEyePointsL, 5, trackingData);

	static int eyebrowLinesPointsR[] = {
		4,	6,
		14,	4,
		4,	4,
		14,	2,
		4,	2
	};
	DrawSpline2D(eyebrowLinesPointsR, 5, trackingData);

	static int eyebrowLinesPointsL[] = {
		4,	1,
		14,	1,
		4,	3,
		14,	3,
		4,	5
	};
	DrawSpline2D(eyebrowLinesPointsL, 5, trackingData);

	// visible contour
	static int contourLinesPointsLVisible[] = {
		13,	1,
		13,	3,
		13,	5,
		13,	7,
		13,	9,
		13,	11,
		13,	13,
		13,	15,
		13,	17,
		13,	16,
		13,	14,
		13,	12,
		13,	10,
		13,	8,
		13,	6,
		13,	4,
		13,	2
	};
	DrawSpline2D(contourLinesPointsLVisible, 17, trackingData);

	glMatrixMode(GL_PROJECTION);
	glPopMatrix();
	glMatrixMode(GL_MODELVIEW);
	glPopMatrix();

	#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
	glPopAttrib();
	#endif
}

void VisageRendering::DisplayGaze(FaceData* trackingData, int width, int height)
{
	glViewport(0,0,width,height);

	glMatrixMode(GL_MODELVIEW);
	glPushMatrix();
	glLoadIdentity();

	glMatrixMode(GL_PROJECTION);
	glPushMatrix();
	glLoadIdentity();

	SetupCamera(width, height, trackingData->cameraFocus);

	glShadeModel(GL_FLAT);

	static float vertices[] = {
		0.0f, 0.0f, 0.0f,
		0.0f, 0.0f, 0.04f
	};

	float tr[6] = {0, 0, 0, 0, 0, 0};

	FDP *fdp = trackingData->featurePoints3D;

	const FeaturePoint &leye = fdp->getFP(3,5);
	const FeaturePoint &reye = fdp->getFP(3,6);

	if(leye.defined && reye.defined)
	{
		tr[0] = leye.pos[0];
		tr[1] = leye.pos[1];
		tr[2] = leye.pos[2];
		tr[3] = reye.pos[0];
		tr[4] = reye.pos[1];
		tr[5] = reye.pos[2];
	}

	float h_rot = V_RAD2DEG(trackingData->gazeDirectionGlobal[1] + V_PI);
	float v_rot = V_RAD2DEG(trackingData->gazeDirectionGlobal[0]);
	float roll	= V_RAD2DEG(trackingData->gazeDirectionGlobal[2]);

	glEnableClientState(GL_VERTEX_ARRAY);

	glMatrixMode(GL_MODELVIEW);
	glPushMatrix();

	glTranslatef(tr[0],tr[1],tr[2]);
	glRotatef(h_rot, 0.0f, 1.0f, 0.0f);
	glRotatef(v_rot, 1.0f, 0.0f, 0.0f);
	glRotatef(roll, 0.0f, 0.0f, 1.0f);

	glLineWidth(2);

	glColor4ub(240,96,0,255);

	if(trackingData->eyeClosure[0] > 0.5f)
	{
		glVertexPointer(3, GL_FLOAT, 0, vertices);
		glDrawArrays(GL_LINES, 0, 2);
	}

	glPopMatrix();

	glPushMatrix();

	glTranslatef(tr[3],tr[4],tr[5]);
	glRotatef(h_rot, 0.0f, 1.0f, 0.0f);
	glRotatef(v_rot, 1.0f, 0.0f, 0.0f);
	glRotatef(roll, 0.0f, 0.0f, 1.0f);

	if(trackingData->eyeClosure[1] > 0.5f)
	{
		glVertexPointer(3, GL_FLOAT, 0, vertices);
		glDrawArrays(GL_LINES, 0, 2);
	}

	glDisableClientState(GL_VERTEX_ARRAY);

	glPopMatrix();

	glMatrixMode(GL_PROJECTION);
	glPopMatrix();

	glMatrixMode(GL_MODELVIEW);
	glPopMatrix();
}

void VisageRendering::DisplayModelAxes(FaceData* trackingData, int width, int height)
{
	glViewport(0,0,width,height);

	glMatrixMode(GL_MODELVIEW);
	glPushMatrix();
	glLoadIdentity();

	glMatrixMode(GL_PROJECTION);
	glPushMatrix();
	glLoadIdentity();

	SetupCamera(width, height, trackingData->cameraFocus);

	glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	glEnable(GL_BLEND);

	glShadeModel(GL_FLAT);

	//rotate and translate into the current coordinate system of the head
	const float *r = trackingData->faceRotation;
	//const float *t = trackingData->faceTranslation;
	FDP *fdp = trackingData->featurePoints3D;

	const FeaturePoint &fp1 = fdp->getFP(4, 1);
	const FeaturePoint &fp2 = fdp->getFP(4, 2);

	glTranslatef((fp1.pos[0] + fp2.pos[0])/2.0f, (fp1.pos[1] + fp2.pos[1])/2.0f, (fp1.pos[2] + fp2.pos[2])/2.0f);
	glRotatef(V_RAD2DEG(r[1] + V_PI), 0.0f, 1.0f, 0.0f);
	glRotatef(V_RAD2DEG(r[0]), 1.0f, 0.0f, 0.0f);
	glRotatef(V_RAD2DEG(r[2]), 0.0f, 0.0f, 1.0f);

	static const float coordVertices[] = {
		0.0f,	0.0f,	0.0f,
		0.07f,	0.0f,	0.0f,
		0.0f,	0.0f,	0.0f,
		0.0f,	0.07f,	0.0f,
		0.0f,	0.0f,	0.0f,
		0.0f,	0.0f,	0.07f,
	};

	static const float coordColors[] = {
		1.0f, 0.0f, 0.0f, 0.25f,
		1.0f, 0.0f, 0.0f, 0.25f,
		0.0f, 0.0f, 1.0f, 0.25f,
		0.0f, 0.0f, 1.0f, 0.25f,
		0.0f, 1.0f, 0.0f, 0.25f,
		0.0f, 1.0f, 0.0f, 0.25f,
	};

	glLineWidth(2);

	glEnableClientState(GL_VERTEX_ARRAY);
	glEnableClientState(GL_COLOR_ARRAY);
	glVertexPointer(3, GL_FLOAT, 0, coordVertices);
	glColorPointer(4, GL_FLOAT, 0, coordColors);
	glDrawArrays(GL_LINES, 0, 6);
	glDisableClientState(GL_VERTEX_ARRAY);
	glDisableClientState(GL_COLOR_ARRAY);

	glMatrixMode(GL_PROJECTION);
	glPopMatrix();

	glMatrixMode(GL_MODELVIEW);
	glPopMatrix();

	#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
	glPopAttrib();
	#endif
}

int* VisageRendering::getCorrectedTriangleData(FaceData* trackingData, int *length){
    std::vector<GLushort> indexList;
    static std::vector<GLushort> output;
    for (int i = 0; i < trackingData->faceModelTriangleCount; i++) {
        GLushort triangle[] = {
            static_cast<GLushort>(trackingData->faceModelTriangles[3*i+0]),
            static_cast<GLushort>(trackingData->faceModelTriangles[3*i+1]),
            static_cast<GLushort>(trackingData->faceModelTriangles[3*i+2]),
        };
        if (triangle[0] > triangle[1])
            swap(triangle[0], triangle[1]);
        if (triangle[0] > triangle[2])
            swap(triangle[0], triangle[2]);
        if (triangle[1] > triangle[2])
            swap(triangle[1], triangle[2]);

        indexList.push_back(triangle[0]);
        indexList.push_back(triangle[1]);
        indexList.push_back(triangle[2]);
        //LOGI("current triangle indices: %2d %2d %2d", triangle[0], triangle[1], triangle[2]);
    }
    output.clear();
    for (std::vector<GLushort>::iterator it= indexList.begin(); it!=indexList.end(); ++it)
    {
        output.push_back(*it);
    }
    //LOGI("%s - %d","output.size()",output.size());
    int* outputFinal = new int[output.size()];
    int i = 0;
    for (std::vector<GLushort>::iterator it = output.begin() ; it != output.end(); ++it){
        outputFinal[i] = *it;
        i++;
    }
    *length = output.size();
    for(int i = 0; i < *length; i++) {
        //LOGI("output: %2d", outputFinal[i]);
    }
    return outputFinal;
}

void VisageRendering::DisplayWireFrame(FaceData* trackingData, int width, int height)
{
	//set image specs
	glViewport(0,0,width,height);

	glMatrixMode(GL_MODELVIEW);
	glPushMatrix();
	glLoadIdentity();

	glMatrixMode(GL_PROJECTION);
	glPushMatrix();
	glLoadIdentity();

	SetupCamera(width, height, trackingData->cameraFocus);

	glEnableClientState(GL_VERTEX_ARRAY);
	glShadeModel(GL_FLAT);
	//set the color for the wireframe
	glColor4f(0.0f,1.0f,0.0f,1.0f);
	//vertex list
	glVertexPointer(3,GL_FLOAT,0,trackingData->faceModelVertices);

	glLineWidth(1);

	const float *r = trackingData->faceRotation;
	const float *t = trackingData->faceTranslation;

	glTranslatef(t[0], t[1], t[2]);
	glRotatef(V_RAD2DEG(r[1] + V_PI), 0.0f, 1.0f, 0.0f);
	glRotatef(V_RAD2DEG(r[0]), 1.0f, 0.0f, 0.0f);
	glRotatef(V_RAD2DEG(r[2]), 0.0f, 0.0f, 1.0f);

	//draw the wireframe
	//initialize indexes for drawing wireframe (once per model)
	if	(numberOfVertices != trackingData->faceModelVertexCount)
	{
		std::set<std::pair<GLushort,GLushort> > indexList;

		for (int i = 0; i < trackingData->faceModelTriangleCount; i++) {
			GLushort triangle[] = {
				static_cast<GLushort>(trackingData->faceModelTriangles[3*i+0]),
				static_cast<GLushort>(trackingData->faceModelTriangles[3*i+1]),
				static_cast<GLushort>(trackingData->faceModelTriangles[3*i+2]),
			};
			if (triangle[0] > triangle[1])
				swap(triangle[0], triangle[1]);
			if (triangle[0] > triangle[2])
				swap(triangle[0], triangle[2]);
			if (triangle[1] > triangle[2])
				swap(triangle[1], triangle[2]);

			indexList.insert(std::make_pair(triangle[0], triangle[1]));
			indexList.insert(std::make_pair(triangle[1], triangle[2]));
			indexList.insert(std::make_pair(triangle[0], triangle[2]));
		}

		output.clear();
		for (std::set<std::pair<GLushort,GLushort> >::iterator it= indexList.begin(); it!=indexList.end(); ++it)
		{
			output.push_back((*it).first);
			output.push_back((*it).second);
		}
	}

	for (std::vector<GLushort>::iterator it = output.begin() ; it != output.end(); ++it){
	    LOGI("%s - %d","Element value is ",*it);
	}
	int h = 0;
	//for (h = 0; h < trackingData->faceModelTriangleCount; h++){
    //    LOGI("%s - %d - %d","Triangle with index value is ",3*h+0,trackingData->faceModelTriangles[3*h+0]);
    //    LOGI("%s - %d - %d","Triangle with index value is ",3*h+1,trackingData->faceModelTriangles[3*h+1]);
    //    LOGI("%s - %d - %d","Triangle with index value is ",3*h+2,trackingData->faceModelTriangles[3*h+2]);
    //}
    //LOGI("%s - %d","Output size is",output.size());
	numberOfVertices = trackingData->faceModelVertexCount;

	glDrawElements(GL_LINES, (int)output.size(), GL_UNSIGNED_SHORT, &output[0]);
	glDisableClientState(GL_VERTEX_ARRAY);

	glMatrixMode(GL_PROJECTION);
	glPopMatrix();

	glMatrixMode(GL_MODELVIEW);
	glPopMatrix();
}

void VisageRendering::CalcSpline(vector <float>& inputPoints, int ratio, vector <float>& outputPoints) {

	int nPoints, nPointsToDraw, nLines;

	nPoints = (int)inputPoints.size()/2 + 2;
	nPointsToDraw = (int)inputPoints.size()/2 + ((int)inputPoints.size()/2 - 1) * ratio;
	nLines = nPoints - 1 - 2;

	inputPoints.insert(inputPoints.begin(), inputPoints[1] + (inputPoints[1] - inputPoints[3]));
	inputPoints.insert(inputPoints.begin(), inputPoints[1] + (inputPoints[1] - inputPoints[3]));
	inputPoints.insert(inputPoints.end(), inputPoints[inputPoints.size()/2-2] + (inputPoints[inputPoints.size()/2-2] - inputPoints[inputPoints.size()/2-4]));
	inputPoints.insert(inputPoints.end(), inputPoints[inputPoints.size()/2-1] + (inputPoints[inputPoints.size()/2-1] - inputPoints[inputPoints.size()/2-3]));

	Vec2D p0(0,0), p1(0,0), p2(0,0), p3(0,0);
	CubicPoly px, py;

	outputPoints.resize(2*nPointsToDraw);

	for(int i = 0; i < nPoints - 2; i++) {
		outputPoints[i*2*(ratio+1)] = inputPoints[2*i+2];
		outputPoints[i*2*(ratio+1)+1] = inputPoints[2*i+1+2];
	}

	for(int i = 0; i < 2*nLines; i=i+2) {
		p0.x = inputPoints[i];
		p0.y = inputPoints[i+1];
		p1.x = inputPoints[i+2];
		p1.y = inputPoints[i+3];
		p2.x = inputPoints[i+4];
		p2.y = inputPoints[i+5];
		p3.x = inputPoints[i+6];
		p3.y = inputPoints[i+7];

		InitCentripetalCR(p0, p1, p2, p3, px, py);

		for(int j = 1; j <= ratio; j++) {
			outputPoints[i*(ratio+1)+2*j] = (px.eval(1.00f/(ratio+1)*(j)));
			outputPoints[i*(ratio+1)+2*j+1] =(py.eval(1.00f/(ratio+1)*(j)));
		}

	}

	inputPoints.erase(inputPoints.begin(), inputPoints.begin()+2);
	inputPoints.erase(inputPoints.end()-2, inputPoints.end());
}

void VisageRendering::DisplayTrackingQualityBar(FaceData* trackingData)
{
	glMatrixMode(GL_MODELVIEW);
	glPushMatrix();
	glLoadIdentity();

	glMatrixMode(GL_PROJECTION);
	glPushMatrix();
	glLoadIdentity();

	#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
	glOrtho(0.0f, 1.0f, 0.0f, 1.0f, -10.0f, 10.0f);
	#else
	glOrthof(0.0f, 1.0f, 0.0f, 1.0f, -10.0f, 10.0f);
	#endif

	int points_to_draw = 2;
	float vertices[6];
	char tmpbuff[200];
	glLineWidth(10);

	vertices[0] = 0.1f;
	vertices[1] = 0.9f;
	vertices[2] = 0.0f;
	vertices[3] = 0.25f;
	vertices[4] = 0.9f;
	vertices[5] = 0.0f;
	glColor4f(0.5, 0.5, 0.5, 1);
	glEnableClientState(GL_VERTEX_ARRAY);
	glVertexPointer(3, GL_FLOAT, 0, vertices);
	glDrawArrays(GL_LINES, 0, points_to_draw);
	glDisableClientState(GL_VERTEX_ARRAY);

	vertices[0] = 0.1f;
	vertices[1] = 0.9f;
	vertices[2] = 0.0f;
	vertices[3] = 0.1 + trackingData->trackingQuality * 0.15f;
	vertices[4] = 0.9f;
	vertices[5] = 0.0f;
	glColor4f((1 - trackingData->trackingQuality), trackingData->trackingQuality, 0, 1);
	glEnableClientState(GL_VERTEX_ARRAY);
	glVertexPointer(3, GL_FLOAT, 0, vertices);
	glDrawArrays(GL_LINES, 0, points_to_draw);
	glDisableClientState(GL_VERTEX_ARRAY);
	glLineWidth(1);

	glMatrixMode(GL_PROJECTION);
	glPopMatrix();

	glMatrixMode(GL_MODELVIEW);
	glPopMatrix();
}

void VisageRendering::Reset()
{
	video_texture_inited = false;
	logo_tex_id = -1;
}

void VisageRendering::DisplayFace(FaceData* trackingData, const VsImage *image, int width, int height){

    // Disable everything
    glDisable(GL_ALPHA_TEST);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_BLEND );
    glDisable(GL_DITHER);
    glDisable(GL_FOG);
    glDisable(GL_SCISSOR_TEST);
    glDisable(GL_STENCIL_TEST);
    glDisable(GL_LIGHTING);
    glDisable(GL_LIGHT0);

    // Resetting modelview to original state
    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();

    // Resetting projection to identity
    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();

    // Setting camera projection
    #if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
    glOrtho(0.0f,1.0f,0.0f,1.0f,-10.0f,10.0f);
    #else
    glOrthof(0.0f,1.0f,0.0f,1.0f,-10.0f,10.0f);
    #endif

    /*glPointSize(3);
    glLineWidth(4);

    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glEnable(GL_BLEND);

    glColor4ub(176,196,222,160);
    // Face Contour Points
    static int contourLinesPointsLVisible[] = {
    		13,	1,
    		13,	3,
    		13,	5,
    		13,	7,
    		13,	9,
    		13,	11,
    		13,	13,
    		13,	15,
    		13,	17,
    		13,	16,
    		13,	14,
    		13,	12,
    		13,	10,
    		13,	8,
    		13,	6,
    		13,	4,
    		13,	2,
    		11, 2,
    		11, 5,
    		11, 3,
    		13, 1
    };*/
    DrawFaceContourWithTexture(trackingData,image,width,height);
    //DrawSpline2D(contourLinesPointsLVisible, 21, trackingData);
    /*int numVertices = trackingData->faceModelVertexCount;
    int numTriangles = trackingData->faceModelTriangleCount;
    float* vertices = new float[numVertices * 3];
    float* texCoordinates = new float[numVertices * 2];
    int* trianglesData = new int[numTriangles * 3];
    int i = 0;
    for(i = 0; i < numVertices; i++){
        vertices[3 * i] = trackingData->faceModelVerticesProjected[2 * i];
        vertices[3 * i + 1] = trackingData->faceModelVerticesProjected[2 * i + 1];
        vertices[3 * i + 2] = -5;

        texCoordinates[2 * i] = trackingData->faceModelTextureCoords[2 * i];
        texCoordinates[2 * i + 1] = 1 - trackingData->faceModelTextureCoords[2 * i + 1];
    }
    i = 0;
    for (i = 0; i < numTriangles; i++){
        trianglesData[3 * i] = trackingData->faceModelTriangles[3 * i];
        trianglesData[3 * i + 1] = trackingData->faceModelTriangles[3 * i + 1];
        trianglesData[3 * i + 2] = trackingData->faceModelTriangles[3 * i + 2];
    }
    // Defining color
    glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    glEnableClientState(GL_VERTEX_ARRAY);

    glVertexPointer(3, GL_FLOAT, 0, vertices);
    glTexCoordPointer(2, GL_FLOAT, 0, texCoordinates);

    glViewport(0, 0, width, height);
    //glDrawElements(GL_TRIANGLES,numTriangles * 3,GL_UNSIGNED_SHORT,trianglesData);
    glDrawArrays(GL_TRIANGLES, 0, numVertices);

    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    glDisableClientState(GL_VERTEX_ARRAY);
    glDisable(GL_TEXTURE_2D);
    */
    //glMatrixMode(GL_PROJECTION);
    glPopMatrix();
    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();

    //glBindTexture(GL_TEXTURE_2D, -1);

    //delete[] vertices;
    //delete[] texCoordinates;
    //delete[] trianglesData;
}

void VisageRendering::DisplayImage(const VsImage *frame, int width, int height){
    ClearGL();
    DisplayFrameCopy(frame,width,height);
}


void VisageRendering::DisplayResults(FaceData* trackingData, int trackStat, int width, int height, VsImage* frame, int drawingOptions)
{
	glViewport(0,0,width,height);

	if(frame != NULL && (drawingOptions & DISPLAY_FRAME))
	{
		ClearGL();
		DisplayFrame(frame, width, height);
	}
	
	if(trackStat == TRACK_STAT_OK)
	{
		if(drawingOptions & DISPLAY_SPLINES)
		{
			DisplaySplines(trackingData, width, height);
		}
		
		if(drawingOptions & DISPLAY_FEATURE_POINTS)
		{
			bool drawQuality = drawingOptions & DISPLAY_POINT_QUALITY;
			DisplayFeaturePoints(trackingData, width, height, frame, false, false, drawQuality); // draw 2D feature points
			//DisplayFeaturePoints(trackingData, width, height, frame, true); // draw 3D feature points
			//DisplayFeaturePoints(trackingData, width, height, frame, true, true); // draw relative 3D feature points
		}
		
		if(drawingOptions & DISPLAY_GAZE)
		{
			DisplayGaze(trackingData, width, height);
		}

		if(drawingOptions & DISPLAY_AXES)
		{
			DisplayModelAxes(trackingData, width, height);
		}

		if(drawingOptions & DISPLAY_WIRE_FRAME)
		{
			DisplayWireFrame(trackingData, width, height);
		}

		if (drawingOptions & DISPLAY_TRACKING_QUALITY)
		{
			DisplayTrackingQualityBar(trackingData);
		}
	}

}

}
