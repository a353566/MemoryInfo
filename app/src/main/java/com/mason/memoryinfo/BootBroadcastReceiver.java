package com.mason.memoryinfo;

/**
 * Created by LALALA on 2017/8/16.
 * 開機自行啟動
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            // 啟動 service - autoRecording
            Intent serviceIntent = new Intent(context,autoRecording.class);
            context.startService(serviceIntent);

            // 啟動 MainActivity
//            Intent activityIntent = new Intent(context, MainActivity.class);
//            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(activityIntent);
        }
    }
}