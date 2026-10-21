package io.github.sunisup666.tiktools;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

/** Links media to its visible caption, so preloaded adjacent videos cannot replace a save target. */
final class MediaCatalog {
    private record Entry(String id,String url) {}
    private static final LinkedHashMap<String,Entry> ITEMS=new LinkedHashMap<>();
    private static int nativeControls;
    static synchronized int size(){return ITEMS.size();}
    static synchronized int controls(){return nativeControls;}
    static String normalize(String value){return java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFKC).replaceAll("[\\s\\p{Z}\\u200b-\\u200f\\ufeff]+", "").trim();}
    static synchronized String forCaption(String caption){Entry e=ITEMS.get(normalize(caption));return e==null?"":e.url();}
    static int install(ClassLoader loader) throws Throwable {
        Class<?> aweme=Class.forName("com.ss.android.ugc.aweme.feed.model.Aweme",false,loader);
        int count=0;
        for(Method method:aweme.getDeclaredMethods()){
            if(method.getName().equals("setDescTitleTranslatedText")&&method.getParameterCount()==1){
                XposedBridge.hookMethod(method,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{remember(p.thisObject,XposedHelpers.callMethod(p.thisObject,"getVideo"));}catch(Throwable ignored){}}});count++;
            }
            if(method.getParameterCount()!=0)continue;
            if(method.getName().equals("getVideo")){
                XposedBridge.hookMethod(method,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){
                    try{remember(p.thisObject,p.getResult());}catch(Throwable ignored){}
                }});count++;
            }
            if(method.getName().equals("getVideoControl")){
                XposedBridge.hookMethod(method,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){
                    Object control=p.getResult();if(control==null)return;
                    try{XposedHelpers.setObjectField(control,"allowDownload",Boolean.TRUE);XposedHelpers.setIntField(control,"preventDownloadType",0);synchronized(MediaCatalog.class){nativeControls=Math.min(nativeControls+1,1000000);}}catch(Throwable ignored){}
                }});count++;
            }
        }
        if(count==0)throw new NoSuchMethodException("Aweme media accessors");
        return count;
    }
    private static void remember(Object aweme,Object video) throws Throwable {
        if(video==null)return;
        ArrayList<String> captions=new ArrayList<>();
        captions.add(String.valueOf(XposedHelpers.callMethod(aweme,"getDesc")));
        try{captions.add(String.valueOf(XposedHelpers.callMethod(aweme,"getContentDesc")));}catch(Throwable ignored){}
        try{Object translated=XposedHelpers.callMethod(aweme,"getDescTitleTranslatedText");if(translated!=null)captions.add(String.valueOf(XposedHelpers.getObjectField(translated,"translatedDescContent")));}catch(Throwable ignored){}
        String id=String.valueOf(XposedHelpers.callMethod(aweme,"getAid"));
        String url="";
        for(String getter:new String[]{"getDownloadAddr","getPlayAddr"}){
            try{Object model=XposedHelpers.callMethod(video,getter);if(model==null)continue;Object list=XposedHelpers.callMethod(model,"getUrlList");
                if(list instanceof List<?> urls)for(Object entry:urls){String candidate=String.valueOf(entry),lower=candidate.toLowerCase(Locale.ROOT);if(candidate.startsWith("https://")&&candidate.length()<8192&&!lower.contains(".m3u8")&&!lower.contains(".mpd")){url=candidate;break;}}
            }catch(Throwable ignored){}
            if(!url.isEmpty())break;
        }
        if(url.isEmpty())return;
        synchronized(MediaCatalog.class){for(String caption:captions){String key=normalize(caption);if(key.isEmpty()||key.equals("null"))continue;Entry old=ITEMS.get(key);if(old!=null&&!old.id().equals(id)){ITEMS.put(key,new Entry("ambiguous",""));continue;}ITEMS.put(key,new Entry(id,url));}while(ITEMS.size()>64)ITEMS.remove(ITEMS.keySet().iterator().next());}
    }
}
