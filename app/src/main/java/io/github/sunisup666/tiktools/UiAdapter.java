package io.github.sunisup666.tiktools;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public final class UiAdapter {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private static final Map<Activity,Session> SESSIONS=new WeakHashMap<>();
    private static final Map<String,Integer> MATCHES=new LinkedHashMap<>();
    private static final Map<String,Integer> TOTAL_MATCHES=new LinkedHashMap<>();
    private static final LinkedHashSet<String> IDS=new LinkedHashSet<>();
    private static JSONArray SAMPLES=new JSONArray();
    static JSONObject matches(){return new JSONObject(MATCHES);}
    static JSONObject totalMatches(){return new JSONObject(TOTAL_MATCHES);}
    static JSONArray ids(){return new JSONArray(IDS);}
    static JSONArray samples(){return SAMPLES;}
    public static void install(Context context,Config config,Runnable report) {
        if(!config.on("ui_enabled")&&!config.on("trans")&&!config.on("download_restrictions"))return;
        XposedHelpers.findAndHookMethod(Activity.class,"onResume",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){Activity a=(Activity)p.thisObject;if(!a.getPackageName().equals(context.getPackageName()))return;Session session=SESSIONS.get(a);if(session==null){session=new Session(a,config,report);SESSIONS.put(a,session);}session.start();}});
        XposedHelpers.findAndHookMethod(Activity.class,"onPause",new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){Session s=SESSIONS.get((Activity)p.thisObject);if(s!=null)s.stop();}});
        XposedHelpers.findAndHookMethod(Activity.class,"onDestroy",new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){Session s=SESSIONS.remove((Activity)p.thisObject);if(s!=null)s.stop();}});
    }
    private static final class Session implements Runnable {
        final Activity activity;final Config config;final Runnable report;
        final WeakHashMap<View,Integer> hidden=new WeakHashMap<>();
        TextView translation,tool;String caption="",lastTranslated="";boolean running,translating;int ticks=0,missingCaption=0;
        final boolean trill4743;
        Session(Activity a,Config c,Runnable r){activity=a;config=c;report=r;boolean supported=false;try{supported=a.getPackageName().equals("com.ss.android.ugc.trill")&&a.getPackageManager().getPackageInfo(a.getPackageName(),0).getLongVersionCode()==470403;}catch(Exception ignored){}trill4743=supported;}
        int dp(float n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
        void start(){stop();running=true;MAIN.postDelayed(this,700);}
        void stop(){running=false;MAIN.removeCallbacks(this);}
        @Override public void run(){if(!running||activity.isFinishing()||activity.isDestroyed())return;try{scan();}catch(Throwable e){XposedBridge.log("TikTools UI: "+e.getClass().getSimpleName());}if(running)MAIN.postDelayed(this,1400);}
        String id(View v){try{return v.getId()==View.NO_ID?"":v.getResources().getResourceEntryName(v.getId());}catch(Throwable ignored){return "";}}
        boolean inFeed(View v){for(int i=0;i<30&&v!=null;i++){if(id(v).equals("view_rootview"))return true;v=v.getParent() instanceof View?(View)v.getParent():null;}return false;}
        String visibleText(View v,String name){
            if(v instanceof TextView t)return t.getText().toString();
            if(trill4743&&name.equals("desc"))try{Object layout=v.getClass().getMethod("getTextLayout").invoke(v);if(layout instanceof android.text.Layout l)return l.getText().toString();}catch(Throwable ignored){}
            if(trill4743&&inFeed(v)&&("android.widget.Button".contentEquals(v.getAccessibilityClassName())||"android.widget.TextView".contentEquals(v.getAccessibilityClassName())))try{android.view.accessibility.AccessibilityNodeInfo info=v.createAccessibilityNodeInfo();if(info!=null){CharSequence text=info.getText();info.recycle();if(text!=null)return text.toString();}}catch(Throwable ignored){}
            return "";
        }
        boolean containsMedia(View v,int budget){if(budget<=0)return true;if(v instanceof SurfaceView||v instanceof TextureView||v instanceof VideoView)return true;if(v instanceof ViewGroup g){for(int i=0;i<g.getChildCount();i++)if(containsMedia(g.getChildAt(i),budget-1))return true;}return false;}
        boolean safe(View v,int screenHeight){return v.getHeight()>0&&v.getHeight()<screenHeight*0.28&&!containsMedia(v,10);}
        void scan() {
            View decor=activity.getWindow().getDecorView();if(!(decor instanceof ViewGroup root)||!root.isShown())return;
            Set<View> keepHidden=new HashSet<>();
            int height=root.getHeight(),width=root.getWidth();if(height<=0||width<=0)return;
            if(config.on("ui_enabled")&&config.on("fullscreen")){WindowInsetsController controller=activity.getWindow().getInsetsController();if(controller!=null){controller.hide(WindowInsets.Type.systemBars());controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}}
            ArrayDeque<View> queue=new ArrayDeque<>();queue.add(root);int budget=1200;String foundCaption="";
            JSONObject custom;try{custom=new JSONObject(config.text("custom_selectors","{}"));}catch(Exception e){custom=new JSONObject();}
            Map<String,Integer> hits=new LinkedHashMap<>();SAMPLES=new JSONArray();
            Rect visible=new Rect(),screen=new Rect();root.getGlobalVisibleRect(screen);
            while(!queue.isEmpty()&&budget-->0){View v=queue.remove();if("tiktools-overlay".equals(v.getTag()))continue;boolean ours=hidden.containsKey(v);if(!v.isShown()&&!ours)continue;
                if(!v.getGlobalVisibleRect(visible)){if(!ours)continue;int[] position=new int[2];v.getLocationOnScreen(position);visible.set(position[0],position[1],position[0]+v.getWidth(),position[1]+v.getHeight());}
                if(!Rect.intersects(screen,visible))continue;
                if(v.getVisibility()==View.VISIBLE&&v instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++)queue.add(g.getChildAt(i));
                String name=id(v),lower=name.toLowerCase(Locale.ROOT);if(!name.isEmpty()&&IDS.size()<180)IDS.add(name);
                String desc=v.getContentDescription()==null?"":v.getContentDescription().toString();String text=visibleText(v,name);
                int[] xy=new int[2];v.getLocationOnScreen(xy);boolean top=xy[1]<height*0.26,bottom=xy[1]>height*0.78,right=xy[0]>width*0.65;
                if(!name.isEmpty()&&(v instanceof TextView||v instanceof ImageView||!text.isEmpty())&&SAMPLES.length()<55){try{JSONObject sample=new JSONObject().put("id",name).put("class",v.getClass().getSimpleName()).put("x",xy[0]).put("y",xy[1]).put("w",v.getWidth()).put("h",v.getHeight()).put("text_length",text.length()).put("clickable",v.isClickable());if(Set.of("推荐","关注","好友","首页","主页","收件箱","商城","社区","LIVE","For You","Following","Home","Profile").contains(text))sample.put("label",text);SAMPLES.put(sample);}catch(Exception ignored){}}
                String semantic=(desc+" "+text).toLowerCase(Locale.ROOT).trim();
                boolean captionMatch=match(custom,"purify_desc",name)||lower.contains("video_desc")||lower.contains("video_caption")||lower.equals("desc")||desc.toLowerCase(Locale.ROOT).startsWith("video description");
                if(captionMatch&&!text.isBlank()&&text.length()<=3000&&foundCaption.isEmpty())foundCaption=text;
                if(!config.on("ui_enabled")||!safe(v,height))continue;
                String selected=null;
                for(String key:new String[]{"purify_topbar","purify_channel","purify_tabbar","purify_sidebtn","purify_author","purify_desc","purify_music","purify_logo","purify_location","purify_translate","purify_paywall"}){
                    if(!config.on(key))continue;boolean hit=match(custom,key,name);
                    hit|=switch(key){
                        case "purify_topbar" -> top&&(lower.contains("top_tab")||Set.of("推荐","关注","好友","社区","商城","for you","following","friends").contains(text.toLowerCase(Locale.ROOT)));
                        case "purify_channel" -> top&&(lower.contains("channel")||text.equalsIgnoreCase("LIVE"));
                        case "purify_tabbar" -> bottom&&(lower.contains("bottom_tab")||Set.of("首页","商城","社区","收件箱","主页","home","shop","inbox","profile").contains(text.toLowerCase(Locale.ROOT)));
                        case "purify_sidebtn" -> right&&(lower.contains("like_icon")||lower.contains("comment_icon")||lower.contains("share_icon")||shortSemantic(semantic,"like video","comment","share","点赞","评论","分享","收藏"));
                        case "purify_author" -> xy[1]>height*.35&&(lower.contains("author_name")||lower.contains("nickname")||lower.equals("user_name"));
                        case "purify_desc" -> captionMatch;
                        case "purify_music" -> lower.contains("music_title")||lower.contains("music_name")||lower.contains("music_cover");
                        case "purify_logo" -> lower.contains("watermark")||lower.contains("brand_logo");
                        case "purify_location" -> lower.contains("poi_name")||lower.contains("location_tag");
                        case "purify_translate" -> lower.contains("translate_button")||Set.of("查看翻译","翻译","see translation").contains(text.toLowerCase(Locale.ROOT));
                        case "purify_paywall" -> lower.contains("promotion_label")||lower.contains("paid_content_label")||Set.of("推广","sponsored","paid partnership").contains(text.toLowerCase(Locale.ROOT));
                        default -> false;
                    };
                    if(trill4743)hit|=switch(key){
                        case "purify_topbar" -> top&&Set.of("k_g","title_shadow","uwq").contains(name);
                        case "purify_channel" -> top&&name.equals("k_d");
                        case "purify_tabbar" -> bottom&&Set.of("onh","ong","ond","oni","onj").contains(name);
                        case "purify_sidebtn" -> right&&inFeed(v)&&Set.of("g8m","eq2","i9b","iws","user_avatar","z_j").contains(name);
                        case "purify_author" -> inFeed(v)&&name.equals("title")&&xy[1]>height*.35;
                        case "purify_music" -> inFeed(v)&&Set.of("pog","o7a").contains(name);
                        default -> false;
                    };
                    if(hit){selected=key;break;}
                }
                if(selected==null&&config.on("comment_filter")&&v instanceof TextView&&(lower.contains("comment_text")||match(custom,"comment_filter",name)||(trill4743&&name.equals("f62")))){for(String word:config.text("filter_keywords","").split("[\\n,，]+"))if(!word.trim().isEmpty()&&text.toLowerCase(Locale.ROOT).contains(word.trim().toLowerCase(Locale.ROOT))){selected="comment_filter";break;}}
                if(selected!=null){if(!hidden.containsKey(v)){hidden.put(v,v.getVisibility());TOTAL_MATCHES.merge(selected,1,(a,b)->Math.min(1000000,a+b));}keepHidden.add(v);if(v.getVisibility()!=View.INVISIBLE)v.setVisibility(View.INVISIBLE);hits.merge(selected,1,Integer::sum);}
            }
            for(var e:new ArrayList<>(hidden.entrySet()))if(!keepHidden.contains(e.getKey())){View view=e.getKey();if(view!=null&&view.getVisibility()==View.INVISIBLE)view.setVisibility(e.getValue());hidden.remove(view);}
            MATCHES.clear();MATCHES.putAll(hits);
            if(!foundCaption.isEmpty()){missingCaption=0;if(!caption.equals(foundCaption)){caption=foundCaption;lastTranslated="";if(translation!=null)translation.setVisibility(View.GONE);}}
            else if(++missingCaption>=2){caption="";lastTranslated="";if(translation!=null)translation.setVisibility(View.GONE);}
            HookEntry.mediaUrl=MediaCatalog.forCaption(caption);
            ensureTools(root);
            if(config.on("trans")&&!caption.isEmpty()&&!caption.equals(lastTranslated)&&!translating)translate(caption,false);
            if(++ticks%4==0)report.run();
        }
        boolean shortSemantic(String text,String...words){if(text.length()>160)return false;for(String word:words)if(text.startsWith(word))return true;return false;}
        boolean match(JSONObject rules,String key,String id){JSONArray array=rules.optJSONArray(key);if(array==null||id.isEmpty())return false;for(int i=0;i<array.length();i++)if(id.equals(array.optString(i)))return true;return false;}
        void ensureTools(ViewGroup root){
            if(config.on("show_tool_button")&&tool==null){tool=new TextView(activity);tool.setTag("tiktools-overlay");tool.setText("TT");tool.setTextColor(Color.WHITE);tool.setTextSize(13);tool.setGravity(Gravity.CENTER);GradientDrawable bg=new GradientDrawable();bg.setColor(0xc9176a63);bg.setCornerRadius(dp(20));tool.setBackground(bg);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(42),dp(42),Gravity.START|Gravity.CENTER_VERTICAL);lp.leftMargin=dp(6);root.addView(tool,lp);tool.setOnClickListener(v->menu());}
            if(config.on("trans")&&translation==null){translation=new TextView(activity);translation.setTag("tiktools-overlay");translation.setTextColor(Color.WHITE);translation.setTextSize(14);translation.setMaxLines(5);translation.setPadding(dp(12),dp(8),dp(12),dp(8));translation.setBackgroundColor(0xb0000000);translation.setVisibility(View.GONE);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);lp.setMargins(dp(18),0,dp(70),dp(110));root.addView(translation,lp);}
        }
        void menu(){String[] items={"打开 TikTools 设置","翻译当前文案","输入文字翻译","保存当前直链视频","查看本页适配信息"};new AlertDialog.Builder(activity).setTitle("TikTools").setItems(items,(d,n)->{
            if(n==0){try{activity.startActivity(new Intent().setClassName("io.github.sunisup666.tiktools","io.github.sunisup666.tiktools.MainActivity"));}catch(Exception e){toast("无法打开设置");}}
            if(n==1){if(caption.isEmpty())toast("当前版本尚未识别到视频文案");else translate(caption,true);}
            if(n==2){EditText input=new EditText(activity);new AlertDialog.Builder(activity).setTitle("本机翻译").setView(input).setNegativeButton("取消",null).setPositiveButton("翻译",(x,w)->translate(input.getText().toString(),true)).show();}
            if(n==3)download();
            if(n==4)new AlertDialog.Builder(activity).setTitle("当前页面匹配").setMessage("已匹配："+MATCHES+"\n资源名称："+IDS).setPositiveButton("关闭",null).show();
        }).show();}
        void translate(String input,boolean manual){
            if(!config.on("trans")){toast("请先在工具箱开启翻译");return;}if(input.isBlank()||translating)return;translating=true;
            WORKER.execute(()->{Bundle args=new Bundle();args.putString("text",input);args.putBoolean("download",false);Bundle result;try{result=activity.getContentResolver().call(SettingsProvider.URI,"translate",null,args);}catch(Throwable e){result=null;}Bundle ready=result;MAIN.post(()->{translating=false;if(activity.isFinishing()||activity.isDestroyed()||(!manual&&!input.equals(caption)))return;if(ready!=null&&ready.getBoolean("ok")){lastTranslated=input;String out=ready.getString("text","");if(manual)new AlertDialog.Builder(activity).setTitle("翻译结果").setMessage(out).setPositiveButton("关闭",null).show();else if(translation!=null){translation.setText(out);translation.setVisibility(View.VISIBLE);}}else{lastTranslated=input;if(manual)toast("请先在 TikTools 工具页下载对应语言模型");}});});
        }
        void download(){if(!config.on("download_restrictions")){toast("请先开启下载控制");return;}String url=HookEntry.mediaUrl;if(url.isEmpty()){toast("当前播放器尚未提供可识别的视频直链");return;}if(url.contains(".m3u8")||url.contains(".mpd")){toast("分段流请使用 TikTok 自带下载功能");return;}new AlertDialog.Builder(activity).setTitle("保存当前视频").setMessage("将从当前播放地址下载到 Movies/TikTools。").setNegativeButton("取消",null).setPositiveButton("下载",(d,w)->WORKER.execute(()->{Bundle args=new Bundle();args.putString("url",url);try{Bundle result=activity.getContentResolver().call(SettingsProvider.URI,"download",null,args);MAIN.post(()->toast(result!=null&&result.getBoolean("ok")?"已加入下载队列":"下载未开始"));}catch(Throwable e){MAIN.post(()->toast("下载服务不可用"));}})).show();}
        void toast(String s){Toast.makeText(activity,s,Toast.LENGTH_LONG).show();}
    }
}
