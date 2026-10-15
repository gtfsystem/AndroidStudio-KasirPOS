package id.gtfsystem.kasirpos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** V10.4 selectable server: admin verified on destination before URL is persisted. */
public class MainActivity extends Activity {
    private WebView web;
    private TextView connectionLabel;
    private String currentUrl;
    private String allowedHost;
    private static final int NAVY = Color.rgb(18, 29, 58);
    private static final int BLUE = Color.rgb(40, 92, 216);
    private static final String PREFS = "kasir_server_v104";
    private static final String KEY_URL = "server_url";

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable rounded(int color) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(14));return d;
    }
    private Button button(String title,int color) {
        Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(12);
        b.setTextColor(Color.WHITE);b.setBackground(rounded(color));b.setMinHeight(dp(50));return b;
    }
    private String normalise(String input) {
        if(input == null) return null;
        Uri uri;
        try { uri=Uri.parse(input.trim()); } catch(Exception ex) { return null; }
        if(!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost()==null || (uri.getEncodedAuthority()!=null && uri.getEncodedAuthority().contains("@")) ||
            uri.getPort()!=-1 || uri.getQuery()!=null || uri.getFragment()!=null) return null;
        String path=uri.getPath();
        if(path==null || path.isEmpty() || "/".equals(path)) path="/KasirPOS/";
        if(path.endsWith(".php")) path=path.substring(0,path.lastIndexOf('/')+1);
        if(!path.endsWith("/")) path += "/";
        if(path.contains("..") || path.contains("//")) return null;
        return new Uri.Builder().scheme("https").authority(uri.getHost().toLowerCase()).path(path+"pos.php").build().toString();
    }
    private String baseUrl(String posUrl) {return posUrl.substring(0,posUrl.lastIndexOf('/')+1);}
    private boolean isConnected() {
        ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm==null) return false;
        android.net.Network active=cm.getActiveNetwork();
        if(active==null)return false;
        NetworkCapabilities caps=cm.getNetworkCapabilities(active);
        return caps!=null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
    private void updateConnection() {
        if(connectionLabel==null)return;
        boolean online=isConnected();
        connectionLabel.setText(online?"● Online":"● Offline");
        connectionLabel.setTextColor(online?Color.rgb(25,145,99):Color.rgb(200,107,34));
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);getWindow().setNavigationBarColor(NAVY);
        String stored=getSharedPreferences(PREFS,MODE_PRIVATE).getString(KEY_URL,BuildConfig.POS_URL);
        currentUrl=normalise(stored);
        if(currentUrl==null)currentUrl=normalise(BuildConfig.POS_URL);
        if(currentUrl==null)throw new IllegalStateException("Default POS_URL harus HTTPS");
        allowedHost=Uri.parse(currentUrl).getHost();
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.WHITE);
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(14),dp(12),dp(14),dp(12));
        top.setBackgroundColor(Color.rgb(248,250,254));
        TextView heading=new TextView(this);heading.setText("KasirPOS");heading.setTextColor(NAVY);
        heading.setTextSize(17);heading.setTypeface(null,Typeface.BOLD);
        top.addView(heading,new LinearLayout.LayoutParams(0,dp(42),1));
        connectionLabel=new TextView(this);connectionLabel.setTextSize(12);
        connectionLabel.setGravity(Gravity.CENTER_VERTICAL|Gravity.END);
        top.addView(connectionLabel,new LinearLayout.LayoutParams(dp(82),dp(42)));
        root.addView(top,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(68)));
        web=new WebView(this);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        Button toolsButton=button("⋮",Color.rgb(53,96,81));
        top.addView(toolsButton,new LinearLayout.LayoutParams(dp(52),dp(48)));
        setContentView(root);
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setAllowFileAccess(false);s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new NativeBridge(this),"KasirNative");
        NativeBridge.schedulePeriodic(this);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest req){
                Uri uri=req.getUrl();
                if("https".equalsIgnoreCase(uri.getScheme()) && allowedHost.equalsIgnoreCase(uri.getHost()))return false;
                try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception ex){Toast.makeText(MainActivity.this,"Link tidak bisa dibuka",Toast.LENGTH_SHORT).show();}
                return true;
            }
            @Override public void onPageFinished(WebView view,String url){updateConnection();}
        });
        toolsButton.setOnClickListener(v->new AlertDialog.Builder(this)
            .setTitle("Menu Aplikasi")
            .setItems(new String[]{"Kasir","Muat ulang","Sinkronkan","Pengaturan Server (Admin)"},(d,which)->{
                if(which==0)web.loadUrl(currentUrl);
                else if(which==1)web.reload();
                else if(which==2){new NativeBridge(this).syncNow();Toast.makeText(this,"Sinkronisasi dijadwalkan",Toast.LENGTH_SHORT).show();}
                else showServerDialog();
            }).setNegativeButton("Tutup",null).show());
        updateConnection();web.loadUrl(currentUrl);
    }
    private void showServerDialog(){
        LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);fields.setPadding(dp(20),dp(4),dp(20),0);
        TextView desc=new TextView(this);desc.setText("Alamat server aktif:\n"+baseUrl(currentUrl)+"\n\nUbah server hanya oleh admin/owner di SERVER TUJUAN. Website tujuan harus sudah memiliki android-server-verify.php.\n");
        desc.setTextSize(13);fields.addView(desc);
        EditText url=new EditText(this);url.setSingleLine(true);url.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);url.setHint("https://domainbaru.com/KasirPOS/");url.setText(baseUrl(currentUrl));fields.addView(url);
        EditText username=new EditText(this);username.setSingleLine(true);username.setHint("Username admin / owner");fields.addView(username);
        EditText password=new EditText(this);password.setSingleLine(true);password.setHint("Password admin / owner");password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);fields.addView(password);
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle("Pengaturan Server • Admin Only")
            .setView(fields).setNegativeButton("Batal",(d,w)->{})
            .setPositiveButton("Verifikasi & Simpan",null).create();
        dlg.setOnShowListener(d->dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String candidate=normalise(url.getText().toString());
            if(candidate==null){url.setError("Wajib HTTPS dengan URL server KasirPOS yang valid");return;}
            String un=username.getText().toString().trim(),pw=password.getText().toString();
            if(un.isEmpty()||pw.isEmpty()){Toast.makeText(this,"Isi admin username dan password",Toast.LENGTH_SHORT).show();return;}
            // Pending queue must be resolved before migration. Never send old pending orders to new server.
            try{if(NativeOutbox.read(this).length()>0){new AlertDialog.Builder(this).setTitle("Ada transaksi belum tersinkron")
                .setMessage("Selesaikan sinkronisasi di server lama sebelum pindah. Perpindahan diblokir agar transaksi tidak masuk ke server yang salah.")
                .setPositiveButton("OK",null).show();return;}}
            catch(Exception ex){Toast.makeText(this,"Gagal memeriksa antrean transaksi",Toast.LENGTH_LONG).show();return;}
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            new Thread(()->{
                boolean ok=false;String error="Verifikasi server ditolak";
                try{
                    URL endpoint=new URL(baseUrl(candidate)+"android-server-verify.php");
                    HttpURLConnection conn=(HttpURLConnection)endpoint.openConnection();
                    conn.setRequestMethod("POST");conn.setConnectTimeout(12000);conn.setReadTimeout(12000);
                    conn.setInstanceFollowRedirects(false);conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type","application/json; charset=utf-8");
                    JSONObject data=new JSONObject();data.put("username",un);data.put("password",pw);
                    try(OutputStream out=conn.getOutputStream()){out.write(data.toString().getBytes(StandardCharsets.UTF_8));}
                    if(conn.getResponseCode()==200){
                        try(InputStream in=conn.getInputStream()){
                            byte[] bytes=new byte[4096];int n=in.read(bytes);
                            if(n>0){JSONObject resp=new JSONObject(new String(bytes,0,n,StandardCharsets.UTF_8));
                                ok=resp.optBoolean("success")&&"KasirPOS".equals(resp.optString("app"))&&"server-config-v1".equals(resp.optString("protocol"));}
                        }
                    }
                    conn.disconnect();
                }catch(Exception ex){error="Server tidak terhubung atau endpoint verifikasi belum tersedia";}
                final boolean verified=ok;final String message=error;
                runOnUiThread(()->{
                    if(!verified){Toast.makeText(this,message,Toast.LENGTH_LONG).show();dlg.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);return;}
                    // WebView IndexedDB persists by origin, no storage is cleared.
                    currentUrl=candidate;allowedHost=Uri.parse(candidate).getHost();
                    getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(KEY_URL,candidate).apply();
                    dlg.dismiss();web.loadUrl(currentUrl);Toast.makeText(this,"Server diperbarui. Silakan login kembali.",Toast.LENGTH_LONG).show();
                });
            }).start();
        }));
        dlg.show();
    }
    @Override protected void onResume(){super.onResume();updateConnection();}
    @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){CookieManager.getInstance().flush();if(web!=null)web.destroy();super.onDestroy();}
}
