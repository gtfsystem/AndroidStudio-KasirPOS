package id.gtfsystem.kasirpos;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.widget.Toast;

/** HTTPS-only first-party wrapper. IndexedDB + service worker remain within WebView storage. */
public class MainActivity extends Activity {
    private WebView web;
    private String allowedHost;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Uri start = Uri.parse(BuildConfig.POS_URL);
        if (!"https".equalsIgnoreCase(start.getScheme()) || start.getHost() == null) {
            throw new IllegalStateException("POS_URL harus HTTPS dan domain valid");
        }
        allowedHost = start.getHost();
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new NativeBridge(this), "KasirNative");
        NativeBridge.schedulePeriodic(this);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri target = req.getUrl();
                if ("https".equalsIgnoreCase(target.getScheme()) && allowedHost.equalsIgnoreCase(target.getHost())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, target)); }
                catch (Exception e) { Toast.makeText(MainActivity.this, "Tidak ada aplikasi untuk membuka link", Toast.LENGTH_SHORT).show(); }
                return true;
            }
        });
        // Never call clearCache/clearHistory/WebStorage.deleteAllData: pending orders live in IndexedDB.
        web.loadUrl(BuildConfig.POS_URL);
    }
    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        CookieManager.getInstance().flush();
        super.onDestroy();
    }
}
