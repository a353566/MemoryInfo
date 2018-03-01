package com.mason.memoryinfo.RecordData;

/**
 * Created by LALALA on 2017/11/14.
 */

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.BatteryManager;
import android.content.Context;
import android.content.IntentFilter;

/**
 * 電池相關的資訊
 * 如果有變化的話就會把 isDataChange 改為 true，
 * 讓主程式可以知道，並將其寫入檔案。
 */
public class BatteryRecord implements RecordData {
    private int BatteryN;           // 目前電量
    private int BatteryStatus;      // 電池狀態
    private boolean isDataChange;   // 資料是否有變化

    public BatteryRecord(Context context) {
        context.registerReceiver(new BatteryReceiver(), new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        BatteryN = -1;
        BatteryStatus = 0;
        isDataChange = false;
    }

    // 回傳電池狀態的字串
    private String getBatteryStatus() {
        switch (BatteryStatus) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return "Charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                return "Discharging";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return "NotCharging";
            case BatteryManager.BATTERY_STATUS_FULL:
                return "Full";
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
                return "Unknown";
            default:
                return "ReallyUnknown";
        }
    }

    /** 電池電量改變、電池狀態改變時
     *  就會回傳 true 要求記錄
     */
    @Override
    public boolean isNeedRecord() {
        return isDataChange;
    }

    @Override
    public boolean canRecord() {
        return true;
    }

    @Override
    public String recordData() {
        isDataChange = false;
        return "Battery:" + BatteryN + "%||status:" + getBatteryStatus();
    }

    class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 如果捕捉到的 action 是 ACTION_BATTERY_CHANGED，就運行 onBatteryInfoReceiver()
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                //double BatteryT = intent.getIntExtra("temperature", 0); //電池溫度
                //int BatteryV = intent.getIntExtra("voltage", 0); //電池電壓

                //目前電量
                int tempBatteryN = intent.getIntExtra("level", 0);
                //電池狀態
                int tempBatteryStatus = intent.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN);

                // 有變化的話 isDataChange 改成 true
                if (BatteryN != tempBatteryN || BatteryStatus != tempBatteryStatus) {
                    BatteryN = tempBatteryN;
                    BatteryStatus = tempBatteryStatus;
                    isDataChange = true;
                }
            }
//            String BatteryTemp; //電池使用情況
//            switch (intent.getIntExtra("health", BatteryManager.BATTERY_HEALTH_UNKNOWN)) {
//                case BatteryManager.BATTERY_HEALTH_UNKNOWN:
//                    BatteryTemp = "未知錯誤";
//                    break;
//                case BatteryManager.BATTERY_HEALTH_GOOD:
//                    BatteryTemp = "狀態良好";
//                    break;
//                case BatteryManager.BATTERY_HEALTH_DEAD:
//                    BatteryTemp = "電池沒有電";
//                    break;
//                case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
//                    BatteryTemp = "電池電壓過高";
//                    break;
//                case BatteryManager.BATTERY_HEALTH_OVERHEAT:
//                    BatteryTemp = "電池過熱";
//                    break;
//            }
        }
    }
}