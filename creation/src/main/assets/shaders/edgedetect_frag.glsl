#extension GL_OES_EGL_image_external : require

void main(void) {
    vec4 pixel = image;
    float y = (pixel.r + 16.0) / 116.0;
    float x = y + (pixel.g/500.0);
    float z = y - (pixel.b/200.0);

    // D65 standard referent
    float X = 0.950470, Y = 1.0, Z = 1.088830;

    x = X * (x > 0.206893034 ? x*x*x : (x - 4.0/29.0) / 7.787037);
    y = Y * (y > 0.206893034 ? y*y*y : (y - 4.0/29.0) / 7.787037);
    z = Z * (z > 0.206893034 ? z*z*z : (z - 4.0/29.0) / 7.787037);

    // second, map CIE XYZ to sRGB
    float r =  3.2404542*x - 1.5371385*y - 0.4985314*z;
    float g = -0.9692660*x + 1.8760108*y + 0.0415560*z;
    float b =  0.0556434*x - 0.2040259*y + 1.0572252*z;

    r = (r <= 0.00304) ? 12.92*r : 1.055 * pow(r,1.0/2.4) - 0.055;
    g = (g <= 0.00304) ? 12.92*g : 1.055 * pow(g,1.0/2.4) - 0.055;
    b = (b <= 0.00304) ? 12.92*b : 1.055 * pow(b,1.0/2.4) - 0.055;

    // third, get sRGB values
    float ir = r; ir = max(0.0, min(ir, 1.0));
    float ig = g; ig = max(0.0, min(ig, 1.0));
    float ib = b; ib = max(0.0, min(ib, 1.0));

    gl_FragColor = vec4(ir, ig, ib, 1.0);
}
