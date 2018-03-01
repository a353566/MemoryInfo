package com.mason.memoryinfo;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    // 專門控制輸出
    private OutputControl outputControl;

    private Intent serviceIntent;
    private boolean isBind = false;
    // Callback
    private IRemoteServiceCallback mIRemoteServiceCallback = new IRemoteServiceCallback.Stub() {
        @Override
        public void basicTypesCallback(int funcID, final String outputDataPackage) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    outputControl.OutputData(outputDataPackage);
                    //((TextView) findViewById(R.id.test)).setText(outputDataPackage);
                    // text 滑動設定
                    //((TextView) findViewById(R.id.test)).setMovementMethod(ScrollingMovementMethod.getInstance());
                }
            });
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        // 與 Service 連線建立時會呼叫
        public void onServiceConnected(ComponentName className, IBinder service) {
            // 用 IRemoteService.Stub.asInterface(service) 取出連線的 Stub
            // 之後就可以呼叫此 interface 來溝通
            outputControl.setIRemoteService(IRemoteService.Stub.asInterface(service));
        }

        // 與 Service 意外斷開連線時呼叫
        public void onServiceDisconnected(ComponentName className) {
            Log.e("MainActivity", "Service has unexpectedly disconnected");
            outputControl.setIRemoteService(null);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // outputControl 宣告，並傳入 ListView
        outputControl = new OutputControl((ListView) findViewById(R.id.listview));

        // 開始 Service
        serviceIntent = new Intent();
        serviceIntent.setAction("service.Record");
        serviceIntent.setPackage("com.mason.memoryinfo");
        startService(serviceIntent);

        // 連接 Service，也開始輸出
        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
                isBind = true;
                outputControl.setContinue(true);
            }
        });

        // 停止 Bind，也停止輸出
        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBind) {
                    unbindService(mConnection);
                    isBind = false;
                }
                outputControl.setContinue(false);
            }
        });

        // 取得各種權限
        getPermissions();
    }

    private class OutputControl {
        private int funcID;
        private boolean continueOutput;
        private IRemoteService mIRemoteService;

        // output listView
        private ListView listView;
        private MyAdapter adapter;
        OutputControl(ListView listView) {
            funcID = 0;
            continueOutput = false;
            this.listView = listView;
        }

        void setIRemoteService(IRemoteService mIRemoteService) {
            this.mIRemoteService = mIRemoteService;
            Continue();
        }

        void setContinue(boolean continueOutput) {
            this.continueOutput = continueOutput;
        }

        boolean Continue() {
            if (continueOutput && mIRemoteService != null) {
                funcID++;
                try {
                    mIRemoteService.basicTypes(funcID, true, mIRemoteServiceCallback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            return continueOutput;
        }

        void OutputData(String data) {
            // 只對 Big 搜尋做輸出
            if (data.substring(14, 17).equals("Big")) {
                List<ProcessData> processDataList = new LinkedList<>();

                int point = 0;
                // 先跳過兩行
                for (int i=0; i<2; i++)
                    point = 1 + data.indexOf('\n', point);

                while(true) {
                    int end = data.indexOf('\n', point);
                    // 檢察是不是已經最後了，沒有資料了
                    if (0<end) {
                        processDataList.add(new ProcessData(data.substring(point, end)));
                        point = end+1;
                    } else {
                        break;
                    }
                }

                // 輸出資料
                if (adapter == null) {
                    adapter = new MyAdapter(MainActivity.this, processDataList);
                } else {
                    adapter.setProcessDataList(processDataList);
                }
                listView.setAdapter(adapter);
            }
            // 繼續下一次
            Continue();
        }

        private class ProcessData {
            String name;        // process 名稱
            String pid;         // pid
            String TotalPss;    // TotalPss，用String是因為沒有轉成數字的必要
            String oom_score;   // oom_score，理由同上
            String oom_adj;     // 有取出來就有
            String ground;      // 0:null 1:foreground 2:background 3:other (-1未定義 = null)

            ProcessData(String procString) {
                int begin, end;

                // name
                begin = 0;
                end = procString.indexOf("|", begin);
                name = procString.substring(begin, end);

                // pid
                begin = 1 + end;
                end = procString.indexOf("|", begin);
                pid = procString.substring(begin, end);

                // TotalPss
                begin = 1 + end;
                end = procString.indexOf("|", begin);
                TotalPss = procString.substring(begin, end);

                // oom_score
                begin = 1 + end;
                end = procString.indexOf("|", begin);
                oom_score = procString.substring(begin, end);

                // ground
                begin = 1 + end;
                end = procString.indexOf("|", begin);
                switch (procString.substring(begin, end)) {
                    case "0":
                        ground = "null";
                        break;
                    case "1":
                        ground = "foreground";
                        break;
                    case "2":
                        ground = "background";
                        break;
                    case "3":
                        ground = procString.substring(begin, end);
                        break;
                    default:
                        ground = "null";
                }

                // oom_adj
                begin = 1 + end;
                end = procString.indexOf("|", begin);
                if (end > 0)
                    oom_adj = procString.substring(begin, end);
                else
                    oom_adj = "null";

//                Log.d("00321", "name:" + name + "pid:" + pid + "TotalPss:" + TotalPss);
            }

            String getSubTitle() {
                return "pid：" + pid + "\tTotalPss：" + TotalPss + "\toom_score：" + oom_score + "\tground：" + ground + "\toom_adj：" + oom_adj;
            }
        }

        public class MyAdapter extends BaseAdapter {
            private LayoutInflater myInflater;
            private List<ProcessData> processDataList;

            public MyAdapter (Context context,List<ProcessData> processDataList) {
                myInflater = LayoutInflater.from(context);
                this.processDataList = processDataList;
            }

            @Override
            public int getCount() {
                return processDataList.size();
            }

            @Override
            public Object getItem(int position) {
                return processDataList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return processDataList.indexOf(processDataList.get(position));
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder;
                if (convertView == null) {
                    convertView = myInflater.inflate(R.layout.list_item, null);
                    holder = new ViewHolder(
                            (TextView) convertView.findViewById(R.id.title),
                            (TextView) convertView.findViewById(R.id.subtitle)
                    );
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                // 取出指定的 processData
                ProcessData processData = processDataList.get(position);
                holder.txtTitle.setText(processData.name);
                holder.txtSubTitle.setText(processData.getSubTitle());

                return convertView;
            }

            void setProcessDataList(List<ProcessData> processDataList) {
                this.processDataList = processDataList;
            }

            private class ViewHolder {
                TextView txtTitle;
                TextView txtSubTitle;
                ViewHolder(TextView txtTitle, TextView txtSubTitle){
                    this.txtTitle = txtTitle;
                    this.txtSubTitle = txtSubTitle;
                }
            }
        }
    }

    /**
     * 會要求的權限
     * 以及呼叫的函式 getPermissions()
     */
    private final static int REQUEST_CODE_CONTACT = 101;
    private static final String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private void getPermissions() {
        // 版本大於 6.0 的情况
        if (Build.VERSION.SDK_INT >= 23) {
            // 檢查是否已經取得權限
            boolean[] isOK = new boolean[permissions.length];
            int num = 0;
            for (int i=0; i<permissions.length; i++) {
                isOK[i] = (this.checkSelfPermission(permissions[i]) == PackageManager.PERMISSION_GRANTED);
                if (!isOK[i]) num++;
            }

            if (num != 0) {
                int tempN = 0;
                String[] tempPermissions = new String[num];
                for (int i=0; i<permissions.length; i++) {
                    if (!isOK[i]) {
                        tempPermissions[tempN++] = permissions[i];
                        Log.d("MainActivity","getPermissions():" + permissions[i]);
                    }
                }
                // 申請權限
                this.requestPermissions(tempPermissions, REQUEST_CODE_CONTACT);
            }
        }
    }
}
