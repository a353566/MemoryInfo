package com.mason.memoryinfo.RecordData;

/**
 * Created by LALALA on 2017/11/14.
 */


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;

import java.util.Calendar;

/**
 * Location 控制
 * 由此取出"定位"相關資訊。
 * 總共取出兩種資料，分別是以下兩種：
 * NETWORK 和 GPS 定位(雖然目前取不出GPS定位)
 * 註冊是用 LOCATION_INTERVAL_TIME 和 LOCATION_INTERVAL_DISTANCE的參數
 * 分別是兩次定位的時間和距離的間隔參數
 */
public class LocationRecord implements RecordData {
    private String TAG = "RecordData LocationRecord";
    private LocationManager locationManager;

    public int LOCATION_INTERVAL_TIME = 30 * 60000;     // 兩次 location 最長更新時間 (ms)
    public int LOCATION_INTERVAL_DISTANCE = 500;     // 兩次定位更新距離 (公尺)

    private boolean locationRegisterSuccess;        // 是否有成功註冊定位

    public LocationRecord(Context context) {
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // 註冊 定位 回傳資訊，需要檢查是否有權限可以用
        locationRegisterSuccess = false;
        if (Build.VERSION.SDK_INT >= 23 && context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "v23 is true. GPS request");
            // Internet 定位
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL_TIME, LOCATION_INTERVAL_DISTANCE, new InternetReceiver());
            // GPS 定位
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_INTERVAL_TIME, LOCATION_INTERVAL_DISTANCE, new GPSReceiver());
            locationRegisterSuccess = true;
            Log.d(TAG, "GPS request OK!!");
        }
    }

    public boolean isGPSOpen() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public boolean isNeedRecord() {
        return !(isGPSRecord && isInternetRecord);
    }

    @Override
    public boolean canRecord() {
        return true;
    }

    @Override
    public String recordData() {
        // 紀錄定位資訊

        StringBuilder output = new StringBuilder();
        output.append("Location:");
        //location.getLongitude();  // 得到經度
        //location.getLatitude();   // 得到緯度
        if (!isGPSRecord) {
            output.append("GPS:");
            output.append(gpsLocation.getLongitude()).append(',').append(gpsLocation.getLatitude()).append('|');
            output.append(gpsLocationTime).append("||");
            isGPSRecord = true;
        }

        if (!isInternetRecord) {
            output.append("Internet:");
            output.append(internetLocation.getLongitude()).append(',').append(internetLocation.getLatitude()).append('|');
            output.append(internetLocationTime).append("||");
            isInternetRecord = true;
        }

        return output.toString();
    }

    // 接收 GPS 定位完後的回傳資訊
    private Location gpsLocation;
    private String gpsLocationTime;
    private boolean isGPSRecord = true;
    private class GPSReceiver implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            // 當位置更新時呼叫，並傳入對應的 Location
            gpsLocation = location;
            gpsLocationTime = getDate();
            isGPSRecord = false;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // 當狀態發生改變時呼叫
        }

        @Override
        public void onProviderEnabled(String provider) {
            // 當所選的 Location Provider 可用時呼叫
        }

        @Override
        public void onProviderDisabled(String provider) {
            // 當所選的 Location Provider 不可用時呼叫
        }

    }

    // 接收 internet 定位完後的回傳資訊
    private Location internetLocation;
    private String internetLocationTime;
    private boolean isInternetRecord = true;
    private class InternetReceiver implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            // 當位置更新時呼叫，並傳入對應的 Location
            internetLocation = location;
            internetLocationTime = getDate();
            isInternetRecord = false;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // 當狀態發生改變時呼叫
        }

        @Override
        public void onProviderEnabled(String provider) {
            // 當所選的 Location Provider 可用時呼叫
        }

        @Override
        public void onProviderDisabled(String provider) {
            // 當所選的 Location Provider 不可用時呼叫
        }

    }

    // 取得目前日期時間 只有時間沒有日期
    private String getDate() {
        // kk:24小時制, hh:12小時制
        return DateFormat.format("kk:mm:ss", Calendar.getInstance().getTime()).toString();
        //return DateFormat.format("yyyy-MM-dd_kk:mm:ss", Calendar.getInstance().getTime()).toString();
    }
}