package io.github.sunisup666.tiktools;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class MainActivity extends Activity {
    private JSONObject config;
    private LinearLayout page;
    private ScrollView scroll;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private int tab=0;
    private static final int GREEN=0xff176a63,INK=0xff162c2a,MUTED=0xff5a716c,BG=0xfff3f6f5;
    private static final String[] TABS={"概览","地区","定位","设备","界面","工具"};
    @Override public void onCreate(Bundle state) { super.onCreate(state);config=Config.load(this).json;if(state!=null)tab=state.getInt("tab",0);render(); }
    private int dp(float n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private TextView text(String value,int size,int color) { TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(5),0,dp(5));return t; }
    private GradientDrawable round(int color) { GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(18));return d; }
    private void render() {
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(dp(18),i.top+dp(10),dp(18),i.bottom+dp(8));return insets;});
        TextView title=text("TikTools",30,INK);title.setTypeface(null,Typeface.BOLD);root.addView(title);
        root.addView(text("你的 TikTok 工具箱",14,MUTED));
        HorizontalScrollView navScroll=new HorizontalScrollView(this);navScroll.setHorizontalScrollBarEnabled(false);LinearLayout nav=new LinearLayout(this);
        for(int n=0;n<TABS.length;n++){final int index=n;Button b=new Button(this);b.setText(TABS[n]);b.setTextColor(n==tab?Color.WHITE:GREEN);b.setBackground(round(n==tab?GREEN:0xffe0eae6));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(66),dp(42));lp.setMargins(0,dp(8),dp(6),dp(12));nav.addView(b,lp);b.setOnClickListener(v->{tab=index;render();});}
        navScroll.addView(nav);root.addView(navScroll);navScroll.post(()->navScroll.scrollTo(Math.max(0,dp((tab+1)*72)-navScroll.getWidth()),0));
        scroll=new ScrollView(this);scroll.setFillViewport(true);page=new LinearLayout(this);page.setOrientation(1);scroll.addView(page);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        switch(tab){case 0->overview();case 1->region();case 2->location();case 3->device();case 4->appearance();case 5->tools();}
        page.addView(text("设置保存在本机。修改后请完全关闭并重新打开 TikTok。",12,MUTED));
    }
    private LinearLayout card(String title,String detail) {
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(16),dp(12),dp(16),dp(12));box.setBackground(round(Color.WHITE));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(12));page.addView(box,lp);
        TextView heading=text(title,19,INK);heading.setTypeface(null,Typeface.BOLD);box.addView(heading);if(detail!=null&&!detail.isEmpty())box.addView(text(detail,13,MUTED));return box;
    }
    private void save() { try{Config.save(this,config);}catch(Exception e){toast(e.getMessage());config=Config.load(this).json;} }
    private void toggle(LinearLayout parent,String key,String label) {
        Switch sw=new Switch(this);sw.setText(label);sw.setTextSize(15);sw.setTextColor(INK);sw.setPadding(0,dp(11),0,dp(11));sw.setChecked(config.optBoolean(key,false));parent.addView(sw);
        sw.setOnCheckedChangeListener((button,checked)->{try{config.put(key,checked);save();}catch(Exception e){toast(e.getMessage());}});
    }
    private void field(LinearLayout parent,String key,String label,boolean numeric) {
        TextView row=text(label+"\n"+config.optString(key,""),15,INK);row.setPadding(0,dp(10),0,dp(10));parent.addView(row);
        row.setOnClickListener(v->{EditText input=new EditText(this);input.setText(config.optString(key,""));input.setSelectAllOnFocus(true);input.setInputType(numeric?InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);new AlertDialog.Builder(this).setTitle(label).setView(input).setNegativeButton("取消",null).setPositiveButton("保存",(d,w)->{try{JSONObject candidate=new JSONObject(config.toString());candidate.put(key,input.getText().toString().trim());if(key.equals("lat")||key.equals("lon"))candidate.put("location_source","custom").put("location_name","自定义坐标");if(key.startsWith("spoof_"))candidate.put("device_preset","自定义");Config.save(this,candidate);config=candidate;int y=scroll.getScrollY();render();scroll.post(()->scroll.scrollTo(0,y));}catch(Exception e){toast(e.getMessage());}}).show();});
    }
    private void button(LinearLayout parent,String label,Runnable action) { Button b=new Button(this);b.setText(label);b.setTextColor(GREEN);parent.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));b.setOnClickListener(v->action.run()); }
    private void toast(String s) { Toast.makeText(this,s==null?"操作失败":s,Toast.LENGTH_LONG).show(); }
    private void overview() {
        LinearLayout c=card("模块开关","在 Vector 中启用 TikTools，并把 TikTok 加入作用域。建议逐组开启功能。");toggle(c,"enabled","启用 TikTools");
        SharedPreferences status=Config.storage(this).getSharedPreferences("status",0);long last=status.getLong("time",0);
        c.addView(text(last==0?"尚未收到目标应用的加载记录":"最近加载："+status.getString("package","")+"\n"+new java.text.SimpleDateFormat("MM-dd HH:mm:ss",java.util.Locale.CHINA).format(new java.util.Date(last)),14,MUTED));
        button(c,"打开 TikTok",()->{Intent i=getPackageManager().getLaunchIntentForPackage("com.ss.android.ugc.trill");if(i==null)i=getPackageManager().getLaunchIntentForPackage("com.zhiliaoapp.musically");if(i==null)toast("未安装 TikTok");else startActivity(i);});
        LinearLayout state=card("独立控制，容易回退","地区、定位、设备信息和界面功能互相独立。原插件请保持停用，避免同时修改相同接口。");
        button(state,"关闭所有功能",()->{try{config.put("enabled",false);save();render();}catch(Exception ignored){}});
        button(state,"查看诊断",()->diagnostic());
    }
    private void region() {
        LinearLayout c=card("地区预设","选择后自动填入运营商、语言、时区和默认城市坐标。功能开关可独立控制。");toggle(c,"region_enabled","SIM / 网络 / 运营商参数");
        LinearLayout quick=new LinearLayout(this);c.addView(quick);
        for(String iso:new String[]{"us","jp","ph","sg"}){Presets.Region r=Presets.find(iso);Button b=new Button(this);b.setText(r.name());b.setTextSize(12);quick.addView(b,new LinearLayout.LayoutParams(0,dp(44),1));b.setOnClickListener(v->applyRegion(r));}
        button(c,"选择国家/地区 · "+config.optString("country_name","菲律宾"),()->{String[] items=new String[Presets.ALL.length];for(int i=0;i<items.length;i++)items[i]=Presets.ALL[i].toString();new AlertDialog.Builder(this).setTitle("地区预设").setItems(items,(d,n)->applyRegion(Presets.ALL[n])).show();});
        c.addView(text(config.optString("op","")+" · "+config.optString("mcc","")+"\n"+config.optString("loc","")+" · "+config.optString("tz",""),14,MUTED));
        LinearLayout manual=advanced(c,"手动调整地区参数");field(manual,"sim","SIM 国家码",false);field(manual,"net","网络国家码",false);field(manual,"mcc","运营商编号 MCC+MNC",false);field(manual,"op","运营商名称",false);
        LinearLayout language=card("语言与时区","跟随所选地区或城市预设填入，可单独开启。");toggle(language,"language_enabled","应用语言环境");toggle(language,"timezone_enabled","应用时区");
        LinearLayout edits=advanced(language,"手动调整语言与时区");field(edits,"loc","语言标签，例如 en-PH",false);field(edits,"tz","时区，例如 Asia/Manila",false);
        LinearLayout ip=card("跟随网络出口","向 ipwho.is 查询出口国家。地图手选或城市预设的坐标会保留。");toggle(ip,"ip_link","IP 国家联动");button(ip,"立即按 IP 更新地区",()->{toast("正在查询网络出口…");io.execute(()->{try{String result=SettingsProvider.refreshIp(this);runOnUiThread(()->{config=Config.load(this).json;render();toast("已选择 "+result);});}catch(Exception e){runOnUiThread(()->toast("查询失败，原配置已保留"));}});});
    }
    private void applyRegion(Presets.Region region){try{region.apply(config);config.put("ip_link",false);save();render();toast("已填入 "+region.name()+" 的配套参数");}catch(Exception e){toast(e.getMessage());}}
    private LinearLayout advanced(LinearLayout parent,String label){Button open=new Button(this);open.setText(label+" ▾");parent.addView(open,new LinearLayout.LayoutParams(-1,dp(44)));LinearLayout child=new LinearLayout(this);child.setOrientation(1);child.setVisibility(View.GONE);parent.addView(child);open.setOnClickListener(v->{boolean show=child.getVisibility()!=View.VISIBLE;child.setVisibility(show?View.VISIBLE:View.GONE);open.setText(label+(show?" ▴":" ▾"));});return child;}
    private void chooseCity(java.util.function.Consumer<Presets.City> action){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(16),0,dp(16),0);EditText search=new EditText(this);search.setHint("搜索城市或国家");search.setSingleLine(true);box.addView(search);ListView list=new ListView(this);box.addView(list,new LinearLayout.LayoutParams(-1,dp(360)));
        java.util.ArrayList<Presets.City> shown=new java.util.ArrayList<>(java.util.Arrays.asList(Presets.CITIES));ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,new java.util.ArrayList<>(shown.stream().map(Object::toString).collect(java.util.stream.Collectors.toList())));list.setAdapter(adapter);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("常用城市预设").setView(box).setNegativeButton("取消",null).create();list.setOnItemClickListener((p,v,n,id)->{action.accept(shown.get(n));dialog.dismiss();});
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){String q=s.toString().trim().toLowerCase(java.util.Locale.ROOT);shown.clear();for(Presets.City city:Presets.CITIES)if((city.toString()+" "+city.iso()).toLowerCase(java.util.Locale.ROOT).contains(q))shown.add(city);adapter.clear();adapter.addAll(shown.stream().map(Object::toString).collect(java.util.stream.Collectors.toList()));adapter.notifyDataSetChanged();}public void afterTextChanged(android.text.Editable e){}});
        dialog.show();dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    private void location() {
        LinearLayout c=card("定位预设","常用城市会同时填入相应地区和时区；地图可精确选择位置。");toggle(c,"location_inject","启用虚拟定位");
        button(c,"地图选点",()->startActivityForResult(new Intent(this,MapActivity.class),20));
        button(c,"选择常用城市",()->chooseCity(city->{try{city.apply(config);config.put("ip_link",false);save();render();}catch(Exception e){toast(e.getMessage());}}));
        c.addView(text(config.optString("location_name",config.optString("country_name","当前位置"))+"\n"+config.optString("lat","")+", "+config.optString("lon",""),15,INK));
        String source=config.optString("location_source","region");c.addView(text(source.equals("region")?"当前坐标跟随地区预设":"当前坐标已固定，IP 国家联动不会覆盖",13,MUTED));
        if(!source.equals("region"))button(c,"恢复跟随地区坐标",()->{Presets.Region region=Presets.find(config.optString("sim","ph"));if(region!=null)applyRegion(region);});
        LinearLayout manual=advanced(c,"手动调整坐标与精度");field(manual,"lat","纬度",true);field(manual,"lon","经度",true);field(manual,"altitude","海拔（米）",true);field(manual,"accuracy","精度（米）",true);toggle(c,"gpsdrift","轻微位置漂移");
        LinearLayout route=card("路径模拟","固定点、圆形行走或自定义路线。");button(route,"模式 · "+config.optString("route_mode","fixed"),()->{String[] labels={"固定坐标","圆形行走","自定义路径往返"};String[] keys={"fixed","circle","route"};new AlertDialog.Builder(this).setTitle("路径模式").setItems(labels,(d,n)->{try{config.put("route_mode",keys[n]);save();render();}catch(Exception ignored){}}).show();});
        LinearLayout routeFields=advanced(route,"调整路径参数");field(routeFields,"route_radius","圆形半径（米）",true);field(routeFields,"route_speed","速度（米/秒）",true);field(routeFields,"route_points","路线坐标：纬度,经度；每行一个点",false);
        LinearLayout favorites=card("坐标收藏","");button(favorites,"收藏当前坐标",()->{try{org.json.JSONArray list=config.optJSONArray("favorites");if(list==null)list=new org.json.JSONArray();list.put(new JSONObject().put("name",config.optString("location_name",config.optString("country_name","坐标"))).put("lat",config.optString("lat")).put("lon",config.optString("lon")));config.put("favorites",list);save();toast("已收藏");}catch(Exception e){toast(e.getMessage());}});
        button(favorites,"打开收藏",()->{org.json.JSONArray list=config.optJSONArray("favorites");if(list==null||list.length()==0){toast("暂无收藏");return;}String[] names=new String[list.length()];for(int i=0;i<names.length;i++){JSONObject n=list.optJSONObject(i);names[i]=n.optString("name")+" · "+n.optString("lat")+", "+n.optString("lon");}new AlertDialog.Builder(this).setTitle("坐标收藏").setItems(names,(d,n)->{try{JSONObject f=list.getJSONObject(n);config.put("lat",f.getString("lat")).put("lon",f.getString("lon")).put("location_source","custom").put("location_name",f.optString("name","收藏坐标"));save();render();}catch(Exception e){toast(e.getMessage());}}).show();});
    }
    private void device() {
        LinearLayout build=card("设备预设","推荐读取本机配套参数。选择预设后，系统版本保持真实值。");toggle(build,"device_spoof","设备信息模拟");
        button(build,"选择设备预设",()->{DevicePresets.Device[] presets=DevicePresets.all();String[] names=java.util.Arrays.stream(presets).map(Object::toString).toArray(String[]::new);new AlertDialog.Builder(this).setTitle("设备预设").setItems(names,(d,n)->{try{presets[n].apply(config);save();render();toast("设备参数已成套填入");}catch(Exception e){toast(e.getMessage());}}).show();});
        build.addView(text(config.optString("device_preset","当前方案")+"\n"+config.optString("spoof_brand","")+" · "+config.optString("spoof_model","")+"\n"+config.optString("spoof_device","")+" · "+config.optString("spoof_hardware",""),14,MUTED));
        LinearLayout details=advanced(build,"手动调整设备字段");String[][] fields={{"spoof_brand","品牌"},{"spoof_manufacturer","制造商"},{"spoof_model","型号"},{"spoof_device","设备代号"},{"spoof_product","产品代号"},{"spoof_board","主板"},{"spoof_hardware","硬件"},{"spoof_fingerprint","Build 指纹（留空则保留）"}};for(String[] f:fields)field(details,f[0],f[1],false);
        LinearLayout cell=card("网络参数","这些是独立测试参数，不代表所选城市的真实基站或 Wi-Fi。");toggle(cell,"cell_spoof","基站参数模拟");toggle(cell,"wifi_spoof","Wi-Fi 参数模拟");LinearLayout network=advanced(cell,"调整基站与 Wi-Fi 参数");field(network,"lac","LAC / TAC",true);field(network,"cid","CID / CI",true);field(network,"pci","PCI",true);field(network,"wifi_ssid","SSID 名称",false);field(network,"wifi_bssid","BSSID",false);
        LinearLayout version=card("系统版本","设备预设使用本机版本。需要兼容性测试时再单独修改。");toggle(version,"version_spoof","模拟系统版本字段");LinearLayout versionFields=advanced(version,"手动调整系统版本");field(versionFields,"spoof_release","Android 版本",false);field(versionFields,"spoof_sdk","SDK 数值",true);field(versionFields,"spoof_patch","安全补丁日期",false);
    }
    private void appearance() {
        LinearLayout c=card("界面净化","采用受限视图匹配；未匹配的界面保持原样。");toggle(c,"ui_enabled","启用界面工具");toggle(c,"show_tool_button","显示应用内工具入口");toggle(c,"fullscreen","全屏显示");
        String[][] toggles={{"purify_topbar","隐藏顶部导航"},{"purify_channel","隐藏频道入口"},{"purify_tabbar","隐藏底部导航"},{"purify_sidebtn","隐藏侧边操作按钮"},{"purify_author","隐藏作者信息"},{"purify_desc","隐藏视频文案"},{"purify_music","隐藏音乐信息"},{"purify_logo","隐藏平台角标"},{"purify_location","隐藏地点标签"},{"purify_translate","隐藏原生翻译提示"},{"purify_paywall","隐藏推广与付费提示"}};for(String[] t:toggles)toggle(c,t[0],t[1]);
        LinearLayout comments=card("评论过滤","仅过滤已加载且能识别的评论视图。");toggle(comments,"comment_filter","关键词过滤");field(comments,"filter_keywords","关键词：每行一个",false);
        LinearLayout download=card("下载控制","开启原生下载选项适配，并在工具入口提供已识别直链视频的保存功能。");toggle(download,"download_restrictions","启用下载控制");
        LinearLayout custom=card("界面适配","TikTok 更新后可以导入新的资源匹配规则。诊断中会显示匹配情况。");field(custom,"custom_selectors","自定义视图规则 JSON",false);
    }
    private void tools() {
        LinearLayout translate=card("本机翻译","首次使用需通过 Wi-Fi 下载语言模型，文本在本机处理。");toggle(translate,"trans","翻译视频文案");field(translate,"translation_source","源语言：auto / en / ja / …",false);field(translate,"translang","目标语言：zh / en / …",false);
        button(translate,"下载模型并测试翻译",()->{EditText t=new EditText(this);t.setText("Hello, welcome to the toolbox.");new AlertDialog.Builder(this).setTitle("翻译测试").setView(t).setNegativeButton("取消",null).setPositiveButton("翻译",(d,w)->{toast("正在准备本机模型…");String input=t.getText().toString();io.execute(()->{try{String result=TranslationBroker.translate(input,config.optString("translation_source","auto"),config.optString("translang","zh"),true);runOnUiThread(()->new AlertDialog.Builder(this).setTitle("翻译结果").setMessage(result).setPositiveButton("确定",null).show());}catch(Exception e){runOnUiThread(()->toast("翻译未完成："+e.getMessage()));}});}).show();});
        LinearLayout backup=card("配置备份","");button(backup,"导出配置",()->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").putExtra(Intent.EXTRA_TITLE,"TikTools-config.json").addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,10);});button(backup,"导入配置",()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE),11));button(backup,"恢复默认设置",()->new AlertDialog.Builder(this).setTitle("恢复默认设置").setMessage("将关闭模块功能并恢复预设参数，TikTok 数据不受影响。").setNegativeButton("取消",null).setPositiveButton("恢复",(d,w)->{config=Config.defaults();save();render();}).show());
        LinearLayout icon=card("桌面入口","隐藏后通过 TikTok 内的 TK 按钮打开设置。先确认模块已在 TikTok 中正常加载。");
        Button hide=new Button(this);hide.setText(config.optBoolean("hide_icon",false)?"恢复桌面图标":"隐藏桌面图标");icon.addView(hide);hide.setOnClickListener(v->{
            boolean next=!config.optBoolean("hide_icon",false);long seen=Config.storage(this).getSharedPreferences("status",0).getLong("time",0);
            if(next&&(!config.optBoolean("enabled",false)||seen==0)){toast("请先启用模块并确认 TikTok 中的 TK 入口可用");return;}
            try{config.put("hide_icon",next);if(next)config.put("ui_enabled",true).put("show_tool_button",true);save();getPackageManager().setComponentEnabledSetting(new android.content.ComponentName(this,"io.github.sunisup666.tiktools.LauncherAlias"),next?android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED:android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,android.content.pm.PackageManager.DONT_KILL_APP);render();}catch(Exception e){toast(e.getMessage());}
        });
        LinearLayout info=card("诊断与版本","TikTools "+BuildConfig.VERSION_NAME+" · 本机独立构建");button(info,"查看诊断",this::diagnostic);
        button(info,"GitHub",()->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/sunisup666/TikTools"))));
        button(info,"隐私说明",()->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/sunisup666/TikTools/blob/main/docs/PRIVACY.md"))));
    }
    private void diagnostic() {
        String raw=Config.storage(this).getSharedPreferences("status",0).getString("report","尚无加载记录。请在 Vector 中启用模块、勾选 TikTok 后重新打开应用。");
        try{raw=new JSONObject(raw).toString(2);}catch(Exception ignored){}
        TextView t=text(raw,12,INK);t.setTextIsSelectable(true);t.setPadding(dp(16),dp(8),dp(16),dp(8));ScrollView s=new ScrollView(this);s.addView(t);new AlertDialog.Builder(this).setTitle("本机诊断").setView(s).setPositiveButton("关闭",null).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==20){
            if(result==RESULT_OK&&data!=null)try{
                JSONObject selected=Config.load(this).json;
                String iso=data.getStringExtra("city_iso");if(iso!=null){selected.put("ip_link",false);Presets.Region region=Presets.find(iso);if(region!=null)region.apply(selected,false);selected.put("tz",data.getStringExtra("city_zone")).put("location_country",iso).put("location_timezone",data.getStringExtra("city_zone"));}
                selected.put("lat",Double.toString(data.getDoubleExtra("lat",0))).put("lon",Double.toString(data.getDoubleExtra("lon",0))).put("location_source","custom").put("location_name",data.getStringExtra("city_name")==null?"地图选点":data.getStringExtra("city_name")).put("location_inject",true);
                Config.save(this,selected);config=selected;render();toast("已保存地图选点");
            }catch(Exception e){toast("未更改配置："+e.getMessage());}
            return;
        }
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        try {
            if(request==10){try(var out=getContentResolver().openOutputStream(data.getData())){out.write(config.toString(2).getBytes(StandardCharsets.UTF_8));}toast("配置已导出");}
            if(request==11){try(var in=getContentResolver().openInputStream(data.getData())){byte[] bytes=in.readNBytes(65537);if(bytes.length>65536)throw new IllegalArgumentException("配置文件过大");JSONObject imported=new JSONObject(new String(bytes,StandardCharsets.UTF_8));JSONObject merged=Config.defaults();for(Iterator<String> i=imported.keys();i.hasNext();){String key=i.next();merged.put(key,imported.get(key));}Config.save(this,merged);config=merged;render();toast("配置已导入，请重新打开 TikTok");}}
        }catch(Exception e){toast("未更改配置："+e.getMessage());}
    }
    @Override protected void onResume(){super.onResume();if(config!=null){JSONObject latest=Config.load(this).json;if(!latest.toString().equals(config.toString())){config=latest;render();}}}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putInt("tab",tab);}
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();}
}
