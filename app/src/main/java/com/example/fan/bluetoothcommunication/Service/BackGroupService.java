package com.example.fan.bluetoothcommunication.Service;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.fan.bluetoothcommunication.AtyClient;
import com.example.fan.bluetoothcommunication.Config;
import com.example.fan.bluetoothcommunication.CurentTimeString;
import com.example.fan.bluetoothcommunication.Speed;
import com.example.fan.bluetoothcommunication.Threads.ClientThread;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;

public class BackGroupService extends Service {
    public LocationManager mLocationManager;//位置管理器
    public Speed speedOp=new Speed(0);
    // 状态监听
    BroadcastReceiver alarB = new BroadcastReceiver() {
        int i = 0;

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("alarm".equals(intent.getAction())) {
                i += 1;
                if (ActivityCompat.checkSelfPermission(BackGroupService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(BackGroupService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
            }
        }
    };
    GpsStatus.Listener gpsStatusListener = new GpsStatus.Listener() {
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    break;
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS: // 卫星状态改变
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for Activity#requestPermissions for more details.
                        }
                    }
                    GpsStatus gpsStatus = mLocationManager.getGpsStatus(null); // 获取当前状态
                    int maxSatellites = gpsStatus.getMaxSatellites(); // 获取卫星颗数的默认最大值
                    Iterator<GpsSatellite> iters = gpsStatus.getSatellites()
                            .iterator(); // 创建一个迭代器保存所有卫星
                    int count = 0;
                    while (iters.hasNext() && count <= maxSatellites) {
                        GpsSatellite s = iters.next();
                        count++;
                    }
                    break;
                case GpsStatus.GPS_EVENT_STARTED: // 定位启动
                    break;
                case GpsStatus.GPS_EVENT_STOPPED: // 定位结束
                    break;
            }
        };
    };@Nullable
    @Override
    public IBinder onBind(Intent intent) {
        AlarmManager alarmManager;
        alarmManager= (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent1 = new Intent("alarm");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent1, 0);
        alarmManager.set(AlarmManager.RTC_WAKEUP, 20000, pendingIntent);
        IntentFilter alarintentFilter=new IntentFilter("alarm");
        registerReceiver(alarB,alarintentFilter);

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE); // 位置
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        } else {
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
            String bestProvider = mLocationManager.getBestProvider(
                    getLocationCriteria(), true);
            Location location = mLocationManager
                    .getLastKnownLocation(bestProvider);
            mLocationManager.addGpsStatusListener(gpsStatusListener);
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
        }
        return new MyBinder();
    }

    private Criteria getLocationCriteria() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setSpeedRequired(true); // 设置是否要求速度
        criteria.setCostAllowed(false); // 设置是否允许运营商收费
        criteria.setBearingRequired(false); // 设置是否需要方位信息
        criteria.setAltitudeRequired(false); // 设置是否需要海拔信息
        criteria.setPowerRequirement(Criteria.POWER_LOW); // 设置对电源的需求
        return criteria;
    }
    private LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            Toast.makeText(BackGroupService.this, "Location update"+location.getSpeed(), Toast.LENGTH_SHORT).show();
            updateSpeedByLocation(location);

        }
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.AVAILABLE: // GPS状态为可见时
                    Toast.makeText(BackGroupService.this,"GPS状态可见",Toast.LENGTH_SHORT).show();
                    break;
                case LocationProvider.OUT_OF_SERVICE: // GPS状态为服务区外时
                    Toast.makeText(BackGroupService.this,"GPS在服务区外",Toast.LENGTH_SHORT).show();
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE: // GPS状态为暂停服务时
                    Toast.makeText(BackGroupService.this,"GPS暂停服务",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
        public void onProviderEnabled(String provider) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for Activity#requestPermissions for more details.
                }
            }
            Location location = mLocationManager.getLastKnownLocation(provider);
            updateSpeedByLocation(location);
        }
        public void onProviderDisabled(String provider) {
            // updateView(null);
            Toast.makeText(BackGroupService.this,"ProviderDisabled",Toast.LENGTH_SHORT).show();
        }
    };

    File gpsFile;
    private void updateSpeedByLocation(Location location) {
        int number = ClientThread.number;
        gpsFile = new File(Environment.getExternalStorageDirectory(), "/gps/" + number + ".txt");
        gpsFile.getParentFile().mkdirs();
        if (!gpsFile.exists()) {
            try {
                gpsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        int tempSpeed = Math.round(location.getSpeed()); // m/s --> Km/h
        speedOp.setSpeed(tempSpeed);
        writeToFile(Math.round(tempSpeed));
    }
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
    public class MyBinder extends Binder {
        public void showToast(){
            Toast.makeText(BackGroupService.this,"showToast",Toast.LENGTH_SHORT).show();
        }
        public void showList(){
            Toast.makeText(BackGroupService.this, "showList", Toast.LENGTH_SHORT).show();
        }
        public Speed getSpeed(){
            return speedOp;
        }
        public boolean setBoolean(){
            return true;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("MyService","onstartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
}
