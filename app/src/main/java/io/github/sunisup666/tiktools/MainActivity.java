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
    private static final int[] TABS={R.string.tab_overview,R.string.tab_region,R.string.tab_location,R.string.tab_device,R.string.tab_interface,R.string.tab_tools};
    private String tr(int id,Object... args){return args.length==0?getString(id):getString(id,args);}
    @Override public void onCreate(Bundle state) { super.onCreate(state);config=Config.load(this).json;if(state!=null)tab=state.getInt("tab",0);render(); }
    private int dp(float n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private void ui(Runnable action){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())action.run();});}
    private TextView text(String value,int size,int color) { TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(5),0,dp(5));return t; }
    private GradientDrawable round(int color) { GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(18));return d; }
    private void render() {
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(dp(18),i.top+dp(10),dp(18),i.bottom+dp(8));return insets;});
        TextView title=text("TikTools",30,INK);title.setTypeface(null,Typeface.BOLD);root.addView(title);
        root.addView(text(tr(R.string.tagline),14,MUTED));
        HorizontalScrollView navScroll=new HorizontalScrollView(this);navScroll.setHorizontalScrollBarEnabled(false);LinearLayout nav=new LinearLayout(this);
        for(int n=0;n<TABS.length;n++){final int index=n;Button b=new Button(this);b.setText(TABS[n]);b.setAllCaps(false);b.setTextSize(14);b.setMinWidth(dp(66));b.setPadding(dp(12),0,dp(12),0);b.setTextColor(n==tab?Color.WHITE:GREEN);b.setBackground(round(n==tab?GREEN:0xffe0eae6));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(42));lp.setMargins(0,dp(8),dp(6),dp(12));nav.addView(b,lp);b.setOnClickListener(v->{tab=index;render();});}
        navScroll.addView(nav);root.addView(navScroll);navScroll.post(()->navScroll.scrollTo(Math.max(0,nav.getChildAt(tab).getRight()-navScroll.getWidth()),0));
        scroll=new ScrollView(this);scroll.setFillViewport(true);page=new LinearLayout(this);page.setOrientation(1);scroll.addView(page);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        switch(tab){case 0->overview();case 1->region();case 2->location();case 3->device();case 4->appearance();case 5->tools();}
        page.addView(text(tr(R.string.settings_footer),12,MUTED));
    }
    private LinearLayout card(String title,String detail) {
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(16),dp(12),dp(16),dp(12));box.setBackground(round(Color.WHITE));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(12));page.addView(box,lp);
        TextView heading=text(title,19,INK);heading.setTypeface(null,Typeface.BOLD);box.addView(heading);if(detail!=null&&!detail.isEmpty())box.addView(text(detail,13,MUTED));return box;
    }
    private void save() { try{Config.save(this,config);}catch(Exception e){toast(UiText.error(this,e));config=Config.load(this).json;} }
    private void toggle(LinearLayout parent,String key,String label) {
        Switch sw=new Switch(this);sw.setText(label);sw.setTextSize(15);sw.setTextColor(INK);sw.setPadding(0,dp(11),0,dp(11));sw.setChecked(config.optBoolean(key,false));parent.addView(sw);
        sw.setOnCheckedChangeListener((button,checked)->{try{config.put(key,checked);save();}catch(Exception e){toast(UiText.error(this,e));}});
    }
    private void field(LinearLayout parent,String key,String label,boolean numeric) {
        TextView row=text(label+"\n"+config.optString(key,""),15,INK);row.setPadding(0,dp(10),0,dp(10));parent.addView(row);
        row.setOnClickListener(v->{EditText input=new EditText(this);input.setText(config.optString(key,""));input.setSelectAllOnFocus(true);input.setInputType(numeric?InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);new AlertDialog.Builder(this).setTitle(label).setView(input).setNegativeButton(tr(R.string.cancel),null).setPositiveButton(tr(R.string.save),(d,w)->{try{JSONObject candidate=new JSONObject(config.toString());candidate.put(key,input.getText().toString().trim());if(key.equals("lat")||key.equals("lon"))candidate.put("location_source","custom").put("location_name",UiText.CUSTOM_COORDINATES);if(key.startsWith("spoof_"))candidate.put("device_preset",UiText.CUSTOM_DEVICE);Config.save(this,candidate);config=candidate;int y=scroll.getScrollY();render();scroll.post(()->scroll.scrollTo(0,y));}catch(Exception e){toast(UiText.error(this,e));}}).show();});
    }
    private void button(LinearLayout parent,String label,Runnable action) { Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setMinHeight(dp(48));b.setTextColor(GREEN);parent.addView(b,new LinearLayout.LayoutParams(-1,-2));b.setOnClickListener(v->action.run()); }
    private void toast(String s) { Toast.makeText(this,s==null?tr(R.string.operation_failed):s,Toast.LENGTH_LONG).show(); }
    private void overview() {
        appLanguage();
        LinearLayout c=card(tr(R.string.module_switch),tr(R.string.module_help));toggle(c,"enabled",tr(R.string.enable_module));
        SharedPreferences status=Config.storage(this).getSharedPreferences("status",0);long last=status.getLong("time",0);
        c.addView(text(last==0?tr(R.string.not_loaded):tr(R.string.last_loaded_prefix)+status.getString("package","")+"\n"+new java.text.SimpleDateFormat("MM-dd HH:mm:ss",java.util.Locale.CHINA).format(new java.util.Date(last)),14,MUTED));
        button(c,tr(R.string.open_tiktok),()->{Intent i=getPackageManager().getLaunchIntentForPackage("com.ss.android.ugc.trill");if(i==null)i=getPackageManager().getLaunchIntentForPackage("com.zhiliaoapp.musically");if(i==null)toast(tr(R.string.tiktok_not_installed));else startActivity(i);});
        LinearLayout state=card(tr(R.string.independent_controls),tr(R.string.independent_help));
        button(state,tr(R.string.disable_all),()->{try{config.put("enabled",false);save();render();}catch(Exception ignored){}});
        button(state,tr(R.string.view_diagnostics),()->diagnostic());
    }
    private void region() {
        LinearLayout c=card(tr(R.string.region_presets),tr(R.string.region_help));toggle(c,"region_enabled",tr(R.string.region_parameters));
        LinearLayout quick=new LinearLayout(this);c.addView(quick);
        for(String iso:new String[]{"us","jp","ph","sg"}){Presets.Region r=Presets.find(iso);Button b=new Button(this);b.setText(r.displayName(this));b.setAllCaps(false);b.setTextSize(12);b.setMinHeight(dp(44));b.setPadding(dp(4),dp(6),dp(4),dp(6));quick.addView(b,new LinearLayout.LayoutParams(0,-2,1));b.setOnClickListener(v->applyRegion(r));}
        button(c,tr(R.string.choose_region_prefix)+UiText.label(this,config.optString("country_name",Presets.find("ph").name())),()->{String[] items=new String[Presets.ALL.length];for(int i=0;i<items.length;i++)items[i]=Presets.ALL[i].label(this);new AlertDialog.Builder(this).setTitle(tr(R.string.region_presets)).setItems(items,(d,n)->applyRegion(Presets.ALL[n])).show();});
        c.addView(text(config.optString("op","")+" · "+config.optString("mcc","")+"\n"+config.optString("loc","")+" · "+config.optString("tz",""),14,MUTED));
        LinearLayout manual=advanced(c,tr(R.string.edit_region));field(manual,"sim",tr(R.string.sim_country),false);field(manual,"net",tr(R.string.network_country),false);field(manual,"mcc",tr(R.string.carrier_code),false);field(manual,"op",tr(R.string.carrier_name),false);
        LinearLayout language=card(tr(R.string.language_timezone),tr(R.string.language_timezone_help));toggle(language,"language_enabled",tr(R.string.override_language));toggle(language,"timezone_enabled",tr(R.string.override_timezone));
        LinearLayout edits=advanced(language,tr(R.string.edit_language_timezone));field(edits,"loc",tr(R.string.locale_tag),false);field(edits,"tz",tr(R.string.timezone_id),false);
        LinearLayout ip=card(tr(R.string.network_region),tr(R.string.network_region_help));toggle(ip,"ip_link",tr(R.string.ip_link));button(ip,tr(R.string.refresh_ip),()->{toast(tr(R.string.querying_ip));io.execute(()->{try{String result=SettingsProvider.refreshIp(this);ui(()->{config=Config.load(this).json;render();toast(tr(R.string.selected_prefix)+UiText.label(this,result));});}catch(Exception e){ui(()->toast(tr(R.string.ip_query_failed)));}});});
    }
    private void applyRegion(Presets.Region region){try{region.apply(config);config.put("ip_link",false);save();render();toast(tr(R.string.preset_applied_prefix)+region.displayName(this)+tr(R.string.preset_applied_suffix));}catch(Exception e){toast(UiText.error(this,e));}}
    private LinearLayout advanced(LinearLayout parent,String label){Button open=new Button(this);open.setText(label+" ▾");open.setAllCaps(false);open.setMinHeight(dp(44));parent.addView(open,new LinearLayout.LayoutParams(-1,-2));LinearLayout child=new LinearLayout(this);child.setOrientation(1);child.setVisibility(View.GONE);parent.addView(child);open.setOnClickListener(v->{boolean show=child.getVisibility()!=View.VISIBLE;child.setVisibility(show?View.VISIBLE:View.GONE);open.setText(label+(show?" ▴":" ▾"));});return child;}
    private void chooseCity(java.util.function.Consumer<Presets.City> action){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(16),0,dp(16),0);EditText search=new EditText(this);search.setHint(tr(R.string.search_city));search.setSingleLine(true);box.addView(search);ListView list=new ListView(this);box.addView(list,new LinearLayout.LayoutParams(-1,dp(360)));
        android.content.Context english=UiText.english(this);java.util.ArrayList<Presets.City> shown=new java.util.ArrayList<>(java.util.Arrays.asList(Presets.CITIES));ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,new java.util.ArrayList<>(shown.stream().map(city->city.label(MainActivity.this)).collect(java.util.stream.Collectors.toList())));list.setAdapter(adapter);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(tr(R.string.city_presets)).setView(box).setNegativeButton(tr(R.string.cancel),null).create();list.setOnItemClickListener((p,v,n,id)->{action.accept(shown.get(n));dialog.dismiss();});
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){String q=s.toString().trim().toLowerCase(java.util.Locale.ROOT);shown.clear();for(Presets.City city:Presets.CITIES)if((city.toString()+" "+city.label(english)+" "+city.iso()).toLowerCase(java.util.Locale.ROOT).contains(q))shown.add(city);adapter.clear();adapter.addAll(shown.stream().map(city->city.label(MainActivity.this)).collect(java.util.stream.Collectors.toList()));adapter.notifyDataSetChanged();}public void afterTextChanged(android.text.Editable e){}});
        dialog.show();dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    private void location() {
        LinearLayout c=card(tr(R.string.location_presets),tr(R.string.location_help));toggle(c,"location_inject",tr(R.string.enable_location));
        button(c,tr(R.string.map_picker),()->startActivityForResult(new Intent(this,MapActivity.class),20));
        button(c,tr(R.string.choose_city),()->chooseCity(city->{try{city.apply(config);config.put("ip_link",false);save();render();}catch(Exception e){toast(UiText.error(this,e));}}));
        c.addView(text(UiText.label(this,config.optString("location_name",config.optString("country_name",tr(R.string.current_location))))+"\n"+config.optString("lat","")+", "+config.optString("lon",""),15,INK));
        String source=config.optString("location_source","region");c.addView(text(source.equals("region")?tr(R.string.location_follows_region):tr(R.string.location_fixed),13,MUTED));
        if(!source.equals("region"))button(c,tr(R.string.restore_region_coordinates),()->{Presets.Region region=Presets.find(config.optString("sim","ph"));if(region!=null)applyRegion(region);});
        LinearLayout manual=advanced(c,tr(R.string.edit_coordinates));field(manual,"lat",tr(R.string.latitude),true);field(manual,"lon",tr(R.string.longitude),true);field(manual,"altitude",tr(R.string.altitude),true);field(manual,"accuracy",tr(R.string.accuracy),true);toggle(c,"gpsdrift",tr(R.string.location_drift));
        LinearLayout route=card(tr(R.string.route_simulation),tr(R.string.route_help));button(route,tr(R.string.route_mode_prefix)+UiText.route(this,config.optString("route_mode","fixed")),()->{String[] labels={tr(R.string.route_fixed),tr(R.string.route_circle),tr(R.string.route_custom)};String[] keys={"fixed","circle","route"};new AlertDialog.Builder(this).setTitle(tr(R.string.route_mode)).setItems(labels,(d,n)->{try{config.put("route_mode",keys[n]);save();render();}catch(Exception ignored){}}).show();});
        LinearLayout routeFields=advanced(route,tr(R.string.edit_route));field(routeFields,"route_radius",tr(R.string.route_radius),true);field(routeFields,"route_speed",tr(R.string.route_speed),true);field(routeFields,"route_points",tr(R.string.route_points),false);
        LinearLayout favorites=card(tr(R.string.coordinate_favorites),"");button(favorites,tr(R.string.save_coordinates),()->{try{org.json.JSONArray list=config.optJSONArray("favorites");if(list==null)list=new org.json.JSONArray();list.put(new JSONObject().put("name",config.optString("location_name",config.optString("country_name",UiText.COORDINATES))).put("lat",config.optString("lat")).put("lon",config.optString("lon")));config.put("favorites",list);save();toast(tr(R.string.favorite_saved));}catch(Exception e){toast(UiText.error(this,e));}});
        button(favorites,tr(R.string.open_favorites),()->{org.json.JSONArray list=config.optJSONArray("favorites");if(list==null||list.length()==0){toast(tr(R.string.no_favorites));return;}String[] names=new String[list.length()];for(int i=0;i<names.length;i++){JSONObject n=list.optJSONObject(i);names[i]=UiText.label(this,n.optString("name"))+" · "+n.optString("lat")+", "+n.optString("lon");}new AlertDialog.Builder(this).setTitle(tr(R.string.coordinate_favorites)).setItems(names,(d,n)->{try{JSONObject f=list.getJSONObject(n);config.put("lat",f.getString("lat")).put("lon",f.getString("lon")).put("location_source","custom").put("location_name",f.optString("name",UiText.FAVORITE_COORDINATES));save();render();}catch(Exception e){toast(UiText.error(this,e));}}).show();});
    }
    private void device() {
        LinearLayout build=card(tr(R.string.device_presets),tr(R.string.device_help));toggle(build,"device_spoof",tr(R.string.device_spoof));
        button(build,tr(R.string.choose_device),()->{DevicePresets.Device[] presets=DevicePresets.all();String[] names=java.util.Arrays.stream(presets).map(preset->preset.label(this)).toArray(String[]::new);new AlertDialog.Builder(this).setTitle(tr(R.string.device_presets)).setItems(names,(d,n)->{try{presets[n].apply(config);save();render();toast(tr(R.string.device_applied));}catch(Exception e){toast(UiText.error(this,e));}}).show();});
        build.addView(text(UiText.label(this,config.optString("device_preset",tr(R.string.current_profile)))+"\n"+config.optString("spoof_brand","")+" · "+config.optString("spoof_model","")+"\n"+config.optString("spoof_device","")+" · "+config.optString("spoof_hardware",""),14,MUTED));
        LinearLayout details=advanced(build,tr(R.string.edit_device));String[][] fields={{"spoof_brand",tr(R.string.brand)},{"spoof_manufacturer",tr(R.string.manufacturer)},{"spoof_model",tr(R.string.model)},{"spoof_device",tr(R.string.device_codename)},{"spoof_product",tr(R.string.product_codename)},{"spoof_board",tr(R.string.board)},{"spoof_hardware",tr(R.string.hardware)},{"spoof_fingerprint",tr(R.string.fingerprint)}};for(String[] f:fields)field(details,f[0],f[1],false);
        LinearLayout cell=card(tr(R.string.network_parameters),tr(R.string.network_help));toggle(cell,"cell_spoof",tr(R.string.cell_spoof));toggle(cell,"wifi_spoof",tr(R.string.wifi_spoof));LinearLayout network=advanced(cell,tr(R.string.edit_network));field(network,"lac","LAC / TAC",true);field(network,"cid","CID / CI",true);field(network,"pci","PCI",true);field(network,"wifi_ssid",tr(R.string.ssid_name),false);field(network,"wifi_bssid","BSSID",false);
        LinearLayout version=card(tr(R.string.system_version),tr(R.string.system_version_help));toggle(version,"version_spoof",tr(R.string.version_spoof));LinearLayout versionFields=advanced(version,tr(R.string.edit_version));field(versionFields,"spoof_release",tr(R.string.android_version),false);field(versionFields,"spoof_sdk",tr(R.string.sdk_value),true);field(versionFields,"spoof_patch",tr(R.string.security_patch),false);
    }
    private void appearance() {
        LinearLayout c=card(tr(R.string.interface_rules),tr(R.string.interface_help));toggle(c,"ui_enabled",tr(R.string.enable_interface));toggle(c,"show_tool_button",tr(R.string.show_tool_button));toggle(c,"fullscreen",tr(R.string.fullscreen));
        String[][] toggles={{"purify_topbar",tr(R.string.hide_topbar)},{"purify_channel",tr(R.string.hide_channel)},{"purify_tabbar",tr(R.string.hide_tabbar)},{"purify_sidebtn",tr(R.string.hide_sidebtn)},{"purify_author",tr(R.string.hide_author)},{"purify_desc",tr(R.string.hide_caption)},{"purify_music",tr(R.string.hide_music)},{"purify_logo",tr(R.string.hide_logo)},{"purify_location",tr(R.string.hide_location)},{"purify_translate",tr(R.string.hide_translation)},{"purify_paywall",tr(R.string.hide_promotion)}};for(String[] t:toggles)toggle(c,t[0],t[1]);
        LinearLayout comments=card(tr(R.string.comment_filter),tr(R.string.comment_filter_help));toggle(comments,"comment_filter",tr(R.string.keyword_filter));field(comments,"filter_keywords",tr(R.string.filter_keywords),false);
        LinearLayout download=card(tr(R.string.download_controls),tr(R.string.download_help));toggle(download,"download_restrictions",tr(R.string.enable_download));
        LinearLayout custom=card(tr(R.string.view_matching),tr(R.string.view_matching_help));field(custom,"custom_selectors",tr(R.string.custom_selectors),false);
    }
    private void tools() {
        LinearLayout translate=card(tr(R.string.local_translation),tr(R.string.translation_help));toggle(translate,"trans",tr(R.string.translate_captions));field(translate,"translation_source",tr(R.string.translation_source),false);field(translate,"translang",tr(R.string.translation_target),false);
        button(translate,tr(R.string.download_models_test),()->{EditText t=new EditText(this);t.setText("Hello, welcome to the toolbox.");new AlertDialog.Builder(this).setTitle(tr(R.string.translation_test)).setView(t).setNegativeButton(tr(R.string.cancel),null).setPositiveButton(tr(R.string.translate),(d,w)->{toast(tr(R.string.preparing_models));String input=t.getText().toString();io.execute(()->{try{String result=TranslationBroker.translate(input,config.optString("translation_source","auto"),config.optString("translang","zh"),true);ui(()->new AlertDialog.Builder(this).setTitle(tr(R.string.translation_result)).setMessage(result).setPositiveButton(tr(R.string.ok),null).show());}catch(Exception e){ui(()->toast(tr(R.string.translation_failed_prefix)+UiText.error(this,e)));}});}).show();});
        LinearLayout backup=card(tr(R.string.config_backup),"");button(backup,tr(R.string.export_config),()->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").putExtra(Intent.EXTRA_TITLE,"TikTools-config.json").addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,10);});button(backup,tr(R.string.import_config),()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE),11));button(backup,tr(R.string.restore_defaults),()->new AlertDialog.Builder(this).setTitle(tr(R.string.restore_defaults)).setMessage(tr(R.string.restore_defaults_help)).setNegativeButton(tr(R.string.cancel),null).setPositiveButton(tr(R.string.restore),(d,w)->{config=Config.defaults();save();render();}).show());
        LinearLayout icon=card(tr(R.string.launcher_entry),tr(R.string.launcher_help));
        Button hide=new Button(this);hide.setText(config.optBoolean("hide_icon",false)?tr(R.string.restore_icon):tr(R.string.hide_icon));icon.addView(hide);hide.setOnClickListener(v->{
            boolean next=!config.optBoolean("hide_icon",false);long seen=Config.storage(this).getSharedPreferences("status",0).getLong("time",0);
            if(next&&(!config.optBoolean("enabled",false)||seen==0)){toast(tr(R.string.check_tools_first));return;}
            try{config.put("hide_icon",next);if(next)config.put("ui_enabled",true).put("show_tool_button",true);save();getPackageManager().setComponentEnabledSetting(new android.content.ComponentName(this,"io.github.sunisup666.tiktools.LauncherAlias"),next?android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED:android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,android.content.pm.PackageManager.DONT_KILL_APP);render();}catch(Exception e){toast(UiText.error(this,e));}
        });
        LinearLayout info=card(tr(R.string.diagnostics_version),"TikTools "+BuildConfig.VERSION_NAME);button(info,tr(R.string.view_diagnostics),this::diagnostic);
        button(info,"GitHub",()->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/sunisup666/TikTools"))));
        button(info,tr(R.string.privacy),()->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/sunisup666/TikTools/blob/main/docs/PRIVACY.md"))));
    }
    private void appLanguage(){
        android.app.LocaleManager manager=getSystemService(android.app.LocaleManager.class);
        android.os.LocaleList locales=manager.getApplicationLocales();
        int selected=locales.isEmpty()?0:locales.get(0).getLanguage().equals("zh")?2:1;
        String[] labels={tr(R.string.language_system),tr(R.string.language_english),tr(R.string.language_chinese)};
        LinearLayout box=card(tr(R.string.app_language),tr(R.string.app_language_help));
        button(box,tr(R.string.choose_ui_language)+" · "+labels[selected],()->new AlertDialog.Builder(this)
            .setTitle(tr(R.string.app_language)).setSingleChoiceItems(labels,selected,(dialog,index)->{
                dialog.dismiss();manager.setApplicationLocales(index==0?android.os.LocaleList.getEmptyLocaleList():android.os.LocaleList.forLanguageTags(index==1?"en":"zh-Hans"));
            }).setNegativeButton(tr(R.string.cancel),null).show());
    }
    private void diagnostic() {
        String raw=Config.storage(this).getSharedPreferences("status",0).getString("report",tr(R.string.no_report));
        try{raw=new JSONObject(raw).toString(2);}catch(Exception ignored){}
        TextView t=text(raw,12,INK);t.setTextIsSelectable(true);t.setPadding(dp(16),dp(8),dp(16),dp(8));ScrollView s=new ScrollView(this);s.addView(t);new AlertDialog.Builder(this).setTitle(tr(R.string.local_diagnostics)).setView(s).setPositiveButton(tr(R.string.close),null).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==20){
            if(result==RESULT_OK&&data!=null)try{
                JSONObject selected=Config.load(this).json;
                String iso=data.getStringExtra("city_iso");if(iso!=null){selected.put("ip_link",false);Presets.Region region=Presets.find(iso);if(region!=null)region.apply(selected,false);selected.put("tz",data.getStringExtra("city_zone")).put("location_country",iso).put("location_timezone",data.getStringExtra("city_zone"));}
                selected.put("lat",Double.toString(data.getDoubleExtra("lat",0))).put("lon",Double.toString(data.getDoubleExtra("lon",0))).put("location_source","custom").put("location_name",data.getStringExtra("city_name")==null?UiText.MAP_POINT:data.getStringExtra("city_name")).put("location_inject",true);
                Config.save(this,selected);config=selected;render();toast(tr(R.string.map_saved));
            }catch(Exception e){toast(tr(R.string.settings_unchanged_prefix)+UiText.error(this,e));}
            return;
        }
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        try {
            if(request==10){try(var out=getContentResolver().openOutputStream(data.getData())){out.write(config.toString(2).getBytes(StandardCharsets.UTF_8));}toast(tr(R.string.config_exported));}
            if(request==11){try(var in=getContentResolver().openInputStream(data.getData())){byte[] bytes=in.readNBytes(65537);if(bytes.length>65536)throw new IllegalArgumentException(tr(R.string.config_too_large));JSONObject imported=new JSONObject(new String(bytes,StandardCharsets.UTF_8));JSONObject merged=Config.defaults();for(Iterator<String> i=imported.keys();i.hasNext();){String key=i.next();merged.put(key,imported.get(key));}Config.save(this,merged);config=merged;render();toast(tr(R.string.config_imported));}}
        }catch(Exception e){toast(tr(R.string.settings_unchanged_prefix)+UiText.error(this,e));}
    }
    @Override protected void onResume(){super.onResume();if(config!=null){JSONObject latest=Config.load(this).json;if(!latest.toString().equals(config.toString())){config=latest;render();}}}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putInt("tab",tab);}
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();}
}
