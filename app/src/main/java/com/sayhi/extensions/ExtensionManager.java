package com.sayhi.extensions;
import android.content.*;import android.webkit.*;import java.io.*;import java.util.*;import java.util.regex.*;import java.util.zip.*;

public class ExtensionManager {
    final File root; final android.content.Context ctx; final ArrayList<Ext> exts=new ArrayList<>();
    ExtensionManager(Context c){ctx=c;root=new File(c.getFilesDir(),"extensions");root.mkdirs();load();}
    static class Ext{String id,name;File dir;ArrayList<String> js=new ArrayList<>(),css=new ArrayList<>();ArrayList<String> matches=new ArrayList<>();}
    ArrayList<String> list(){ArrayList<String> x=new ArrayList<>();for(Ext e:exts)x.add(e.name);return x;}
    void load(){File[] fs=root.listFiles();if(fs==null)return;for(File f:fs)if(new File(f,"manifest.json").exists())try{parse(f);}catch(Exception ignored){}}
    void install(android.net.Uri uri)throws Exception{String id="ext_"+System.currentTimeMillis();File d=new File(root,id);d.mkdirs();ZipInputStream z=new ZipInputStream(new FileInputStream(copyToTemp(uri)));ZipEntry e;while((e=z.getNextEntry())!=null){String n=e.getName();if(n.contains(".."))continue;File out=new File(d,n);if(e.isDirectory()){out.mkdirs();continue;}File p=out.getParentFile();if(p!=null)p.mkdirs();FileOutputStream o=new FileOutputStream(out);byte[] buf=new byte[8192];int k;while((k=z.read(buf))>0)o.write(buf,0,k);o.close();}z.close();parse(d);}
    File copyToTemp(android.net.Uri u)throws Exception{File t=new File(root,"_install.zip");InputStream i=ctx.getContentResolver().openInputStream(u);FileOutputStream o=new FileOutputStream(t);byte[] b=new byte[8192];int n;while((n=i.read(b))>0)o.write(b,0,n);i.close();o.close();return t;}
    void parse(File d)throws Exception{String m=read(new File(d,"manifest.json"));Ext e=new Ext();e.id=d.getName();e.dir=d;e.name=val(m,"name");String perms=val(m,"content_scripts");if(perms!=null){Matcher q=Pattern.compile("\\{(.*?)\\}",Pattern.DOTALL).matcher(perms);while(q.find()){String o=q.group(1);String ms=val(o,"matches"), js=val(o,"js"), css=val(o,"css");if(ms!=null)for(String s:arr(ms))e.matches.add(s);if(js!=null)for(String s:arr(js))e.js.add(s);if(css!=null)for(String s:arr(css))e.css.add(s);}}exts.removeIf(x->x.id.equals(e.id));exts.add(e);}
    void inject(WebView w,String u){for(Ext e:exts){if(!match(e.matches,u))continue;for(String f:e.css){String s=read(new File(e.dir,f));String esc=android.webkit.WebView.class.getName();w.evaluateJavascript("(function(){var s=document.createElement('style');s.textContent="+js(s)+";document.documentElement.appendChild(s);})()",null);}for(String f:e.js){String s=read(new File(e.dir,f));w.evaluateJavascript("(function(){"+s+"\n})()",null);}}}
    boolean match(ArrayList<String> p,String u){if(p.isEmpty())return false;for(String x:p){String r=Pattern.quote(x).replace("\\Q*\\E",".*");if(u.matches(r))return true;if(x.equals("<all_urls>"))return true;}return false;}
    static String js(String s){return "'"+s.replace("\\","\\\\").replace("'","\\'").replace("\n","\\n").replace("\r","")+"'";}
    static String read(File f)throws Exception{if(!f.exists())return "";ByteArrayOutputStream b=new ByteArrayOutputStream();InputStream i=new FileInputStream(f);byte[] x=new byte[8192];int n;while((n=i.read(x))>0)b.write(x,0,n);i.close();return b.toString("UTF-8");}
    static String val(String j,String k){Matcher m=Pattern.compile("\\\""+Pattern.quote(k)+"\\\"\\s*:\\s*(\\\"(?:\\\\.|[^\\\"])*\\\"|\\[[^]]*\\]|\\{.*?\\})",Pattern.DOTALL).matcher(j);if(!m.find())return null;String v=m.group(1);return v.startsWith("\"")?v.substring(1,v.length()-1):v;}
    static ArrayList<String> arr(String s){ArrayList<String>a=new ArrayList<>();Matcher m=Pattern.compile("\"([^\"]*)\"").matcher(s);while(m.find())a.add(m.group(1));return a;}
}
