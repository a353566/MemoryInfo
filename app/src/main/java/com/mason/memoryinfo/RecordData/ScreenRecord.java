package com.mason.memoryinfo.RecordData;

/**
 * Created by LALALA on 2017/11/14.
 */

import android.os.PowerManager;

/**
 * ScreenManager 控制
 * 由此取出螢幕是否是亮的
 */
public class ScreenRecord implements RecordData {
    private PowerManager powerManager;
    private boolean lastScreen;

    public ScreenRecord(PowerManager powerManager) {
        this.powerManager = powerManager;
        lastScreen = false;
    }

    public boolean isScreenOn() {
        return powerManager.isScreenOn();
    }

    @Override
    public boolean isNeedRecord() {
        return lastScreen != isScreenOn();
    }

    @Override
    public boolean canRecord() {
        return true;
    }

    @Override
    public String recordData() {
        lastScreen = isScreenOn();
        return lastScreen ? "Screen:On" : "Screen:Off";
    }
}