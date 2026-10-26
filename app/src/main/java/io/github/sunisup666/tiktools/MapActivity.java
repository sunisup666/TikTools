package io.github.sunisup666.tiktools;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.*;
import android.os.Bundle;
import android.util.LruCache;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

public final class MapActivity extends Activity {
    private MapView map;private TextView coordinates,status;private double selectedLat,selectedLon;
    private Presets.City selectedCity;
    private String tr(int id,Object... args){return args.length==0?getString(id):getString(id,args);}
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setMinHeight(dp(48));return b;}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);Config c=Config.load(this);selectedLat=state==null?c.number("lat",14.5995):state.getDouble("lat");selectedLon=state==null?c.number("lon",120.9842):state.getDouble("lon");
        if(state!=null)for(Presets.City cityPreset:Presets.CITIES)if(cityPreset.toString().equals(state.getString("selected_city")))selectedCity=cityPreset;
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(0xfff3f6f5);
        root.setOnApplyWindowInsetsListener((v,insets)->{Insets i=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(dp(12),i.top+dp(8),dp(12),i.bottom+dp(8));return insets;});
        TextView heading=new TextView(this);heading.setText(tr(R.string.map_picker));heading.setTextColor(0xff162c2a);heading.setTextSize(25);root.addView(heading);
        TextView hint=new TextView(this);hint.setText(tr(R.string.map_help));hint.setTextSize(13);root.addView(hint);
        LinearLayout actions=new LinearLayout(this);Button city=button(tr(R.string.map_jump_city)),back=button(tr(R.string.map_back_selection));actions.addView(city,new LinearLayout.LayoutParams(0,-2,1));actions.addView(back,new LinearLayout.LayoutParams(0,-2,1));root.addView(actions);
        FrameLayout frame=new FrameLayout(this);map=new MapView();frame.addView(map,new FrameLayout.LayoutParams(-1,-1));root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout zoom=new LinearLayout(this);zoom.setOrientation(1);Button plus=button("＋"),minus=button("－");plus.setContentDescription(tr(R.string.map_zoom_in));minus.setContentDescription(tr(R.string.map_zoom_out));zoom.addView(plus,new LinearLayout.LayoutParams(dp(48),dp(48)));zoom.addView(minus,new LinearLayout.LayoutParams(dp(48),dp(48)));FrameLayout.LayoutParams z=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.END);z.setMargins(0,dp(8),dp(6),0);frame.addView(zoom,z);
        TextView attribution=new TextView(this);attribution.setText("© OpenStreetMap contributors");attribution.setTextSize(11);attribution.setTextColor(0xff263e3a);attribution.setBackgroundColor(0xeaffffff);attribution.setPadding(dp(5),dp(3),dp(5),dp(3));FrameLayout.LayoutParams a=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.END);frame.addView(attribution,a);attribution.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://www.openstreetmap.org/copyright")));}catch(Exception ignored){}});
        status=new TextView(this);status.setText(tr(R.string.map_loading));status.setTextSize(12);status.setTextColor(0xff5a716c);root.addView(status);status.setOnClickListener(v->{map.failures.clear();status.setText(tr(R.string.map_reloading));map.invalidate();});
        coordinates=new TextView(this);coordinates.setTextSize(16);coordinates.setTextColor(0xff162c2a);coordinates.setPadding(0,dp(6),0,dp(6));root.addView(coordinates);showCoordinates();
        TextView note=new TextView(this);note.setText(tr(R.string.map_fixed_note));note.setTextSize(12);root.addView(note);
        LinearLayout footer=new LinearLayout(this);Button cancel=button(tr(R.string.cancel)),save=button(tr(R.string.map_use_location));save.setTextColor(0xff176a63);footer.addView(cancel,new LinearLayout.LayoutParams(0,dp(50),1));footer.addView(save,new LinearLayout.LayoutParams(0,dp(50),2));root.addView(footer);setContentView(root);
        plus.setOnClickListener(v->map.zoom(1,map.getWidth()/map.density/2,map.getHeight()/map.density/2));minus.setOnClickListener(v->map.zoom(-1,map.getWidth()/map.density/2,map.getHeight()/map.density/2));back.setOnClickListener(v->map.center(selectedLat,selectedLon));
        city.setOnClickListener(v->{String[] labels=Arrays.stream(Presets.CITIES).map(cityPreset->cityPreset.label(this)).toArray(String[]::new);new AlertDialog.Builder(this).setTitle(tr(R.string.map_jump_city)).setItems(labels,(d,n)->{selectedCity=Presets.CITIES[n];selectedLat=selectedCity.lat();selectedLon=selectedCity.lon();map.center(selectedLat,selectedLon);showCoordinates();}).setNegativeButton(tr(R.string.cancel),null).show();});
        cancel.setOnClickListener(v->finish());save.setOnClickListener(v->{Intent result=new Intent().putExtra("lat",selectedLat).putExtra("lon",selectedLon);if(selectedCity!=null)result.putExtra("city_iso",selectedCity.iso()).putExtra("city_zone",selectedCity.zone()).putExtra("city_name",selectedCity.toString());setResult(RESULT_OK,result);finish();});
        if(state!=null){map.level=state.getInt("zoom",14);map.center(state.getDouble("center_lat",selectedLat),state.getDouble("center_lon",selectedLon));}
    }
    private void showCoordinates(){coordinates.setText(String.format(Locale.ROOT,tr(R.string.map_coordinates),selectedLat,selectedLon)+(selectedCity==null?"":"\n"+selectedCity.label(this)));}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putDouble("lat",selectedLat);state.putDouble("lon",selectedLon);if(selectedCity!=null)state.putString("selected_city",selectedCity.toString());state.putInt("zoom",map.level);state.putDouble("center_lat",WebMercator.lat(map.cy,map.level));state.putDouble("center_lon",WebMercator.lon(map.cx,map.level));}
    @Override protected void onDestroy(){if(map!=null)map.close();super.onDestroy();}
    private final class MapView extends View {
        final float density=getResources().getDisplayMetrics().density;int level=14;double cx,cy;boolean closed;int loaded=0;
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        final LruCache<String,Bitmap> images=new LruCache<>(24*1024*1024){@Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getAllocationByteCount();}};
        final Set<String> pending=ConcurrentHashMap.newKeySet();final Map<String,Long> failures=new ConcurrentHashMap<>();volatile Set<String> visible=Set.of();
        final ThreadPoolExecutor network=new ThreadPoolExecutor(2,2,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(32));
        final MapTileStore store=new MapTileStore(MapActivity.this);final String template=Config.load(MapActivity.this).text("map_tile_url","https://tile.openstreetmap.org/{z}/{x}/{y}.png");
        final GestureDetector gestures;final ScaleGestureDetector scales;float accumulated=1;
        MapView(){super(MapActivity.this);setContentDescription(tr(R.string.map_accessibility));center(selectedLat,selectedLon);
            gestures=new GestureDetector(MapActivity.this,new GestureDetector.SimpleOnGestureListener(){
                @Override public boolean onDown(android.view.MotionEvent e){return true;}
                @Override public boolean onScroll(android.view.MotionEvent a,android.view.MotionEvent b,float dx,float dy){if(scales.isInProgress())return true;cx=WebMercator.wrap(cx+dx/density,WebMercator.size(level));cy=Math.max(0,Math.min(WebMercator.size(level),cy+dy/density));invalidate();return true;}
                @Override public boolean onSingleTapConfirmed(android.view.MotionEvent e){selectedLat=WebMercator.lat(cy+(e.getY()-getHeight()/2f)/density,level);selectedLon=WebMercator.lon(cx+(e.getX()-getWidth()/2f)/density,level);showCoordinates();invalidate();performClick();return true;}
                @Override public boolean onDoubleTap(android.view.MotionEvent e){zoom(1,e.getX()/density,e.getY()/density);return true;}
            });
            scales=new ScaleGestureDetector(MapActivity.this,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScaleBegin(ScaleGestureDetector d){accumulated=1;return true;}@Override public boolean onScale(ScaleGestureDetector d){accumulated*=d.getScaleFactor();if(accumulated>1.4){zoom(1,d.getFocusX()/density,d.getFocusY()/density);accumulated=1;}else if(accumulated<.7){zoom(-1,d.getFocusX()/density,d.getFocusY()/density);accumulated=1;}return true;}});
        }
        void center(double lat,double lon){cx=WebMercator.x(lon,level);cy=WebMercator.y(lat,level);invalidate();}
        void zoom(int delta,float focusX,float focusY){int next=Math.max(2,Math.min(18,level+delta));if(next==level)return;double ox=focusX-getWidth()/density/2,oy=focusY-getHeight()/density/2,scale=Math.scalb(1.0,next-level);cx=(cx+ox)*scale-ox;cy=(cy+oy)*scale-oy;level=next;cx=WebMercator.wrap(cx,WebMercator.size(level));cy=Math.max(0,Math.min(WebMercator.size(level),cy));invalidate();}
        @Override public boolean performClick(){super.performClick();return true;}
        @Override public boolean onTouchEvent(android.view.MotionEvent event){scales.onTouchEvent(event);gestures.onTouchEvent(event);return true;}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.save();canvas.scale(density,density);float w=getWidth()/density,h=getHeight()/density;canvas.drawColor(0xffe8eeeb);double left=cx-w/2,top=cy-h/2;int n=1<<level;Set<String> active=new HashSet<>();
            for(int y=(int)Math.floor(top/256);y<=(int)Math.floor((top+h)/256);y++)for(int x=(int)Math.floor(left/256);x<=(int)Math.floor((left+w)/256);x++){
                if(y<0||y>=n)continue;int wrapped=Math.floorMod(x,n);String key=level+"/"+wrapped+"/"+y;active.add(key);Bitmap bitmap=images.get(key);float px=(float)(x*256.0-left),py=(float)(y*256.0-top);
                if(bitmap!=null)canvas.drawBitmap(bitmap,null,new RectF(px,py,px+256,py+256),paint);else{paint.setColor(0xffd8e1dd);paint.setStrokeWidth(1);canvas.drawLine(px,py,px+256,py,paint);canvas.drawLine(px,py,px,py+256,paint);}
            }
            visible=active;for(String key:active)if(images.get(key)==null)request(key);
            float pinX=(float)(w/2+WebMercator.near(WebMercator.x(selectedLon,level)-cx,level)),pinY=(float)(h/2+WebMercator.y(selectedLat,level)-cy);paint.setColor(0xffca3f44);canvas.drawCircle(pinX,pinY-12,10,paint);Path pin=new Path();pin.moveTo(pinX-8,pinY-9);pin.lineTo(pinX,pinY);pin.lineTo(pinX+8,pinY-9);pin.close();canvas.drawPath(pin,paint);paint.setColor(Color.WHITE);canvas.drawCircle(pinX,pinY-12,3,paint);canvas.restore();
        }
        void request(String key){if(closed||failures.getOrDefault(key,0L)>System.currentTimeMillis()||!pending.add(key))return;
            try{network.execute(()->{try{if(closed||!visible.contains(key))return;String[] part=key.split("/");String address=template.replace("{z}",part[0]).replace("{x}",part[1]).replace("{y}",part[2]);Bitmap bitmap=store.load(address);if(closed)return;images.put(key,bitmap);post(()->{if(closed)return;loaded++;status.setText(tr(R.string.map_loaded_prefix)+level);invalidate();});}catch(Exception e){failures.put(key,System.currentTimeMillis()+30000);post(()->{if(!closed&&loaded==0)status.setText(tr(R.string.map_failed));});}finally{pending.remove(key);}});}catch(RejectedExecutionException ignored){pending.remove(key);}
        }
        void close(){closed=true;network.shutdownNow();images.evictAll();}
    }
}
