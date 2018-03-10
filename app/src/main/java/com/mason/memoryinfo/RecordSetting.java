package com.mason.memoryinfo;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Objects;

import static android.provider.BaseColumns._ID;  //這個是資料庫都會有個唯一的ID
/**
 * Created by LALALA on 2018/3/8.
 */

public class RecordSetting extends SQLiteOpenHelper{
    // 喧告公用常數(final)
    private static final String TABLE_NAME = "setting";  //表格名稱
    private static final String NAME = "name";
    private static final String VALUE = "value";

    private final static String DATABASE_NAME = "setting.db";  //資料庫名稱
    private final static int DATABASE_VERSION = 1;  //資料庫版本

    public static final String Notification = "Notification";
    public static final String FullScan = "FullScan";
    public static boolean isNotification = true;
    public static boolean isFullScan = false;
    RecordSetting(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public boolean addSetting(String name, String value) {
        ContentValues values = new ContentValues();
        values.put(NAME, name);
        values.put(VALUE, value);

        getWritableDatabase().insert(TABLE_NAME, null, values);
        return true;
    }

    public String getSetting(String selectName) {
        String[] columns = {_ID, NAME, VALUE};
        @SuppressLint("Recycle") Cursor cursor = getReadableDatabase().query(TABLE_NAME, columns, null, null, null, null, null);

        String value;
        // 初始化
        if (Objects.equals(Notification, selectName)) {
            value = isNotification ? "true" : "false";
        } else if (Objects.equals(FullScan, selectName)) {
            value = isFullScan ? "true" : "false";
        } else {
            value = null;
        }

        while(cursor.moveToNext()){
            //int id = cursor.getInt(0);
            String name = cursor.getString(1);
            if (Objects.equals(name, selectName)) {
                value = cursor.getString(2);
            }
        }

        return value;
    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String INIT_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + NAME + " CHAR, " + VALUE + " CHAR);";
        sqLiteDatabase.execSQL(INIT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
