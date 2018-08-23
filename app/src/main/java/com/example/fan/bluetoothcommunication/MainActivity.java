package com.example.fan.bluetoothcommunication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private Button btnSearch;
    private ListView listView;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> listAdapter;
    private List<BluetoothDevice> deviceList;

    /**
     * The {@link ViewPager} that will host the section contents.
     */

    long timeStart ;
    long timeEnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();

        initEvent();


    }

    public void initView (){
        try {
            timeStart = CurentTimeString.getTime1();


            setContentView(R.layout.activity_main);
            btnSearch = (Button) findViewById(R.id.btnSearch);
            listView = (ListView) findViewById(R.id.lvDevice);
            //获得蓝牙服务
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            //设备列表的适配器
            listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
            //设备列表数据
            deviceList = new ArrayList<>();

            if(listAdapter!=null) {
                listView.setAdapter(listAdapter);
            }
            timeEnd = CurentTimeString.getTime1();
            Log.e("hah", "time:" + (timeEnd - timeStart));
//        Toast.makeText(this, "左变道"+"  检测时长：" + (timeEnd- timeStart )+"毫秒", Toast.LENGTH_LONG).show();
//
//        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
//        getSystemService(VIBRATOR_SERVICE);//获得一个震动的服务
//        vibrator.vibrate(2000);
        }
        catch (RuntimeException e){
            Log.i("myerror","程序意外崩溃");
        }
}

    Vibrator vibrator;
    public  void initEvent(){

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar.make(v, "正在搜索", Snackbar.LENGTH_LONG).show();
                search();
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                gotoClient(position);
            }
        });
    }




    private void gotoClient(int position) {
        Intent intent = new Intent(this, AtyClient.class);
        intent.putExtra(Config.KEY_DEVICE, deviceList.get(position));
        startActivity(intent);
    }


    //搜寻蓝牙设备
    private void search() {
        try{
            deviceList.clear();
            listAdapter.clear();
            //判断蓝牙是否开启
            if (!bluetoothAdapter.isEnabled())
                bluetoothAdapter.enable();//开启蓝牙
            //判断蓝牙是否设置为被其他蓝牙设备可发现
            if (!bluetoothAdapter.isDiscovering()) {
                Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120000);//设置可被发现的事件
                startActivity(i);//弹出一个对话框，询问是否允许被发现
            }
            bluetoothAdapter.startDiscovery();//开启被发现
            //found device
            ////定义了BroadcastReceiver对象receiver,就可以来使用它来监听蓝牙查找情况

        }
        catch (RuntimeException e){
            Log.i("myerror","程序意外崩溃");
        }
    }

    @Override
    protected void onResume() {
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, intentFilter);
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                if (intent.getAction() == BluetoothDevice.ACTION_FOUND) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    deviceList.add(device);
                    listAdapter.add(device.getName());
                } else if (intent.getAction() == BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                    Toast.makeText(MainActivity.this, "搜索完成", Toast.LENGTH_SHORT).show();
            }
           catch (RuntimeException e){
               Log.i("myerror","程序意外崩溃");
            }
        }
    };

}
