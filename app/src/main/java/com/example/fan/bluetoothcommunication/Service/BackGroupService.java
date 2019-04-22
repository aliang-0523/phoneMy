package com.example.fan.bluetoothcommunication.Service;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.example.fan.bluetoothcommunication.Speed;

public class BackGroupService extends Service {
    public LocationManager mLocationManager;//位置管理器
    public Speed speedOp=new Speed(0);
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = getLocation();
        return new MyBinder();
    }
    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            // TODO Auto-generated method stub
            Toast.makeText(BackGroupService.this, "Location changed", Toast.LENGTH_SHORT).show();
            if (location != null)
                System.out.println("GPS定位信息:");
            updateLoc(location);

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
    private void updateLoc(Location location) {
//        Toast.makeText(AtyClient.this, "speed:" + location.getSpeed(), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(BackGroupService.this, "updating speed", Toast.LENGTH_SHORT).show();
            speedOp.setSpeed(location.getSpeed());
    }
    public Location getLocation() {
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
        mLocationManager.requestLocationUpdates(provider, 200, 1, locationListener);
        return location;
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
