import io.github.sunisup666.tiktools.WebMercator;

public final class MapMathCheck {
    static void near(double actual,double expected,double epsilon){if(Math.abs(actual-expected)>epsilon)throw new AssertionError(actual+" != "+expected);}
    public static void main(String[] args){
        double[][] points={{0,0},{35.6762,139.6503},{40.7128,-74.006},{-33.8688,151.2093},{85,179.99},{-85,-179.99}};
        for(int zoom:new int[]{2,8,14,18}){
            double size=WebMercator.size(zoom);near(WebMercator.x(0,zoom),size/2,1e-8);near(WebMercator.y(0,zoom),size/2,1e-8);
            for(double[] point:points){near(WebMercator.lon(WebMercator.x(point[1],zoom),zoom),point[1],1e-9);near(WebMercator.lat(WebMercator.y(point[0],zoom),zoom),point[0],1e-9);}
            near(WebMercator.near(WebMercator.x(-179,zoom)-WebMercator.x(179,zoom),zoom),size*2/360,1e-7);
            if(!(WebMercator.y(45,zoom)<size/2&&WebMercator.y(-45,zoom)>size/2))throw new AssertionError("Latitude axis reversed");
        }
        System.out.println("Map coordinate reference points, round trips, hemispheres and dateline checks passed.");
    }
}
