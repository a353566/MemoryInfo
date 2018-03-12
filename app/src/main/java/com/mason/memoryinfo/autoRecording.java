package com.mason.memoryinfo;

import com.mason.memoryinfo.RecordData.*;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Objects;

public class autoRecording extends Service {
    private static String startTime;
    private static final String TAG = "autoRecording";
    private static final boolean isDebug = false;

    // 基本設定
    public boolean isNotification = true;
    public boolean isFullScan = false;

    // 基本宣告
    private ProcessRecord processRecord;
    private ScreenRecord screenRecord;
    private LocationRecord locationRecord;
    private WifiRecord wifiRecord;
    private SensorRecord sensorRecord;
    private BatteryRecord batteryRecord;
    private WriteFile writeFile;
    private ComputeTime computeTime;
    private Context serviceContext;

    // Cellback 函式
    private CellbackControl cellbackControl;

    private Intent RecoverServiceIntent;
    private ServiceConnection RecoverConnection = new ServiceConnection() {
        // 與 Service 連線建立時會呼叫
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            // 用 IRemoteService.Stub.asInterface(service) 取出連線的 Stub
            // 之後就可以呼叫此 interface 來溝通
        }

        // 與 Service 意外斷開連線時呼叫
        public void onServiceDisconnected(ComponentName className) {
            Log.e("autoRecording", "Service has unexpectedly disconnected");
            // 斷掉後就是重啟 autoRecording
            boolean bindSuccess = contactService();
            Log.d(TAG, "Service restart " + (bindSuccess ? "good" : "fail"));
        }
    };

    // 自動記錄的 thread
    private AutoRecordData autoRecordData;
    private String android_id;

    @SuppressLint("HardwareIds")
    @Override
    public void onCreate() {
        super.onCreate();
        android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        // 基本宣告
        screenRecord = new ScreenRecord((PowerManager) getSystemService(Context.POWER_SERVICE));
        processRecord = new ProcessRecord((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE), screenRecord);
        locationRecord = new LocationRecord(this);
        wifiRecord = new WifiRecord(this);
        sensorRecord = new SensorRecord((SensorManager) getSystemService(Context.SENSOR_SERVICE));
        batteryRecord = new BatteryRecord(this);
        writeFile = new WriteFile();
        computeTime = new ComputeTime();
        serviceContext = this;
        // 基本設定
        settingInitial();
        // Cellback 函式
        cellbackControl = new CellbackControl();

        autoRecordData = new AutoRecordData();
        autoRecordData.start();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        contactService();
    }

    private boolean contactService() {
        RecoverServiceIntent = new Intent();
        RecoverServiceIntent.setAction("service.Recover");
        RecoverServiceIntent.setPackage("com.mason.memoryinfo");
        // 開始 Recover Service
        startService(RecoverServiceIntent);
        boolean bindSuccess = bindService(RecoverServiceIntent, RecoverConnection, Context.BIND_WAIVE_PRIORITY);
        if (!bindSuccess) {
            // 開始 Recover Service
            startService(RecoverServiceIntent);
            bindSuccess = bindService(RecoverServiceIntent, RecoverConnection, Context.BIND_WAIVE_PRIORITY);
        }
        Log.d(TAG, "onServiceConnected : bindSuccess:" + bindSuccess);
        return bindSuccess;
    }

    /**
     * 自動記錄資料的 Thread
     * start 後就會開始記錄了
     */
    private class AutoRecordData extends Thread {
        // 時間常數設定，目前是預設 (ms)
        private double chickScreenCD = 1000;           // 檢查螢幕是否亮的 等待時間

        // 資料內容參數
        private StringBuffer outTitle;
        private StringBuffer outSensor;
        private StringBuffer outWiFi;
        private StringBuffer outProc;
        private StringBuffer[] outputString;

        // 是否停止
        private boolean loop;

        AutoRecordData() {
            // 資料內容參數
            outTitle = new StringBuffer();
            outSensor = new StringBuffer();
            outWiFi = new StringBuffer();
            outProc = new StringBuffer();
            outputString = new StringBuffer[4];
            outputString[0] = outTitle;
            outputString[1] = outProc;
            outputString[2] = outSensor;
            outputString[3] = outWiFi;

            loop = true;
        }

        @Override
        public void run() {
            // 寫入基本標頭檔案，並設定檔名為現在時間
            writeFile.ChangeAndroidID(android_id);

            // 跳出通知
            startTime = getDate();
            Notification("已從" + startTime + "紀錄至現在");
            while (loop) {
                // 先讓計時開始
                computeTime.startTime();

                // 清空 outputString
                outTitle.setLength(0);
                outProc.setLength(0);
                outSensor.setLength(0);
                outWiFi.setLength(0);

                // 標上 ---------- 表示分隔，以及標示時間
                outTitle.append("----------").append('\n');
                outTitle.append(getDate()).append('\n');
                // ========================================= 檢查是否掃描 =========================================
                // 紀錄是否有 scan (這邊也會直接開始記錄相關資訊)
                boolean isScan = processRecord.isNeedRecord();
                // 有掃描的話，就取出相關 sensor 參數，並記錄
                if (isScan) {
                    if (processRecord.canRecord())
                        outProc.append(processRecord.recordData()).append('\n');

                    // ---------- WiFi 檔案 Part.1/2 ----------
                    // 看後面要不要紀錄
                    boolean isWiFiScan = wifiRecord.isNeedRecord();
                    // ======================================== sensor 檔案 ========================================
                    // 有掃描的話，就取出相關 sensor 參數，並記錄
                    // Screen
                    outSensor.append(screenRecord.recordData()).append('\n');

                    // Battery
                    outSensor.append(batteryRecord.recordData()).append('\n');

                    // WiFi，GPS
                    outSensor.append(wifiRecord.isWiFiOpen() ? "WiFi:Open||" : "WiFi:Close||");
                    outSensor.append(wifiRecord.isGPSOpen() ? "GPS:Open" : "GPS:Close").append('\n');

                    // G-sensor
                    if (sensorRecord.isNeedRecord() && sensorRecord.canRecord())
                        outSensor.append(sensorRecord.recordData()).append('\n');

                    // Location
                    if (locationRecord.isNeedRecord() && locationRecord.canRecord())
                        outSensor.append(locationRecord.recordData()).append('\n');

                    // ---------- WiFi 檔案 Part.2/2 ----------
                    // 一段時間後看結果
                    if (isWiFiScan && wifiRecord.canRecord()) {
                        outWiFi.append(wifiRecord.recordData()).append('\n');
                    }

                    // ========================================= 寫入檔案 =========================================
                    writeFile.write(outputString, outputString.length);
                    // Log.d 輸出資料
                    if (isDebug) {
                        StringBuilder temp = new StringBuilder();
                        for (StringBuffer stringBuffer : outputString)
                            temp.append(stringBuffer);
                        Log.d(TAG, temp.toString());
                    }
                    // 是否要回傳資訊
                    if (cellbackControl.isCallback() && processRecord.isBigScan()) {
                        cellbackControl.Cellback(outProc.toString());
                    }
                }

                // ========================================= 判斷睡眠時間 =========================================
                // 如果是亮的，那就判斷 processRecord.getSleepTime() 回傳的數值作睡眠時間
                // 如果是暗的，那就最多睡一秒(chickScreenCD)就起來判斷螢幕有沒有亮
                long sleepingTime = processRecord.getSleepTime();   // 需要睡的時間 (毫秒 ms)
                if (!screenRecord.isScreenOn()) {
                    if (chickScreenCD < sleepingTime)
                    sleepingTime = (long) chickScreenCD;
                }

                // 有掃描的話，就會記錄此時間，等下一次寫檔的時候再寫入
                if (isScan) {
                    writeFile.lastComputeTime = computeTime.getComputeTime();
                }

                if (sleepingTime > 0) {
                    try {
                        Thread.sleep(sleepingTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // 取得系統的通知服務
    private NotificationManager notificationManager;
    private void Notification(String test) {
        if (!isNotification) {
            return;
        }
        // 通知的識別號碼
        int notifyID = 1;
        // ========================================= 跳出通知 =========================================
        Notification.Builder notiBuilder = new Notification.Builder(getApplicationContext());
        // 設定要顯示的內容
        notiBuilder.setSmallIcon(R.drawable.notification);
        notiBuilder.setContentTitle("謝謝您提供的研究資料");
        notiBuilder.setContentText(test);
        // 給予最高權限
        notiBuilder.setPriority(Notification.PRIORITY_MAX);
        // 建立通知
        Notification notification = notiBuilder.build();
        // 將 ongoing(持續)的 flag 添加到通知中
        //notification.flags |= Notification.FLAG_ONGOING_EVENT;
        // 發送通知
        notificationManager.notify(notifyID, notification);
    }

    /**
     * 寫入檔案
     * 輸出成檔案，目前是放在主SD卡裡面
     */
    private class WriteFile {
        private String directory;   // 目錄資料夾
        private String filename;    // 檔案名稱
        private String android_id;
        long lastComputeTime;       // 紀錄上次的搜尋秒數

        // 預設
        WriteFile() {
            directory = "memInfo";
            filename = getDate();
            lastComputeTime = 0;

            times=0;
            updateTime = new ComputeTime();
            updateTime.startTime();
        }

        boolean ChangeDirectory(String directory) {
            this.directory = directory;
            return true;
        }
        boolean ChangeFilename(String filename) {
            this.filename = filename;
            return true;
        }
        boolean ChangeAndroidID(String android_id) {
            this.android_id = android_id;
            return true;
        }

        private static final int TimesToIntervalTime = 60 * 1000;
        private static final int Max_IntervalTime = 2 * 60 * 60 * 1000;
        private int times;
        private ComputeTime updateTime;
        private void isChangeFileName() {
            // 算看看有沒有超過閥值
            long thresholdTime = times*TimesToIntervalTime + updateTime.getComputeTime();
            if (thresholdTime > Max_IntervalTime) { // 有的話幫檔案結尾
                // 先檢查這次是不是大掃描
                if (!processRecord.isBigScan()) {
                    times++;
                    return;
                }
                // 寫入上一刻的時間
                if (lastComputeTime!=0) {
                    write("scanTime:" + lastComputeTime + "ms\n----------\nover");
                    lastComputeTime=0;
                }
                Notification("已從" + startTime + "紀錄至現在");
                filename = getDate();
                updateTime.startTime();
                times=0;
            }
            if (times == 0) { // 第一次寫入的話，加入一些基本資訊
                // phoneID:fb5e43235974561d
                write("phoneID:" + android_id + '\n');
            }
            times++;
        }

        boolean write(StringBuffer[] output, int bufferAmount) {
            isChangeFileName();
            // 將 output 寫入 directory 資料夾中，檔名為 filename
            // 檢查儲存裝置是否存在
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                String root = Environment.getExternalStorageDirectory().toString();
                File file = new File(root + "/" + directory);
                // 檢察跟目錄是否在
                if (!file.exists()) {
                    if (!file.mkdirs()) {
                        Log.d(TAG, "(WriteFile) Directory not created");
                    }
                }

                // 檢查是否可以寫入
                if (file.canWrite()) {
                    if (file.exists()) {
                        file = new File(file, filename);
                        try {
                            FileWriter mFileWriter = new FileWriter(file, true);
                            // 寫入上一刻的時間
                            if (lastComputeTime!=0) {
                                mFileWriter.write("scanTime:" + lastComputeTime + "ms\n");
                                lastComputeTime=0;
                            }

                            // 檔案寫入
                            for (int i = 0; i < bufferAmount; i++)
                                mFileWriter.write(output[i].toString());

                            // 檔案關閉
                            mFileWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.d("WriteFile", "file is not exists");
                        return false;
                    }
                } else {
                    Log.d("WriteFile", "can not Write");
                    return false;
                }
            } else {
                Log.d("WriteFile", "儲存裝置不存在");
                return false;
            }

            return true;
        }

        boolean write(String output) {
            // 將 output 寫入 directory 資料夾中，檔名為 filename
            // 檢查儲存裝置是否存在
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                String root = Environment.getExternalStorageDirectory().toString();
                File file = new File(root + "/" + directory);
                // 檢察跟目錄是否在
                if (!file.exists()) {
                    if (!file.mkdirs()) {
                        Log.d("WriteFile", "Directory not created");
                    }
                }

                // 檢查是否可以寫入
                if (file.canWrite()) {
                    if (file.exists()) {
                        file = new File(file, filename);
                        try {
                            FileWriter mFileWriter = new FileWriter(file, true);
                            // 檔案寫入
                            mFileWriter.write(output);
                            // 檔案關閉
                            mFileWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e("WriteFile", "Write error");
                        }
                    } else {
                        Log.d("WriteFile", "file is not exists");
                        return false;
                    }
                } else {
                    Log.d("WriteFile", "can not Write");
                    return false;
                }
            } else {
                Log.d("WriteFile", "儲存裝置不存在");
                return false;
            }

            return true;
        }
    }

    /** 基本設定 */
    private void settingInitial() {
        RecordSetting setting = new RecordSetting(serviceContext);
        // close
        setting.close();

        // Notification
        String temp = setting.getSetting(RecordSetting.Notification);
        isNotification = Objects.equals(temp, "true") || !Objects.equals(temp, "false") && RecordSetting.isNotification;
        // FullScan
        temp = setting.getSetting(RecordSetting.FullScan);
        isFullScan = Objects.equals(temp, "true") || !Objects.equals(temp, "false") && RecordSetting.isFullScan;
        processRecord.setFullScan(isFullScan);
    }

    /**
     * Cellback 控制
     * 專門回傳資料給主程式 (MainActivity)
     */
    private class CellbackControl {
        private IRemoteServiceCallback callback;
        private int funcID;
        private boolean isGetData;

        /**
         * 回傳資訊給主程式的 stub
         * 用 callback.basicTypesCallback(...) 就可以回傳資料
         * 參數的變化可以去修改 IRemoteServiceCallback 的接口中的參數
         */
        private IRemoteService.Stub stub = new IRemoteService.Stub() {
            @Override
            public void basicTypes(int funcID, boolean isGetData, IRemoteServiceCallback callback) throws RemoteException {
                if (callback != null) {
                    setCellback(funcID, isGetData, callback);
                } else {
                    Log.d(TAG, "callback is null");
                }
            }
        };

        CellbackControl() {
            callback = null;
            funcID = -1;
            isGetData = false;
        }

        void setCellback(int funcID, boolean isGetData, IRemoteServiceCallback callback) {
            if (funcID == MainActivity.settingResult) {
                settingInitial();
                return;
            }
            settingInitial();
            this.funcID = funcID;
            this.isGetData = isGetData;
            this.callback = callback;
        }

        IRemoteService.Stub getStub() {
            return stub;
        }

        boolean isCallback() {
            return isGetData;
        }

        void Cellback(String aString) {
            if (isGetData) {
                // 目前只打算送一次，有需要的話在呼叫就好了
                isGetData = false;
                try {
                    callback.basicTypesCallback(funcID, aString);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        // app 在連接時，回傳 stub 給他即可
        if (cellbackControl == null)
            cellbackControl = new CellbackControl();
        return cellbackControl.getStub();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return flags;
    }

    // 取得目前日期時間
    public String getDate() {
        // kk:24小時制, hh:12小時制
        return DateFormat.format("yyyy-MM-dd_kk.mm.ss", Calendar.getInstance().getTime()).toString();
    }
}
