package io.github.sunisup666.tiktools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.json.JSONObject;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Cache only tiles the user is viewing; retain HTTP validators and origin expiry. */
final class MapTileStore {
    private final File directory;
    MapTileStore(Context context){directory=new File(context.getCacheDir(),"map-tiles");directory.mkdirs();}
    Bitmap load(String address)throws Exception{
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(address.getBytes(StandardCharsets.UTF_8));
        StringBuilder key=new StringBuilder();for(byte b:digest)key.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        File image=new File(directory,key+".png"),meta=new File(directory,key+".json");JSONObject cached=new JSONObject();
        if(meta.isFile())try{cached=new JSONObject(new String(Files.readAllBytes(meta.toPath()),StandardCharsets.UTF_8));}catch(Exception ignored){}
        if(image.isFile()&&cached.optLong("expires")>System.currentTimeMillis()){Bitmap ready=decode(image);if(ready!=null)return ready;image.delete();meta.delete();cached=new JSONObject();}
        HttpURLConnection connection=(HttpURLConnection)new URL(address).openConnection();
        connection.setConnectTimeout(7000);connection.setReadTimeout(9000);connection.setRequestProperty("User-Agent","TikTools/"+BuildConfig.VERSION_NAME+" (io.github.sunisup666.tiktools; Android interactive map)");
        if(image.isFile()){
            if(!cached.optString("etag").isEmpty())connection.setRequestProperty("If-None-Match",cached.optString("etag"));
            if(cached.optLong("modified")>0)connection.setIfModifiedSince(cached.optLong("modified"));
        }
        try{
            int status=connection.getResponseCode();
            if(status!=200&&status!=304)throw new java.io.IOException("Map HTTP "+status);
            String control=connection.getHeaderField("Cache-Control");
            long expires=connection.getExpiration();Matcher maxAge=Pattern.compile("(?i)(?:^|,)\\s*max-age=(\\d+)").matcher(control==null?"":control);
            if(maxAge.find())expires=System.currentTimeMillis()+Math.min(31536000L,Long.parseLong(maxAge.group(1)))*1000;
            if(expires<=0)expires=System.currentTimeMillis()+7L*24*3600*1000;
            boolean store=control==null||!control.toLowerCase(java.util.Locale.ROOT).contains("no-store");
            Bitmap bitmap;
            if(status==200){
                byte[] bytes;try(var input=connection.getInputStream()){bytes=input.readNBytes(2*1024*1024+1);}
                if(bytes.length>2*1024*1024)throw new java.io.IOException("Tile too large");
                bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);if(bitmap==null)throw new java.io.IOException("Invalid tile");
                if(store){atomic(image,bytes);}
            }else{bitmap=decode(image);if(bitmap==null)throw new java.io.IOException("Missing cached tile");}
            if(store){JSONObject next=new JSONObject().put("expires",expires).put("etag",connection.getHeaderField("ETag")==null?cached.optString("etag"):connection.getHeaderField("ETag")).put("modified",Math.max(connection.getLastModified(),cached.optLong("modified")));atomic(meta,next.toString().getBytes(StandardCharsets.UTF_8));}
            bitmap.setDensity(Bitmap.DENSITY_NONE);return bitmap;
        }finally{connection.disconnect();}
    }
    private static Bitmap decode(File image){Bitmap result=BitmapFactory.decodeFile(image.getAbsolutePath());if(result!=null)result.setDensity(Bitmap.DENSITY_NONE);return result;}
    private static void atomic(File target,byte[] bytes)throws Exception{File temp=File.createTempFile("tile-",".part",target.getParentFile());try{Files.write(temp.toPath(),bytes);Files.move(temp.toPath(),target.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);}finally{temp.delete();}}
}
