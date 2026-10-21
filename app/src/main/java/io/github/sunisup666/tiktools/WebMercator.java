package io.github.sunisup666.tiktools;

/** WGS84 coordinates to standard raster-tile pixels. No Android or location permissions needed. */
public final class WebMercator {
    private WebMercator(){}
    public static double size(int zoom){return Math.scalb(256.0,zoom);}
    public static double wrap(double value,double range){return value-Math.floor(value/range)*range;}
    public static double x(double lon,int zoom){return wrap((lon+180)/360*size(zoom),size(zoom));}
    public static double y(double lat,int zoom){double phi=Math.toRadians(Math.max(-85.05112878,Math.min(85.05112878,lat)));return (1-Math.log(Math.tan(phi)+1/Math.cos(phi))/Math.PI)/2*size(zoom);}
    public static double lon(double x,int zoom){return wrap(x,size(zoom))/size(zoom)*360-180;}
    public static double lat(double y,int zoom){double clamped=Math.max(0,Math.min(size(zoom),y));return Math.toDegrees(Math.atan(Math.sinh(Math.PI*(1-2*clamped/size(zoom)))));}
    public static double near(double delta,int zoom){return delta-Math.floor((delta+size(zoom)/2)/size(zoom))*size(zoom);}
}
