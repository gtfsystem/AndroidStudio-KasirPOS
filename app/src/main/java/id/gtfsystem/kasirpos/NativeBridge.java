package id.gtfsystem.kasirpos;

import android.content.Context;
import android.webkit.JavascriptInterface;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public final class NativeBridge {
    private final Context context;
    NativeBridge(Context c){context=c.getApplicationContext();}
    private static Constraints network(){return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();}
    static void schedulePeriodic(Context c) {
        PeriodicWorkRequest periodic=new PeriodicWorkRequest.Builder(SyncWorker.class,15,TimeUnit.MINUTES)
                .setConstraints(network()).build();
        WorkManager.getInstance(c).enqueueUniquePeriodicWork("pos-background-sync",ExistingPeriodicWorkPolicy.KEEP,periodic);
    }
    private void scheduleNow() {
        OneTimeWorkRequest request=new OneTimeWorkRequest.Builder(SyncWorker.class).setConstraints(network()).build();
        WorkManager.getInstance(context).enqueueUniqueWork("pos-sync-now",ExistingWorkPolicy.KEEP,request);
    }
    @JavascriptInterface public boolean enqueue(String json) {
        try {NativeOutbox.upsert(context,json);scheduleNow();return true;}
        catch(Exception e){return false;}
    }
    @JavascriptInterface public void discard(String id) {
        try {NativeOutbox.remove(context,id);}catch(Exception ignored){}
    }
    @JavascriptInterface public boolean isAcknowledged(String id) {return NativeOutbox.acknowledged(context,id);}
    @JavascriptInterface public void syncNow(){scheduleNow();}
}
