package com.sayhi.extensions;

import android.app.*;import android.os.*;import android.content.*;import android.net.Uri;import android.provider.Settings;import android.view.*;import android.webkit.*;import android.widget.*;import java.util.*;

public class MainActivity extends Activity {
    WebView web; EditText url; ExtensionManager extensions;
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_main);
        web=findViewById(R.id.web); url=findViewById(R.id.url); extensions=new ExtensionManager(this);
        WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true); s.setAllowFileAccess(true); s.setAllowContentAccess(true); s.setSupportZoom(false);
        web.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView v,String u){extensions.inject(v,u);url.setText(u);}});
        findViewById(R.id.go).setOnClickListener(v->go()); findViewById(R.id.back).setOnClickListener(v->{if(web.canGoBack())web.goBack();}); findViewById(R.id.forward).setOnClickListener(v->{if(web.canGoForward())web.goForward();}); findViewById(R.id.ext).setOnClickListener(v->showExtensions());
        web.loadUrl("https://www.google.com");
    }
    void go(){String u=url.getText().toString().trim();if(!u.startsWith("http://")&&!u.startsWith("https://"))u="https://"+u;web.loadUrl(u);}
    void showExtensions(){final String[] items=extensions.list().toArray(new String[0]); AlertDialog.Builder b=new AlertDialog.Builder(this).setTitle("Extensions");
        b.setItems(items.length==0?new String[]{"No extensions installed"}:items,(d,w)->{}).setPositiveButton("Install ZIP",(d,w)->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/zip");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,44);}).setNegativeButton("Close",null).show();}
    @Override protected void onActivityResult(int r,int c,Intent data){super.onActivityResult(r,c,data);if(r==44&&c==RESULT_OK&&data!=null){try{extensions.install(data.getData());Toast.makeText(this,"Extension installed",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Install failed: "+e.getMessage(),Toast.LENGTH_LONG).show();}}}
}
