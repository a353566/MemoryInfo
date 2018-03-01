package com.mason.memoryinfo.RecordData;

/**
 * Created by LALALA on 2017/11/14.
 */

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * SensorManager 控制
 */
public class SensorRecord implements RecordData {
    private SensorManager sensorManager;

    public SensorRecord(SensorManager sensorManager) {
        // (SensorManager) getSystemService(Context.SENSOR_SERVICE)
        this.sensorManager = sensorManager;
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(new G_sensor(), sensor, SensorManager.SENSOR_DELAY_NORMAL);
        GsensorDataA = new StringBuilder();
        GsensorDataB = new StringBuilder();
        isUseGsensorDataA = false;
    }

    // 分兩個字串，以免資料衝突
    private StringBuilder GsensorDataA;
    private StringBuilder GsensorDataB;
    private boolean isUseGsensorDataA;

    @Override
    public boolean isNeedRecord() {
        return true;
    }

    @Override
    public boolean canRecord() {
        return true;
    }

    @Override
    public String recordData() {
        StringBuilder output = new StringBuilder();
        output.append("G-sensor:");
        // 先處理 DataB
        output.append(GsensorDataB);
        GsensorDataB.setLength(0);
        // 數一百次來等待 Gsensor 存完資料
        while (true) {
            if (!isUseGsensorDataA) {
                isUseGsensorDataA = true;
                output.append(GsensorDataA);
                GsensorDataA.setLength(0);
                isUseGsensorDataA = false;
                break;
            }
        }
        return output.toString();
    }

    private class G_sensor implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // 整理數值
            StringBuilder output = new StringBuilder();

            for (int i=0; i<3; i++) {
                String str = String.valueOf(event.values[i]);
                int point = str.indexOf(".");
                if (point<=0 || point + 3 > str.length()) {
                    output.append(str);
                } else {
                    output.append(str.substring(0, point + 3));
                }
                if (i!=2)
                    output.append(',');
                else
                    output.append('|');
            }

            // 回傳加速規數值
            if (!isUseGsensorDataA) {
                isUseGsensorDataA = true;
                GsensorDataA.append(output);
                isUseGsensorDataA = false;
            } else {
                GsensorDataB.append(output);
            }

            // 回傳加速規數值
            //if (!isUseGsensorDataA) {
            //    isUseGsensorDataA = true;
            //    GsensorDataA.append(event.values[0]).append(',');
            //    GsensorDataA.append(event.values[1]).append(',');
            //    GsensorDataA.append(event.values[2]).append('|');
            //    isUseGsensorDataA = false;
            //} else {
            //    GsensorDataB.append(event.values[0]).append(',');
            //    GsensorDataB.append(event.values[1]).append(',');
            //    GsensorDataB.append(event.values[2]).append('|');
            //}
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }
}
