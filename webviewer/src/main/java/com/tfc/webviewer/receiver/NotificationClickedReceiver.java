package com.tfc.webviewer.receiver;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Fobid on 2016. 4. 9..
 */
public class NotificationClickedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent notiIntent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
        notiIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(notiIntent);
    }
}
