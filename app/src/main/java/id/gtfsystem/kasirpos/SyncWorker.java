package id.gtfsystem.kasirpos;

import android.content.Context;
import android.net.Uri;
import android.webkit.CookieManager;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** WorkManager retries durable native mirror; server must be idempotent by client_txn_id. */
public class SyncWorker extends Worker {
    public SyncWorker(@NonNull Context c,@NonNull WorkerParameters p){super(c,p);}
    @NonNull @Override public Result doWork() {
        try {
            JSONArray orders=NativeOutbox.read(getApplicationContext());
            Uri base=Uri.parse(BuildConfig.POS_URL);
            Uri api=base.buildUpon().path(base.getPath().substring(0,base.getPath().lastIndexOf('/')+1)+"api.php")
                .encodedQuery("action=create_order").build();
            if(!"https".equalsIgnoreCase(api.getScheme())) return Result.failure();
            for(int i=0;i<orders.length();i++) {
                JSONObject order=orders.getJSONObject(i);
                String id=order.getString("client_txn_id");
                if(NativeOutbox.acknowledged(getApplicationContext(),id)) continue;
                HttpURLConnection conn=(HttpURLConnection)new URL(api.toString()).openConnection();
                conn.setRequestMethod("POST");conn.setConnectTimeout(12000);conn.setReadTimeout(15000);
                conn.setDoOutput(true);conn.setInstanceFollowRedirects(false);
                conn.setRequestProperty("Content-Type","application/json; charset=utf-8");
                String cookies=CookieManager.getInstance().getCookie(api.toString());
                if(cookies!=null && !cookies.isEmpty()) conn.setRequestProperty("Cookie",cookies);
                try {
                    byte[] b=order.toString().getBytes(StandardCharsets.UTF_8);
                    try(OutputStream out=conn.getOutputStream()){out.write(b);}
                    int status=conn.getResponseCode();
                    if(status>=200 && status<300) {
                        String response;
                        try(java.io.InputStream in=conn.getInputStream()) {
                            java.io.ByteArrayOutputStream buf=new java.io.ByteArrayOutputStream();
                            byte[] chunk=new byte[4096];int n;
                            while((n=in.read(chunk))!=-1 && buf.size()<65536) buf.write(chunk,0,n);
                            response=buf.toString("UTF-8");
                        }
                        JSONObject accepted=new JSONObject(response);
                        if(accepted.optBoolean("success") && accepted.optJSONObject("order") != null
                                && accepted.optJSONObject("order").optInt("id",0) > 0) {
                            NativeOutbox.ack(getApplicationContext(),id);
                            NativeOutbox.remove(getApplicationContext(),id);
                            continue;
                        }
                    }
                    // Do not endlessly retry authentication / validation failures.
                    if(status==401||status==403||status==419 || (status>=400&&status<500)) return Result.success();
                    return Result.retry();
                } finally {conn.disconnect();}
            }
            return Result.success();
        } catch(Exception ex) {return Result.retry();}
    }
}
