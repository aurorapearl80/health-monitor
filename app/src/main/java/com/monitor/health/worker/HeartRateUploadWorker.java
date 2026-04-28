package com.monitor.health.worker;

// app/src/main/java/.../workers/HeartRateUploadWorker.java
import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.monitor.health.dao.HeartRateDao;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.model.HeartRateEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class HeartRateUploadWorker extends Worker {

    DatabaseClient databaseClient;
    private static final String PREF_LAST_SYNC = "lastSync";

    public HeartRateUploadWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
        //Toast.makeText(getApplicationContext(), "starting", Toast.LENGTH_SHORT).show();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("Running", "doWork: the working....................................");
        Context c = getApplicationContext();
        SharedPreferences sp = c.getSharedPreferences("sync_prefs", MODE_PRIVATE);
        long lastSync = sp.getLong(PREF_LAST_SYNC, 0);

        databaseClient = DatabaseClient.getInstance(getApplicationContext());

        Log.wtf("Running", "doWork: the working....................................");

        //HeartRateDao dao = AppDatabase.get(c).heartRateDao();
        HeartRateDao dao = databaseClient.getAppDatabase().heartRateDao();
        List<HeartRateEntity> newSamples = dao.getNewSamples(lastSync);

        if (newSamples.isEmpty()) return Result.success();  // nothing to do

        // ---- Build JSON payload ------------------------------------------
        JSONArray arr = new JSONArray();
        for (HeartRateEntity e : newSamples) {
            JSONObject o = new JSONObject();
            try {
                o.put("timestamp", e.getEpochMillis());
                o.put("bpm",       e.getValue());
            } catch (JSONException ignore) {}
            arr.put(o);
        }
        JSONObject body = new JSONObject();
        try { body.put("samples", arr); } catch (JSONException ignore) {}

        Log.d("Running", "doWork: the working............"+arr.toString());

        // ---- Send to your REST endpoint (pseudo--replace with Retrofit) ---
       // boolean ok = MyApi.uploadHeartRates(body); // returns true on 2xx
        //Toast.makeText(getApplicationContext(), body.toString(), Toast.LENGTH_LONG).show();
        boolean ok = false;
        if (ok) {
            long newestTs = newSamples.get(newSamples.size() - 1).getEpochMillis();
            dao.deleteUpTo(newestTs);                       // keep DB small
            sp.edit().putLong(PREF_LAST_SYNC, newestTs).apply();
            return Result.success();
        } else {
            return Result.retry();  // WorkManager backs off automatically
        }
    }
}
