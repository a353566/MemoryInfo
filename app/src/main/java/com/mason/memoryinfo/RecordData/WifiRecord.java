package com.mason.memoryinfo.RecordData;

/**
 * Created by LALALA on 2017/11/14.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;
import android.support.annotation.NonNull;

import java.util.List;

/**
 * Location 控制
 * 由此取出"定位"相關資訊
 * 主要目的是利用 wifi & GPS 觀察使用者所在地區
 */
public class WifiRecord implements RecordData {
    private String TAG = "RecordData WifiRecord";
    private WifiManager wifiManager;
    private LocationManager locationManager;

    public double WIFI_SCAN_INTERVAL_TIME = 60000;   // WiFi 兩次掃描要相隔的時間 (ms)

    private boolean isScanWiFiNow;  // 是不是正在掃描
    private boolean isRecordData;   // 是不是已經紀錄了
    private ComputeTime scanIntervalTime;// 計算時間

    private List<ScanResult> scanResultList;    // 掃描結果

    public WifiRecord(Context context) {
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.wifiManager = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE));

        // 註冊 WiFi 回傳資訊
        context.registerReceiver(new WifiReceiver(), new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        scanIntervalTime = new ComputeTime();

        scanResultList = null;
        isScanWiFiNow = false;
        isRecordData = true;
        // 先第一次掃描
        // 先減掉基本等待時間，讓第一次可以順利執行
        scanIntervalTime.startTime();
        scanIntervalTime.startTime -= WIFI_SCAN_INTERVAL_TIME;
    }

    /** 呼叫掃描 */
    private void wifiScan() {
        // 沒有正在掃描、WiFi是開的
        if (isScanWiFiNow || !isRecordData) {
            return;
        } else if (!isWiFiOpen()) {
            isScanWiFiNow = false;
            isRecordData = false;
            return;
        }

        // 可以開始掃瞄了
        wifiManager.startScan();
        scanIntervalTime.startTime();
        isScanWiFiNow = true;
    }

    public boolean isWiFiScanIntervalTimeOK() {
        return scanIntervalTime.getComputeTime() > WIFI_SCAN_INTERVAL_TIME;
    }
    public boolean isGPSOpen() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    public boolean isWiFiOpen() {
        return wifiManager != null && wifiManager.isWifiEnabled();
    }

    private String wifiConnectMac = "00:00:00:00:00:00";   // 初始化，有沒有連線
    @NonNull
    private String connectData() {
        // 取得連線的 MAC，isConnectChange()會用來判斷有沒有改變連線
        wifiConnectMac = wifiManager.getConnectionInfo().getBSSID();

        StringBuilder output = new StringBuilder();
        output.append("WiFiConnect:");

        // 對方 MAC 為 "00:00:00:00:00:00" 就是沒有連接
        if (wifiConnectMac == null || wifiConnectMac.equals("00:00:00:00:00:00")) {
            wifiConnectMac = "00:00:00:00:00:00";
            output.append("Disconnect");
        }
        // 已連線，回傳連線資訊
        else {
            // MAC
            output.append(wifiManager.getConnectionInfo().getBSSID());
            // Name
            output.append(',').append(wifiManager.getConnectionInfo().getSSID());
        }

        return output.toString();
    }
    /** 回傳連線是否已改變 */
    private boolean isConnectChange() {
        // 利用 Mac 來得知有沒有變化
        String newwifiConnectMac = wifiManager.getConnectionInfo().getBSSID();
        if (newwifiConnectMac == null || newwifiConnectMac.equals("00:00:00:00:00:00")) {
            return !(wifiConnectMac == null || wifiConnectMac.equals("00:00:00:00:00:00"));
        } else {
            return (wifiConnectMac == null) || !(newwifiConnectMac.equals(wifiConnectMac));
        }
    }
    /** 回傳所有 WiFiList 資料 */
    private String scanResultOutput() {
        if (scanResultList == null) return "null";

        StringBuilder output = new StringBuilder();
        for (ScanResult scanResult : scanResultList) {
            // 寫入 MAC
            output.append(scanResult.BSSID);
            // 寫入名字
            output.append(",\"");
            output.append(scanResult.SSID);
            output.append("\"|");
        }
        output.append("unll");
        return output.toString();
    }

    /**
     *  呼叫後檢查是否需要記錄　Wi-Fi 紀錄
     *  true : 1. 正要開始掃描
     *         2. 或是 已經有掃描結果
     *  false : 1. 間隔時間還不夠久
     *          2. 沒有切換連線的 Wi-Fi
     */
    @Override
    public boolean isNeedRecord() {
        boolean isNeed = false;
        // 如果正在掃描的話、如果有結果還沒紀錄的話
        // 直接回傳 true
        if (isScanWiFiNow || !isRecordData){
            return true;
        }
        // 如果連線改變的話就要重新掃描
        else if (isConnectChange()) {
            isNeed = true;
        }
        // 判斷時間間隔來知道是否需要紀錄
        else if (isWiFiScanIntervalTimeOK()) {
            isNeed = true;
        }

        // 需要掃描的話，需要啟動掃描
        if (isNeed) {
            wifiScan();
            return true;
        }
        return false;
    }

    @Override
    public boolean canRecord() {
        return !isRecordData;
    }

    @Override
    public String recordData() {
        // 標記已記錄
        isRecordData = true;
        // 先判斷 WiFi 有沒有打開
        if (!isWiFiOpen()) {
            // 沒有的話，把舊資料清空
            scanResultList = null;
            return "WiFiSensor:null";
        }

        StringBuilder output = new StringBuilder();
        output.append("WiFiSensor:good\n");
        // WiFi 紀錄自己的連線
        output.append(connectData()).append('\n');
        // WiFi List
        output.append(scanResultOutput());

        return output.toString();
    }

    /** 接收 WiFi 掃描完後的回傳資訊 */
    class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent intent) {
            // 把結果寫入
            scanResultList = wifiManager.getScanResults();
            // 沒有在掃描了，也沒有紀錄
            isScanWiFiNow = false;
            isRecordData = false;
        }
    }
}
