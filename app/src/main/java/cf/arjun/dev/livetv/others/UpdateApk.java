package cf.arjun.dev.livetv.others;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;

public class UpdateApk {

    public static void downloadApk(Context context, String url) {

        String fileName = "Live TV.apk";
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        SharedPreferences prefs = context.getSharedPreferences("download_prefs", Context.MODE_PRIVATE);
        long previousId = prefs.getLong("download_id", -1);
        if (previousId != -1) {
            manager.remove(previousId);
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                .setTitle("Downloading Live TV")
                .setDescription("Please wait...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        long downloadId = manager.enqueue(request);
        prefs.edit().putLong("download_id", downloadId).apply();
    }

}
