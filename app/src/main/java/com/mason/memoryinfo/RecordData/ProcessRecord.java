package com.mason.memoryinfo.RecordData;

/**
 * Created by LALALA on 2017/11/14.
 */

import android.app.ActivityManager;
import android.os.Debug;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * ProcessManager
 * 這裡實作取出 proc 目錄檔的資料，並將其整理成 String 來讓之後好寫入檔案。
 * 有 全部搜尋 和 小搜尋 兩種方法。
 * 小蒐尋主要是觀察需不需要及時做更完整的搜尋。
 */
public class ProcessRecord implements RecordData{
    private static final String TAG = "autoRecordingLog:ProcessRecord";
    // 基本設定
    public boolean isFullScan = false;
    // 取得 activityManager，之後需要從此取出使用的的記憶體量
    private ActivityManager activityManager;
    // 要從這裡取出螢幕亮度
    private ScreenRecord screenRecord;

    private int procAmount;             // 目前有幾個 proc 是重要的
    private ProcessData[] procArray;   // 分別是哪些 pid

    // 時間常數設定，目前是預設 (ms)
    private double screenONbigScanCD = 60 * 1000;      // 螢幕開啟時，全部掃描 等待時間
    private double screenONsmallScanCD = 20 * 1000;     // 螢幕開啟時，小掃描 等待時間
    private double screenOFFbigScanCD = 30 * 60 * 1000;    // 螢幕關閉時，全部掃描 等待時間
    private double screenOFFsmallScanCD = 3 * 60 * 1000;   // 螢幕關閉時，小掃描 等待時間

    private ComputeTime bigScanInterval;
    private ComputeTime smallScanInterval;

    // 是否已經紀錄了
    private boolean isBigScan;
    private boolean isSmallScanRecord;
    private boolean isBigScanRecord;

    // 螢幕是否亮的狀態，在取出資料時才會做改寫 recordData()
    private boolean isLastScreenON;

    public ProcessRecord(ActivityManager activityManager, ScreenRecord screenRecord) {
        this.activityManager = activityManager;
        this.screenRecord = screenRecord;
        procAmount = 0;
        procArray = new ProcessData[128];

        isSmallScanRecord = true;
        isBigScanRecord = true;
        isLastScreenON = false;
        // 時間，並初始化，讓他第一次掃描可以順利進行
        bigScanInterval = new ComputeTime();
        smallScanInterval = new ComputeTime();
        bigScanInterval.startTime -= screenOFFbigScanCD;
        smallScanInterval.startTime -= screenOFFsmallScanCD;
    }

    @Override
    public boolean isNeedRecord() {
        // 判斷是否掃描和哪一種掃描
        switch (scanState()) {
            case NO_SCAN :      // 不用掃描
                return false;
            case BIG_SCAN :     // 大掃描
                BigScan();
                return true;
            case SMALL_SCAN :   // 小掃描
                // PS: SmallScan()會自己判斷要不要做大掃描
                if (!SmallScan()) {
                    BigScan();
                }
                return true;
            default:
                Log.d(TAG, "scanState() is fall");
                return false;
        }
    }

    @Override
    public boolean canRecord() {
        return !(isSmallScanRecord && isBigScanRecord);
    }

    @Override
    public String recordData() {
        // 更改螢幕狀態
        isLastScreenON = screenRecord.isScreenOn();

        if (!isBigScanRecord) {
            // 整理輸出 big 檔案
            StringBuilder output = new StringBuilder();
            output.append("ProcessRecord:Big");
            output.append("\nprocNum:").append(procAmount);
            // 整理 Process 檔案
            for (int i = 0; i < procAmount; i++)
                output.append('\n').append(procArray[i].AllOutput());

            isBigScanRecord = true;
            isSmallScanRecord = true;
            return output.toString();
        } else if (!isSmallScanRecord) {
            // 整理輸出 small 檔案
            StringBuilder output = new StringBuilder();
            output.append("ProcessRecord:Small");
            output.append("\nprocNum:").append(procAmount);
            // 整理 Process 檔案
            for (int i = 0; i < procAmount; i++)
                output.append('\n').append(procArray[i].SmallOutput());

            isSmallScanRecord = true;
            return output.toString();
        }
        return null;
    }

    /** 取出對於自己(ProcessRecord)來說要睡多久的時間 */
    public long getSleepTime() {
        double bigSleepTime = 0;
        double smallSleepTime = 0;
        // 先判斷螢幕亮案來決定用哪種睡眠
        if (screenRecord.isScreenOn()) {
            bigSleepTime = screenONbigScanCD - bigScanInterval.getComputeTime();
            smallSleepTime = screenONsmallScanCD - smallScanInterval.getComputeTime();
        } else {
            bigSleepTime = screenOFFbigScanCD - bigScanInterval.getComputeTime();
            smallSleepTime = screenOFFsmallScanCD - smallScanInterval.getComputeTime();
        }

        // 取出比較小的
        double smallNum = smallSleepTime;
        if (bigSleepTime < smallNum)
            smallNum = bigSleepTime;

        return (smallNum>0) ? ((long) smallNum) : 0;
    }

    public void setFullScan(boolean FullScan) {
        this.isFullScan = FullScan;
    }

    /** 判斷是否掃描和哪一種掃描 */
    private static final int NO_SCAN = 100;
    private static final int BIG_SCAN = 101;
    private static final int SMALL_SCAN = 102;
    private int scanState() {
        boolean isNowScreenON = screenRecord.isScreenOn();
        // 先看螢幕最後的狀態來決定做不同的判斷
        // 上次是亮的
        if (isLastScreenON) {
            // 判斷亮的時間是否太久沒有掃描
            if (bigScanInterval.getComputeTime() >= screenONbigScanCD) {
                return BIG_SCAN;
            } else if (smallScanInterval.getComputeTime() >= screenONsmallScanCD) {
                return SMALL_SCAN;
            }
        }
        // 上次是暗的
        else {
            // 轉成亮的直接大掃描
            if (isNowScreenON) {
                return BIG_SCAN;
            }
            // 判斷暗的時間是否太久沒有掃描
            else if (bigScanInterval.getComputeTime() >= screenOFFbigScanCD) {
                return BIG_SCAN;
            } else if (smallScanInterval.getComputeTime() >= screenOFFsmallScanCD) {
                return SMALL_SCAN;
            }
        }
        // 上面都沒過的話，不需要掃描
        return NO_SCAN;
    }
    public boolean isBigScan() {
        return isBigScan;
    }

    /** Process 存檔的地方，之後再寫回檔案裡 */
    private class ProcessData {
        String name;        // process 名稱
        File File;          // 此 process 的檔案位置
        int pid;            // pid
        int TotalPss;       // TotalPss，用String是因為沒有轉成數字的必要
        String oom_score;   // oom_score，理由同上
        String oom_adj;     // 有取出來就有
        int ground;         // 0:null 1:foreground 2:background 3:other (-1未定義 = null)
        String otherGround; // ground = 3 才會用到

        // 給予一般的資料，這樣可以比較好檢查是否有問題
        ProcessData() {
            Reset();
        }

        void Reset() {
            name = null;
            File = null;
            pid = -1;
            TotalPss = -1;
            oom_score = null;
            oom_adj = null;
            ground = -1;
            otherGround = null;
        }

        /**
         * 回傳完整資料，範例如下
         * name|pid|TotalPss|oom_score|ground|oom_adj (沒有的話輸出 null)
         */
        StringBuilder AllOutput() {
            // StringBuilder 如果是單thread運用的話，用此比較好
            StringBuilder output = new StringBuilder();
            // name
            if (name != null) output.append(name);
            else output.append("null");
            output.append('|');

            // pid
            if (pid != -1) output.append(pid);
            else output.append("null");
            output.append('|');

            // TotalPss
            if (TotalPss != -1) output.append(TotalPss);
            else output.append("null");
            output.append('|');

            // oom_score
            if (oom_score != null) output.append(oom_score);
            else output.append("null");
            output.append('|');

            // Ground ((0:null 1:foreground 2:background 3:other (-1未定義 = null)
            if (ground != 3) output.append(ground);
            else {
                output.append(ground).append('_');
                if (otherGround != null) output.append(otherGround);
                else output.append("null");
            }
            output.append('|');

            // oom_adj
            if (oom_adj != null) output.append(oom_adj);
            else output.append("null");
            output.append('|');

            return output;
        }

        /**
         * 回傳一半資料，因為許多資料和完整搜尋類似，
         * 所以只回傳不同的資料即可，其他資料在整合時合併即可。範例如下
         * TotalPss|oom_score|ground|oom_adj (沒有的話輸出 null)
         */
        StringBuilder SmallOutput() {
            // StringBuilder 如果是單thread運用的話，用此比較好
            StringBuilder output = new StringBuilder();

            // TotalPss
            if (TotalPss != -1) output.append(TotalPss);
            else output.append("null");

            // oom_score
            if (oom_score != null) output.append('|').append(oom_score);
            else output.append('|').append("null");
            output.append('|');

            // Ground
            if (ground != 3) output.append(ground);
            else {
                output.append(ground).append('_');
                if (otherGround != null) output.append(otherGround);
                else output.append("null");
            }
            output.append('|');

            // oom_adj
            if (oom_adj != null) output.append(oom_adj);
            else output.append("null");
            output.append('|');

            return output;
        }

        // 檢查資料是否沒問題
        boolean isOK() {
            return (name != null) &&
                    (File != null) &&
                    (pid != -1) &&
                    (TotalPss != -1) &&
                    (oom_score != null) &&
                    (oom_adj != null) &&
                    (ground != 0);
        }
    }

    // 全部搜尋
    private void BigScan() {
        isBigScan = true;
        // 全部搜尋，所以重新整理小搜尋的數據
        procAmount = 0;
        bigScanInterval.startTime();
        // ========================= 取出重要的 process =========================
        // 取得系統檔案目錄的 proc 資料
        File[] files = new File("/proc").listFiles();
        for (File file : files) {
            // ========================= process 判定 =========================
            // 不能讀取 則跳過
            if (!file.canRead())
                continue;

            // ======= 取得 pid
            int pid = isNumeric(file.getName());
            // 取出的不是 "數字" 則跳過 (-1代表不是數字)
            if (pid == -1)
                continue;

            // ======= 取得 name。從 /cmdline 取得
            String name = catData(file, "/cmdline");
            // 沒有 name 的話 則跳過
            if (name == null)
                continue;
            // 過濾名字
            if (!filterName(name))
                continue;

            // ------- 到這裡的話，應該就是個 process 了
            // ========================= 取出資料並放入 procArray =========================
            // 將此 process 放入 procArray 中。null 的話就宣告一下
            ProcessData proc = procArray[procAmount];
            if (proc == null) {
                proc = new ProcessData();
                procArray[procAmount] = proc;
            }

            // ======= 放入 File、name、pid
            proc.File = file;
            proc.name = name;
            proc.pid = pid;

            // ======= 取得 oom_score。從 /oom_score 取得
            String oom_score = catData(file, "/oom_score");
            // 放入 oom_score
            if (oom_score != null)
                proc.oom_score = oom_score;

            // ======= 試著取出 oom_adj 看看
            String oom_adj = catData(file, "/oom_adj");
            // 放入 oom_adj
            if (oom_adj != null)
                proc.oom_adj = oom_adj;

            // ======= 取得 foreground 或是 background 或其他。從 /cpuset 取得
            String ground = catData(file, "/cpuset");
            //Log.d(TAG,ground);
            // 轉換成數字
            int groundType = WhatGround(ground);
            // 放入 ground
            proc.ground = groundType;
            if (groundType == 3)
                proc.otherGround = ground;

            // ======= 到這邊算是完成一個 process 資料，所以+1換下一個
            procAmount++;
        }
        // ------- 取完重要的 process

        // ========================= 取出 TotalPss =========================
        // 為了知道記憶體用量。一起取出，比較省時間
        getProcessTotalPss();
        isBigScanRecord = false;
    }

    // 小搜尋  ：  一有不正常的事情返回 false，讓主程式知道要利用大搜尋來記錄不尋常的訊息
    private boolean SmallScan() {
        isBigScan = false;
        smallScanInterval.startTime();
        for (int i = 0; i < procAmount; i++) {
            ProcessData procData = procArray[i];
            File file = procData.File;
            // ========================= process 判定 =========================
            // 不能讀取，算是例外
            if (!file.canRead()) {
                return false;
            }

            // ======= 取得 pid
            int pid = isNumeric(file.getName());
            if (pid == -1) {    // 取出的不是 "數字"，算是例外 (-1代表不是數字)
                return false;
            } else if (pid != procData.pid) {  // pid 數字不一樣的話，算是例外
                return false;
            }

            // ======= 取得 name。從 /cmdline 取得
            String name = catData(file, "/cmdline");

            if (name == null) {  // 沒有 name 的話，算是例外
                return false;
            } else if (!filterName(name)) {  // 過濾名字，沒過的話算是例外
                return false;
            } else if (!name.equals(procData.name)) { // name 不一樣的話，算是例外
                return false;
            }

            // ------- 到這裡的話，應該就是原本的 process 了
            // ========================= 更新 procArray 資料 =========================

            // ======= 取得 ground
            // 取得 foreground 或是 background 或其他。從 /cpuset 取得
            String ground = catData(file, "/cpuset");
            // 轉換成數字
            int groundType = WhatGround(ground);
            // 放入 ground
            procData.ground = groundType;
            if (groundType == 3)
                procData.otherGround = ground;

            // ======= 取得 oom_score。從 /oom_score 取得
            String oom_score = catData(file, "/oom_score");
            // 放入 oom_score
            if (oom_score != null)
                procData.oom_score = oom_score;

            // ======= 試著取出 oom_adj 看看
            String oom_adj = catData(file, "/oom_adj");
            // 放入 oom_adj
            if (oom_adj != null)
                procData.oom_adj = oom_adj;
        }
        // ------- 檢查完重要的 process

        // ========================= 取出 TotalPss =========================
        // 為了知道記憶體用量。一起取出，比較省時間
        getProcessTotalPss();
        isSmallScanRecord = false;
        return true;
    }

    // 類似 cat 資料的方法
    private String catData(File file, String line) {
        String data = null;
        try {
            FileReader mFileReader = new FileReader(file.getPath() + line);
            BufferedReader mBufferedReader = new BufferedReader(mFileReader);
            data = mBufferedReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    /**
     * 取出記憶體使用量
     * 目前最吃時間的函式
     */
    private boolean getProcessTotalPss() {
        if (!isFullScan) {
            for (int i = 0; i < procAmount; i++)
                procArray[i].TotalPss = -1;
            return false;
        }
        Log.d(TAG,"------------------------------getProcessTotalPss()------------------------------");
        int[] pids = new int[procAmount];

        // 將 pid 都放入 pids 中
        for (int i = 0; i < procAmount; i++)
            pids[i] = procArray[i].pid;

        // 再將資料接出來
        Debug.MemoryInfo[] memoryInfos = activityManager.getProcessMemoryInfo(pids);
        // 將記憶體用量寫回去
        for (int i = 0; i < procAmount; i++)
            procArray[i].TotalPss = memoryInfos[i].getTotalPss();

        return true;
    }

    // 轉成數字，不是的話回傳 -1
    private int isNumeric(String str) {
        for (int i = str.length(); --i >= 0; )
            if (!Character.isDigit(str.charAt(i)))
                return -1;

        return Integer.valueOf(str);
    }

    // 過濾部分不需要的 processes
    // true：要留著的  |  false：不需要留著的
    private boolean filterName(String name) {
        return !name.substring(0, 1).equals("/");
//        if (name.substring(0,8).equals("/system/")) return false;
//        if (name.substring(0,6).equals("/sbin/")) return false;
//        return true;
    }

    // 0:null 1:foreground 2:background 3:NOmatch
    private int WhatGround(String ground) {
        if (ground == null)
            return 0;
        else if (ground.equals("/background"))
            return 2;
        else if (ground.equals("/foreground"))
            return 1;
        else
            return 3;
    }
}
