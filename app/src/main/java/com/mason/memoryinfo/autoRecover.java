package com.mason.memoryinfo;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by LALALA on 2018/2/27.
 * 用來和 autoRecording 互相綁定的 App
 */

public class autoRecover extends Service {
    private Intent RecordServiceIntent;

    private ServiceConnection RecordConnection = new ServiceConnection() {
        // 與 Service 連線建立時會呼叫
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("autoRecover", "onServiceConnected");
            // 用 IRemoteService.Stub.asInterface(service) 取出連線的 Stub
            // 之後就可以呼叫此 interface 來溝通
        }

        // 與 Service 意外斷開連線時呼叫
        public void onServiceDisconnected(ComponentName className) {
            Log.e("autoRecover", "Service has unexpectedly disconnected");
            // 開始 Service
            RecordServiceIntent = new Intent();
            RecordServiceIntent.setAction("service.Record");
            RecordServiceIntent.setPackage("com.mason.memoryinfo");
            boolean bindSuccess = bindService(RecordServiceIntent, RecordConnection, Context.BIND_AUTO_CREATE);
            if (!bindSuccess) {
                startService(RecordServiceIntent);
                bindService(RecordServiceIntent, RecordConnection, Context.BIND_AUTO_CREATE);
            }
        }
    };

    @Override
    public void onCreate() {
        // 開始 Service
        RecordServiceIntent = new Intent();
        RecordServiceIntent.setAction("service.Record");
        RecordServiceIntent.setPackage("com.mason.memoryinfo");
        boolean bindSuccess = bindService(RecordServiceIntent, RecordConnection, Context.BIND_AUTO_CREATE);
        if (!bindSuccess) {
            startService(RecordServiceIntent);
            bindService(RecordServiceIntent, RecordConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
