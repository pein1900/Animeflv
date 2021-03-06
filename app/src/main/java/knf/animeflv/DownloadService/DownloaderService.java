package knf.animeflv.DownloadService;

import android.app.DownloadManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;

import knf.animeflv.Explorer.ExplorerRoot;
import knf.animeflv.Parser;
import knf.animeflv.R;
import knf.animeflv.Utils.FileUtil;
import knf.animeflv.Utils.NetworkUtils;

public class DownloaderService extends IntentService {
    public static final int CANCELED = 1554785;
    private static final int DOWNLOAD_NOTIFICATION_ID = 4458758;
    private static final int NULL = 388744;
    public static String RECEIVER_ACTION_ERROR = "knf.animeflv.DownloadService.DownloadService.RECIEVER_ERROR";
    private NotificationManager manager;
    private NotificationCompat.Builder downloading;

    public DownloaderService() {
        super("Animeflv Download Service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        startForeground(DOWNLOAD_NOTIFICATION_ID, getDownloadingBuilder().build());
        return IntentService.START_REDELIVER_INTENT;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int count;
        Bundle bundle = intent.getExtras();
        String eid = "error";
        try {
            SharedPreferences preferences = getSharedPreferences("data", MODE_PRIVATE);
            eid = bundle.getString("eid");
            long downloadID = bundle.getLong("downloadID");
            if (new SQLiteHelperDownloads(this).getDownloadInfo(eid, true)._download_id != downloadID)
                throw new DownloadCanceledException(String.valueOf(downloadID));
            if (new SQLiteHelperDownloads(this).getState(eid) == DownloadManager.STATUS_RUNNING)
                FileUtil.init(this).DeleteAnime(eid);
            if (!NetworkUtils.isNetworkAvailable())
                throw new IllegalStateException();
            onStartDownload(eid);
            URL url = new URL(bundle.getString("url"));
            URLConnection conection = url.openConnection();
            if (bundle.getBoolean("constructor")) {
                conection.setRequestProperty("Cookie", bundle.getString("cookie"));
                conection.setRequestProperty("Referer", bundle.getString("referer"));
                conection.setRequestProperty("User-Agent", bundle.getString("useragent"));
            }
            conection.connect();
            int lenghtOfFile = conection.getContentLength();
            InputStream input = conection.getInputStream();
            OutputStream output = FileUtil.init(this).getOutputStream(eid);
            byte data[] = new byte[1024 * 6];
            long total = 0;
            int prog = 0;
            while ((count = input.read(data)) != -1) {
                if (preferences.getLong(eid + "_downloadID", -1) != downloadID)
                    throw new DownloadCanceledException(String.valueOf(downloadID));
                if (!NetworkUtils.isNetworkAvailable())
                    throw new IllegalStateException();
                total += count;
                int tprog = (int) ((total * 100) / lenghtOfFile);
                if (tprog > prog) {
                    prog = tprog;
                    updateCurrentProgress(eid, prog);
                }
                output.write(data, 0, count);
                output.flush();
            }
            output.close();
            input.close();
            onSuccess(eid);
        } catch (DownloadCanceledException canceled) {
            Log.e("DownloadService", "Canceled - Eid: " + eid + " ID: " + canceled.getMessage());
            FileUtil.init(this).DeleteAnime(eid);
            DownloadListManager.delete(this, eid + "_" + bundle.getLong("downloadID"));
            new SQLiteHelperDownloads(this).delete(eid).close();
        } catch (Exception e) {
            Log.e("DownloadService", "error on try", e);
            onDownloadFailed(eid, intent);
        }
    }

    private void onStartDownload(String eid) {
        getManager().cancel(getDownloadID(eid));
        String title = new Parser().getTitCached(eid.replace("E", "").split("_")[0]);
        NotificationCompat.Builder mBuilder = getDownloadingBuilder()
                .setContentTitle(title)
                .setContentText("Capítulo " + eid.replace("E", "").split("_")[1])
                .setContentIntent(PendingIntent.getActivity(this, 0, getDownloadingIntent(eid.split("_")[0]), PendingIntent.FLAG_UPDATE_CURRENT))
                .setProgress(100, 0, true);
        getManager().notify(DOWNLOAD_NOTIFICATION_ID, mBuilder.build());
        new SQLiteHelperDownloads(this).updateState(eid, DownloadManager.STATUS_RUNNING).close();
    }

    private Intent getDownloadingIntent(String eid) {
        Intent intent = new Intent(this, ExplorerRoot.class);
        intent.putExtra("aid", eid);
        return intent;
    }

    private NotificationCompat.Builder getDownloadingBuilder() {
        if (downloading == null)
            downloading = new NotificationCompat.Builder(this)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setOngoing(true);
        return downloading;
    }

    private void updateCurrentProgress(String eid, int progress) {
        updateSavedProgress(eid, progress);
        int pending = new SQLiteHelperDownloads(this).getTotalDownloads();
        NotificationCompat.Builder mBuilder = getDownloadingBuilder()
                .setProgress(100, progress, false);
        if (pending - 1 > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mBuilder.setSubText(pending - 1 + (pending - 1 == 1 ? " pendiente" : " pendientes"));
            } else {
                if (pending > 1) {
                    mBuilder.setNumber(pending);
                } else {
                    mBuilder.setNumber(-1);
                }
            }
        }
        getManager().notify(DOWNLOAD_NOTIFICATION_ID, mBuilder.build());
    }

    private void updateSavedProgress(String eid, int progress) {
        new SQLiteHelperDownloads(this).updateProgress(eid, progress).close();
    }

    private void onDownloadFailed(String eid, Intent intent) {
        FileUtil.init(this).DeleteAnime(eid);
        DownloadListManager.delete(this, eid + "_" + getSharedPreferences("data", MODE_PRIVATE).getLong(eid + "_downloadID", -1));
        String[] semi = eid.replace("E", "").split("_");
        Intent n_intent = new Intent(DownloadBroadCaster.ACTION_RETRY);
        n_intent.putExtras(intent.getExtras());
        n_intent.putExtra("not_id", getDownloadID(eid));
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(new Parser().getTitCached(semi[0]) + " - " + semi[1])
                .setContentText("ERROR AL DESCARGAR")
                .addAction(R.drawable.redo, "REINTENTAR", PendingIntent.getBroadcast(this, new Random().nextInt(), n_intent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setOngoing(false);
        getManager().notify(getDownloadID(eid), builder.build());
        new SQLiteHelperDownloads(this).updateState(eid, DownloadManager.STATUS_FAILED).delete(eid);
        sendBroadcast(new Intent(RECEIVER_ACTION_ERROR));
    }

    private int getDownloadID(String eid) {
        return Math.abs(eid.hashCode());
    }

    private NotificationManager getManager() {
        if (manager == null)
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        return manager;
    }

    private void onSuccess(String eid) {
        DownloadListManager.delete(this, eid + "_" + getSharedPreferences("data", MODE_PRIVATE).getLong(eid + "_downloadID", -1));
        String title = new Parser().getTitCached(eid.replace("E", "").split("_")[0]);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(title + " - " + eid.replace("E", "").split("_")[1])
                .setContentText("DESCARGA COMPLETADA")
                .setAutoCancel(true)
                .setOngoing(false);
        if (Build.VERSION.SDK_INT < 24) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(FileUtil.init(this).getFileNormal(eid)));
            intent.putExtra("title", title);
            intent.setDataAndType(Uri.fromFile(FileUtil.init(this).getFileNormal(eid)), "video/mp4");
            PendingIntent pendingIntent = PendingIntent.getActivity(this, getDownloadID(eid), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);
        }
        getManager().notify(getDownloadID(eid), builder.build());
        new SQLiteHelperDownloads(this).updateState(eid, DownloadManager.STATUS_SUCCESSFUL).delete(eid);
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        getManager().cancel(DOWNLOAD_NOTIFICATION_ID);
        new SQLiteHelperDownloads(this).reset();
        super.onDestroy();
    }
}
