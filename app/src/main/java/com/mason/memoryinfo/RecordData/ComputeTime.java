package com.mason.memoryinfo.RecordData;

/**
 * Created by LALALA on 2017/11/14.
 */

/**
 * 計算時間
 * 呼叫 startTime() 就會開始計時，或是重新計時。
 * getComputeTime() 則會回傳當時記時點到現在的總時間，但不會重新計時。
 */
public class ComputeTime {
    long startTime = 0;

    public void startTime() {
        startTime = System.currentTimeMillis();
    }

    public long getComputeTime() {
        return System.currentTimeMillis() - startTime;
    }
}