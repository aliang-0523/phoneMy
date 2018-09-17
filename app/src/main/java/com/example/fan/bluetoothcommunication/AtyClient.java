package com.example.fan.bluetoothcommunication;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fan.bluetoothcommunication.Threads.ClientThread;

import org.achartengine.GraphicalView;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Timer;


/**
 * Created by fan on 15-12-6.
 */
public class AtyClient extends Activity {


    private Button btnSend;
    private Button btnToggle;
    private TextView tvTitle, tvContent;
    private boolean isConnected;
    private BluetoothDevice device;
    private ClientThread clientThread;

    private LinearLayout xCurveLayout;// 存放x轴图表的布局容器
    private LinearLayout yCurveLayout;// 存放y轴图表的布局容器

    private GraphicalView mView, mView2;// 左右图表
    private ChartService mService, mService2;
    private Timer timer;

    private Vibrator vibrator;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aty_client);
        initView();

        initData();

        //  initEvent();

    }


    private void initView() {
        //连接的是蓝牙设备
        device = getIntent().getParcelableExtra(Config.KEY_DEVICE);

        //连接蓝牙设备的名称
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvTitle.setText(device.getName());
        btnToggle = (Button) findViewById(R.id.btnToggle);
        // 存放x轴图表的布局容器
        xCurveLayout = (LinearLayout) findViewById(R.id.ll_x_curve);
        // 存放y轴图表的布局容器
        yCurveLayout = (LinearLayout) findViewById(R.id.ll_y_curve);


        //获取x曲线变化图表
        mService = new ChartService(this);
        mService.setXYMultipleSeriesDataset("x轴曲线");  //获取数据集容器
        //        mService.setXYMultipleSeriesRenderer(100, 10, "x轴曲线", "时间", "x", //获取渲染器
        mService.setXYMultipleSeriesRenderer(100, 1000, "x轴曲线", "时间", "x", //获取渲染器
                Color.RED, Color.RED, Color.RED, Color.BLACK);
        mView = mService.getGraphicalView();  //获取x曲线变化图表


        //获取y曲线变化图表
        mService2 = new ChartService(this);
        mService2.setXYMultipleSeriesDataset("y轴曲线");
        //   mService2.setXYMultipleSeriesRenderer(100, 10, "y轴曲线", "时间", "y",
        mService2.setXYMultipleSeriesRenderer(100, 1000, "y轴曲线", "时间", "y",
                Color.RED, Color.RED, Color.RED, Color.BLACK);
        mView2 = mService2.getGraphicalView();


        // 将图表添加到布局容器中
        xCurveLayout.addView(mView, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        yCurveLayout.addView(mView2, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

     /*   //设置定时器
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.obtainMessage(Config.TIMER).sendToTarget();

            }
        } , 1000 ,1000 );*/

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        getSystemService(VIBRATOR_SERVICE);//获得一个震动的服务


    }


    private void initData() {

        //开启客户端线程
        clientThread = new ClientThread(device, handler, this);
        clientThread.start();
        btnToggle.setEnabled(false);
        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.equals(btnToggle.getText(), "启动")) {
                    btnToggle.setText("停止");
                    clientThread.addFileNumber();
                    clientThread.PAUSE_WRITE = false; //将加速度传感器数据写入文档的标志位
                    isWritingGPS = true; //将GPS数据写入文档的标志位
                } else {
                    btnToggle.setText("启动");
                    clientThread.PAUSE_WRITE = true;
                    isWritingGPS = false;
                }
            }
        });
    }




/*
    private void initEvent() {

        //拍照
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // ClientThread.setIsWrite(true);
                Intent   intent =  new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                intent.addCategory("android.intent.category.DEFAULT");
                startActivityForResult(intent, 0);
            }
        });
    }*/


/*
    //返回到此界面调用该方法
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ClientThread.setIsWrite(false);
        tvContent.append("传速完成");
    }
*/

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Config.STATUES_CONNECT_SUCCESS:
                    //  btnSend.setEnabled(true);
                    // tvContent.append(CurentTimeString.getTime() + "->>" + "连接成功\n");
                    isConnected = true;
                    btnToggle.setEnabled(true);
                    isWritingGPS = false;
                  startGPS();
                    break;
                case Config.STATUES_CONNECT_FAILED:
                    //  btnSend.setEnabled(false);
                    //   tvContent.append(CurentTimeString.getTime() + "->>" + "连接失败\n");
                    break;
                case Config.STATUES_READ_FAILED:
                    //  tvContent.append(CurentTimeString.getTime() + "->>" + "读取失败\n");
                    break;
                case Config.STATUES_READ_SUCCESS:
                    // tvContent.append(CurentTimeString.getTime() + "->>接收：" + msg.obj.toString() + "\n");
                    //                    System.out.println(msg.obj.toString());
                    //开始绘图
                    drawPicture(msg.obj.toString());

                    break;
                case Config.STATUES_WRITE_FAILED:
                    //  tvContent.append(CurentTimeString.getTime() + "->>" + "写入\n");
                    break;
                case Config.STATUES_WRITE_SUCCESS:
                    //   tvContent.append(CurentTimeString.getTime() + "->>发送："+msg.obj.toString() +
                    //           "    ---->>>>发送成功\n");
                    break;
                case Config.STATUES_RECOGNIZE_SUCCESS:
                    Toast.makeText(AtyClient.this, msg.obj+"", Toast.LENGTH_LONG).show();
                    vibrator.vibrate(2000);

                    break;
            }
        }


    };
//    //存放识别结果的文件
    File filenameResult;


    private void ResultwriteToFile(String a) throws IOException {



        filenameResult = new File(Environment.getExternalStorageDirectory(), "/result/"  + ".txt");

        filenameResult.getParentFile().mkdirs();
        if (!filenameResult.exists()) {
            filenameResult.createNewFile();
        }


        method3(filenameResult, a);


    }

    /**
     * 追加文件：使用RandomAccessFile
     *
     * @param fileName 文件名
     * @param content  追加的内容
     */

    public static void method3(File fileName, String content) {
        //    if(!isWrite) return ;
        try {
            // 打开一个随机访问文件流，按读写方式
            RandomAccessFile randomFile = new RandomAccessFile(fileName, "rw");
            // 文件长度，字节数
            long fileLength = randomFile.length();
            // 将写文件指针移到文件尾。
            randomFile.seek(fileLength);
            randomFile.writeBytes(content + "\r\n");
            randomFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startGPS() {
        Toast.makeText(AtyClient.this, "StartGPS", Toast.LENGTH_SHORT).show();
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //获取到GPS_PROVIDER

        Location location = getLocation();

        //更新位置信息显示到TextView
        updata(location);
    }

    private Location getLocation() {
        //获取位置管理服务
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        //查找服务信息
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE); //定位精度: 最高
        criteria.setAltitudeRequired(false); //海拔信息：不需要
        criteria.setBearingRequired(false); //方位信息: 不需要
        criteria.setCostAllowed(true);//是否允许付费
        criteria.setPowerRequirement(Criteria.POWER_LOW); //耗电量: 低功耗
        criteria.setSpeedRequired(true);        // 对速度是否关注
        String provider = mLocationManager.getBestProvider(criteria, true); //获取GPS信息
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                return null;
            }
        }
        Location location = mLocationManager.getLastKnownLocation(provider);
        mLocationManager.requestLocationUpdates(provider, 200, 0, locationListener);
        return location;
    }

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {


            // TODO Auto-generated method stub
            if (location != null)
                System.out.println("GPS定位信息:");
            updata(location);
        }

        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }

        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub

        }
    };


    private LocationManager mLocationManager;//位置管理器


    private boolean isWritingGPS;

    private void updata(Location location) {
//        Toast.makeText(AtyClient.this, "speed:" + location.getSpeed(), Toast.LENGTH_SHORT).show();
        if (isWritingGPS) {
            int number = clientThread.number;
            gpsFile = new File(Environment.getExternalStorageDirectory(), "/gps/" + number + ".txt");
            gpsFile.getParentFile().mkdirs();
            if (!gpsFile.exists()) {
                try {
                    gpsFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (location != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("实时的位置信息:\n");
                sb.append("经度:");
                sb.append(location.getLongitude());
                sb.append("\n纬度:");
                sb.append(location.getLatitude());
                sb.append("\b高度:");
                sb.append(location.getAltitude());
                sb.append("\n速度：");
                sb.append(location.getSpeed());
                sb.append("\n方向：");
                sb.append(location.getBearing());
                System.out.println(sb);
            }

            float speedTempVar = location.getSpeed();
            clientThread.setSpeedTempVar(Math.round(speedTempVar));
            writeToFile(Math.round(speedTempVar));
        }
    }

    File gpsFile;

    private void writeToFile(double speed) {
        try {
            // 打开一个随机访问文件流，按读写方式
            RandomAccessFile randomFile = new RandomAccessFile(gpsFile, "rw");
            // 文件长度，字节数
            long fileLength = randomFile.length();
            // 将写文件指针移到文件尾。
            randomFile.seek(fileLength);
            randomFile.writeBytes(CurentTimeString.getTime() + ":" + speed + "" + "\r\n");
            randomFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    int t = 0;

    private void drawPicture(String sensorData) {
        String[] datas = sensorData.split(" ");
        mService.updateChart(t, Double.parseDouble(datas[2]));
        mService2.updateChart(t, Double.parseDouble(datas[3]));
        t += 2.5;

    }


  /*  private void drawPicture (String sensorData ){
        String[]  datas = sensorData.split(" ");
        mService.updateChart(Double.parseDouble(datas[2]),Double.parseDouble(datas[3]));

    }*/


    @Override
    protected void onDestroy() {
        clientThread.cancel();
        super.onDestroy();
    }
}
