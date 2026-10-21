package io.github.sunisup666.tiktools.probe;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Base64;
import android.util.Xml;
import android.view.accessibility.AccessibilityNodeInfo;
import org.xmlpull.v1.XmlSerializer;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/** A one-shot ADB instrumentation snapshot; never waits for a continuously playing feed to idle. */
public final class InspectInstrumentation extends Instrumentation {
    private int budget=1800;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            UiAutomation automation=getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            AccessibilityServiceInfo service=automation.getServiceInfo();
            service.flags|=AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS|AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
            automation.setServiceInfo(service);
            AccessibilityNodeInfo root=null;
            for(int i=0;i<10&&root==null;i++){root=automation.getRootInActiveWindow();if(root==null)Thread.sleep(100);}
            if(root==null)throw new IllegalStateException("No active window");
            StringWriter output=new StringWriter();XmlSerializer xml=Xml.newSerializer();xml.setOutput(output);xml.startDocument("UTF-8",true);xml.startTag(null,"hierarchy");node(root,xml,0);xml.endTag(null,"hierarchy");xml.endDocument();
            result.putString("xml_b64",Base64.encodeToString(output.toString().getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP));
            finish(-1,result);
        }catch(Throwable e){result.putString("error",e.getClass().getSimpleName()+": "+e.getMessage());finish(0,result);}
    }
    private static String text(CharSequence value){if(value==null)return "";String s=value.toString();return s.replaceAll("[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f]","");}
    private void node(AccessibilityNodeInfo view,XmlSerializer xml,int depth)throws Exception{
        if(view==null||budget--<=0||depth>60)return;
        try{
            if(!view.isVisibleToUser())return;
            xml.startTag(null,"node");
            xml.attribute(null,"package",text(view.getPackageName()));xml.attribute(null,"class",text(view.getClassName()));
            xml.attribute(null,"resource-id",text(view.getViewIdResourceName()));xml.attribute(null,"text",text(view.getText()));xml.attribute(null,"content-desc",text(view.getContentDescription()));
            xml.attribute(null,"clickable",String.valueOf(view.isClickable()));xml.attribute(null,"enabled",String.valueOf(view.isEnabled()));
            Rect r=new Rect();view.getBoundsInScreen(r);xml.attribute(null,"bounds","["+r.left+","+r.top+"]["+r.right+","+r.bottom+"]");
            for(int i=0;i<view.getChildCount();i++)node(view.getChild(i),xml,depth+1);
            xml.endTag(null,"node");
        }finally{view.recycle();}
    }
}
