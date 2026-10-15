package id.gtfsystem.kasirpos;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.widget.Toast;

/** V10 Android shell. Keeps the existing POS site and persistent offline queue. */
public class MainActivity extends Activity {
    private WebView web;
    private TextView connectionLabel;
    private String allowedHost;
    private static final int NAVY = Color.rgb(18, 29, 58);
    private static final int BLUE = Color.rgb(40, 92, 216);

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable rounded(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(14));
        return d;
    }

    private Button button(String title, int color) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTextColor(Color.WHITE);
        b.setBackground(rounded(color));
        b.setMinHeight(dp(52));
        b.setPadding(dp(6), 0, dp(6), 0);
        return b;
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network active = cm.getActiveNetwork();
        if (active == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(active);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void updateConnection() {
        if (connectionLabel == null) return;
        boolean online = isConnected();
        connectionLabel.setText(online ? "●  Online" : "●  Offline - antrean lokal");
        connectionLabel.setTextColor(online ? Color.rgb(25, 145, 99) : Color.rgb(200, 107, 34));
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);

        Uri start = Uri.parse(BuildConfig.POS_URL);
        if (!"https".equalsIgnoreCase(start.getScheme()) || start.getHost() == null) {
            throw new IllegalStateException("POS_URL harus HTTPS dan domain valid");
        }
        allowedHost = start.getHost();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(18), dp(8), dp(18), dp(8));
        top.setBackgroundColor(Color.rgb(248, 250, 254));
        TextView heading = new TextView(this);
        heading.setText("KasirPOS  V10");
        heading.setTextColor(NAVY);
        heading.setTextSize(19);
        heading.setTypeface(null, Typeface.BOLD);
        top.addView(heading, new LinearLayout.LayoutParams(0, dp(42), 1));

        connectionLabel = new TextView(this);
        connectionLabel.setTextSize(12);
        connectionLabel.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        top.addView(connectionLabel, new LinearLayout.LayoutParams(dp(164), dp(42)));
        root.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));

        web = new WebView(this);
        root.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(dp(10), dp(8), dp(10), dp(10));
        bottom.setBackgroundColor(Color.rgb(248, 250, 254));
        Button home = button("Kasir", BLUE);
        Button reload = button("Muat ulang", NAVY);
        Button sync = button("Sinkronkan", Color.rgb(17, 117, 100));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(0, dp(52), 1);
        buttonParams.setMargins(dp(4), 0, dp(4), 0);
        bottom.addView(home, buttonParams);
        LinearLayout.LayoutParams second = new LinearLayout.LayoutParams(0, dp(52), 1);
        second.setMargins(dp(4), 0, dp(4), 0);
        bottom.addView(reload, second);
        LinearLayout.LayoutParams third = new LinearLayout.LayoutParams(0, dp(52), 1);
        third.setMargins(dp(4), 0, dp(4), 0);
        bottom.addView(sync, third);
        root.addView(bottom, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(72)));

        setContentView(root);
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
                catch (Exception e) { Toast.makeText(MainActivity.this, "Link tidak dapat dibuka", Toast.LENGTH_SHORT).show(); }
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                updateConnection();
            }
        });
        home.setOnClickListener(v -> web.loadUrl(BuildConfig.POS_URL));
        reload.setOnClickListener(v -> { updateConnection(); web.reload(); });
        sync.setOnClickListener(v -> {
            new NativeBridge(MainActivity.this).syncNow();
            updateConnection();
            Toast.makeText(MainActivity.this, "Permintaan sinkronisasi dijadwalkan", Toast.LENGTH_SHORT).show();
        });
        updateConnection();
        // Do not wipe WebView data; IndexedDB may contain transactions not yet uploaded.
        web.loadUrl(BuildConfig.POS_URL);
    }

    @Override protected void onResume() {
        super.onResume();
        updateConnection();
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        CookieManager.getInstance().flush();
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
