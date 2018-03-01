package com.mason.memoryinfo.RecordData;

/**
 * Created by LALALA on 2017/11/14.
 */

public interface RecordData {
    // 主要是做許多判斷，並回傳是否要記錄
    boolean isNeedRecord();
    // 取得資料前詢問是否可以取得，主要是有些資料要等待回傳
    boolean canRecord();
    // 取得資料
    String recordData();
}
