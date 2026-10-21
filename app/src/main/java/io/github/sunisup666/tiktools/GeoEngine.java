package io.github.sunisup666.tiktools;

import java.util.ArrayList;
import java.util.List;

public final class GeoEngine {
    private final Config config;
    private final long started = android.os.SystemClock.elapsedRealtime();
    private final List<double[]> points = new ArrayList<>();
    public GeoEngine(Config config) {
        this.config=config;
        try {
            for (String s:config.text("route_points", "").split("[;\\n]+")) {
                if (s.trim().isEmpty()) continue;
                String[] pair=s.trim().split(","); points.add(new double[]{Double.parseDouble(pair[0]),Double.parseDouble(pair[1])});
            }
        } catch (Exception e) { points.clear(); }
    }
    public synchronized double[] position() {
        double lat=config.number("lat",14.5995),lon=config.number("lon",120.9842);
        double seconds=(android.os.SystemClock.elapsedRealtime()-started)/1000.0;
        double speed=config.number("route_speed",1.4),radius=config.number("route_radius",20);
        String mode=config.text("route_mode","fixed");
        if (mode.equals("circle") && radius>0) {
            double angle=seconds*speed/radius;
            lat+=radius*Math.sin(angle)/111320.0;
            lon+=radius*Math.cos(angle)/(111320.0*Math.max(0.01,Math.cos(Math.toRadians(lat))));
        } else if (mode.equals("route") && points.size()>1) {
            double total=0;
            for (int i=1;i<points.size();i++) total+=distance(points.get(i-1),points.get(i));
            if (total>0) {
                double position=(seconds*speed)%(2*total); if (position>total) position=2*total-position;
                for (int i=1;i<points.size();i++) {
                    double[] from=points.get(i-1),to=points.get(i); double length=distance(from,to);
                    if (position<=length && length>0) { double f=position/length;lat=from[0]+(to[0]-from[0])*f;lon=from[1]+(to[1]-from[1])*f;break; }
                    position-=length;
                }
            }
        }
        if (config.on("gpsdrift")) { lat+=Math.sin(seconds*0.31)*0.000012;lon+=Math.cos(seconds*0.27)*0.000012; }
        lat=Math.max(-90,Math.min(90,lat));lon=((lon+540)%360)-180;
        return new double[]{lat,lon};
    }
    private static double distance(double[] a,double[] b) {
        double p=Math.toRadians(a[0]),q=Math.toRadians(b[0]),d=Math.toRadians(b[0]-a[0]),e=Math.toRadians(b[1]-a[1]);
        double h=Math.sin(d/2)*Math.sin(d/2)+Math.cos(p)*Math.cos(q)*Math.sin(e/2)*Math.sin(e/2);
        return 12742000*Math.asin(Math.sqrt(Math.min(1,h)));
    }
}
