package com.mason.memoryinfo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.util.Objects;

import static com.mason.memoryinfo.RecordSetting.isFullScan;

/**
 * Created by LALALA on 2018/3/8.
 */

public class SettingActivity extends Activity {
    private CheckBox checkBox1;
    private CheckBox checkBox2;
    private boolean isNotification;
    private boolean isFullScan;
    private RecordSetting setting;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        // 建立 setting 資料庫
        settingInitial();
        checkBox1 = findViewById(R.id.checkBox1);
        checkBox2 = findViewById(R.id.checkBox2);
        checkBox1.setChecked(isNotification);
        checkBox2.setChecked(isFullScan);
        checkBox1.setOnCheckedChangeListener(chklistener);
        checkBox2.setOnCheckedChangeListener(chklistener);
    }

    private CheckBox.OnCheckedChangeListener chklistener = new CheckBox.OnCheckedChangeListener(){
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            isNotification = checkBox1.isChecked();
            isFullScan = checkBox2.isChecked();
            setting.addSetting(RecordSetting.Notification, isNotification ? "true" : "false");
            setting.addSetting(RecordSetting.FullScan, isFullScan ? "true" : "false");
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /** 基本設定 */
    private void settingInitial() {
        setting = new RecordSetting(this);
        setting.close();
        // Notification
        String temp = setting.getSetting(RecordSetting.Notification);
        isNotification = Objects.equals(temp, "true") || !Objects.equals(temp, "false") && RecordSetting.isNotification;
        // FullScan
        temp = setting.getSetting(RecordSetting.FullScan);
        isFullScan = Objects.equals(temp, "true") || !Objects.equals(temp, "false") && RecordSetting.isFullScan;
    }
}
