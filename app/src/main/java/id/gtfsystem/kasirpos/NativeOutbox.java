package id.gtfsystem.kasirpos;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/** App-private mirror of JS outbox. JavaScript IndexedDB remains primary ledger. */
final class NativeOutbox {
    private static final Object LOCK = new Object();
    private static File file(Context c) { return new File(c.getFilesDir(), "pending-native-orders.json"); }
    static JSONArray read(Context c) throws Exception {
        File f=file(c);
        if(!f.exists()) return new JSONArray();
        try(FileInputStream in=new FileInputStream(f)) {
            byte[] data=new byte[(int)f.length()];
            int n=0, r;
            while(n<data.length && (r=in.read(data,n,data.length-n))>0) n+=r;
            return new JSONArray(new String(data,0,n,StandardCharsets.UTF_8));
        }
    }
    static void write(Context c,JSONArray arr) throws Exception {
        File tmp=new File(c.getFilesDir(),"pending-native-orders.tmp");
        try(FileOutputStream out=new FileOutputStream(tmp)) {
            out.write(arr.toString().getBytes(StandardCharsets.UTF_8));out.getFD().sync();
        }
        if(!tmp.renameTo(file(c))) throw new Exception("Tidak dapat menyimpan antrean native");
    }
    static void upsert(Context c,String payload) throws Exception {
        synchronized(LOCK) {
            JSONObject item=new JSONObject(payload);
            String id=item.getString("client_txn_id");
            JSONArray old=read(c),next=new JSONArray();
            for(int i=0;i<old.length();i++) {
                JSONObject row=old.getJSONObject(i);
                if(!id.equals(row.optString("client_txn_id"))) next.put(row);
            }
            next.put(item);write(c,next);
        }
    }
    static void remove(Context c,String id) throws Exception {
        synchronized(LOCK) {
            JSONArray old=read(c),next=new JSONArray();
            for(int i=0;i<old.length();i++) {
                JSONObject row=old.getJSONObject(i);
                if(!id.equals(row.optString("client_txn_id"))) next.put(row);
            }
            write(c,next);
        }
    }
    static void ack(Context c,String id) {
        c.getSharedPreferences("native_sync_acks",0).edit().putBoolean(id,true).apply();
    }
    static boolean acknowledged(Context c,String id) {
        return c.getSharedPreferences("native_sync_acks",0).getBoolean(id,false);
    }
}
