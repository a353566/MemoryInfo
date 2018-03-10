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
    private static final String TAG = "autoRecover";
    private Intent RecordServiceIntent;
    // Callback
    private IRemoteServiceCallback mIRemoteServiceCallback;
    private ServiceConnection RecordConnection = new ServiceConnection() {
        // 與 Service 連線建立時會呼叫
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            // 用 IRemoteService.Stub.asInterface(service) 取出連線的 Stub
            // 之後就可以呼叫此 interface 來溝通
        }

        // 與 Service 意外斷開連線時呼叫
        public void onServiceDisconnected(ComponentName className) {
            Log.e(TAG, "Service has unexpectedly disconnected");
            // 斷掉後就是重啟 autoRecording
            boolean bindSuccess = contactService();
            Log.d(TAG, "Service restart " + (bindSuccess ? "good" : "fail"));
        }
    };

    @Override
    public void onCreate() {
        // 只有一個任務 就是連接 autoRecording
        contactService();
    }

    private boolean contactService() {
        RecordServiceIntent = new Intent();
        RecordServiceIntent.setAction("service.Record");
        RecordServiceIntent.setPackage("com.mason.memoryinfo");
        // 先試者連接看看
        boolean bindSuccess = bindService(RecordServiceIntent, RecordConnection, Context.BIND_AUTO_CREATE);
        if (!bindSuccess) {
            // 開始 Service
            startService(RecordServiceIntent);
            bindSuccess = bindService(RecordServiceIntent, RecordConnection, Context.BIND_AUTO_CREATE);
        }
        return bindSuccess;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        // app 在連接時，回傳 stub 給他即可
        if (mIRemoteServiceCallback == null) {
            mIRemoteServiceCallback = new IRemoteServiceCallback.Stub() {
                @Override
                public void basicTypesCallback(int funcID, final String outputDataPackage) {

                }
            };
        }
        return (IBinder) mIRemoteServiceCallback;
    }
}
