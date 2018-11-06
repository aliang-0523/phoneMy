package com.example.fan.bluetoothcommunication.Threads;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.fan.bluetoothcommunication.Complex;
import com.example.fan.bluetoothcommunication.Config;
import com.example.fan.bluetoothcommunication.CurentTimeString;
import com.example.fan.bluetoothcommunication.FFT;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by fan on 15-12-6.
 */
public class ClientThread extends Thread {

    private Handler handler;
    private InputStream in;
    private OutputStream out;
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private boolean isStopReading;
    private Context mContext;
    public static int anInt;

    private List<MyData> mCurrentMyData=new ArrayList<>();

    public static class MyData {
        float x;
        float y;
        float z;

        public MyData(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }


    public int number;
    public File filename;
    File filename1;
    File filename2;

    public ClientThread(BluetoothDevice device, Handler handler, Context context) {
        this.handler = handler;
        this.device = device;
        mContext = context;
    }


    public int addFileNumber() {
        SharedPreferences sp = mContext.getSharedPreferences("sensor", Context.MODE_PRIVATE);
        number = sp.getInt("number", 1);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("number", number + 1);
        editor.commit();
        return number;
    }

    public int getNumber() {
        return number;
    }


    @Override
    public void run() {
        try {
            //通过deceive来创建BluetoothSocket
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString(Config.UUID));
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("TAG", "socket 创建失败" + e.toString());
            handler.sendEmptyMessage(Config.STATUES_CONNECT_FAILED);
        }


        //连接成功
        if (socket.isConnected()){
            handler.sendEmptyMessage(Config.STATUES_CONNECT_SUCCESS);
        }


        else
            handler.sendEmptyMessage(Config.STATUES_CONNECT_FAILED);

        //文件的读操作 一直接受读取手表发过来的加速度数据
        while (!isStopReading) {
            try {
                sleep(100);
                if (socket == null)
                    continue;

                if (in == null)
                    in = socket.getInputStream();


                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(in));

                String line = reader.readLine();
                //  if(!isWrite) continue; //在将数据写进手机根目录前，设置标志位

                if (line != null && line != "") {

                    //读取数据成功 显示在手机界面上
                    Message msg = new Message();
                    msg.what = Config.STATUES_READ_SUCCESS;
                    msg.obj = line;
                    handler.sendMessage(msg);

                    //当手机的GPS数据的speed不为0  再追加到传感器数据后面 并写入文件
//                      if(speedTempVar!=0){
                        line+=" "+speedTempVar;
                        writeToFile(line);
//                      }


                }

            } catch (IOException e) {
                e.printStackTrace();
                handler.sendEmptyMessage(Config.STATUES_READ_FAILED);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    //数据能量数组
    List<Double> EnergyArr=new ArrayList<>();
    //List<Double> EnergyArr = new ArrayList<>();
    //数据实部数组
    List<Double> shibuchaArr=new ArrayList<>();
    List<Double> E2array=new ArrayList<>();
    List<Double> Earray=new ArrayList<>();
    List<Double> Amp2array=new ArrayList<>();
    List<Double> timesArray=new ArrayList<>();
    List<Double> costArray=new ArrayList<>();
    List<Double> costArray2=new ArrayList<>();

    double[] timesarray={0.8,1.33,2.42,1.19};
    double[] timesarrayEvery={1,2,2,1};
    double[] timesarrayEvery2={4,3,2,1};
    boolean[] boolArray={true,true,true,true};
    int SWturned = 0;
    int predetect = 0;
    int alarming=0;
    int swturned=0;
    int tempBool=0;
    int tempNum=0;
    int In=0;
    int Sureturn=0;
    int swturn2=0;
//    List<Double> shibuchaArr = new ArrayList<>();
    long timeStart;
    long timeEnd;

    private void handleRecognize(int speed , List<MyData> list) {

            double Energy = 0;
//            if(speed < 16){
//                // E = 8.79 * exp(-((S*3.6+37.49)/47.92)^2)//matlab上能量计算方法
//                //转换到java上计算能量E
//                double s = - ((speed *3.6 + 37.49) / 47.92) * ((speed *3.6 + 37.49) / 47.92);
//                double e1 = Math.E;
//                double s1 =  Math.pow(e1, s);
//                Energy =  8.79 * s1;
//
//            }else{
//               // E= 8.79 * exp(-((S+31)/51.8)^2)
//                double s = - ((speed *3.6 + 31) / 51.8) * ((speed *3.6 + 31) / 51.8);
//                double e1 = Math.E;
//                double s1 =  Math.pow(e1, s);
//                Energy =  8.79 * s1;
//            }


        double s = - 0.021*speed*3.6;
        double s2=-0.09*speed*3.6;
        double e1 = Math.E;
        double s1 =  Math.pow(e1, s);
        double s12=Math.pow(e1,s2);
        Energy =  0.25 * s1 + 1.2*s12;
            int leftOrRight = -1;//0左 1右  -1无变道

            if (list != null && list.size() == 35) {


                timeStart = CurentTimeString.getTime1();   //开始检测时间
                                //Log.e("timeStart", "检测时间开始(毫秒):" + timeStart);

                Complex[] queue = new Complex[35];
                               //Log.e("calculate", "mFristData:" + mFristData);
                               //将原始数据平移到x轴 零点为起点 即第一个y减去他本身变为0 后面每一个数也减去第一个y
                for (int i = 0; i < list.size(); i++) {
                    queue[i] = new Complex(list.get(i).y, 0).minus(mFristData);
                    queue[i] = queue[i].divides(new Complex(100, 0));
                                //Log.e("calculate", "queue[i]:" + queue[i].re());
                }


                //傅里叶转换 得到的是一个复数数组 此时进行的是复数的操作
                double fs2 = 5; //自己设置采样频率

                int NFFT2 = 128;//转化为2的基数倍 DFT需要的采样点数为2的幂指数
                double[] F_domain2 = new double[64];
                for (int i = 0; i < NFFT2 / 2; i++) {
                    F_domain2[i] = (fs2 / 2) * 1.0 / (NFFT2 / 2 - 1) * i;
                    }
                //--------------------------------------------------------------------------------------------------------
                            // FFT变换 将时域上的原始数据queue转化到频域上 Y2为复数 a+b*i
                Complex[] Y2 = FFT.fft(queue, 0, NFFT2);
                //原始幅值
                Complex[] Amp = new Complex[64];
                for (int i = 0; i < 64; i++) {
                    Y2[i] = Y2[i].divides(new Complex(list.size(), 0));
                    // 幅值
                    Amp[i] = Y2[i].times(new Complex(2, 0));
                    //  Log.e("calculate", "Amp2:" + Amp2[i].re() + " , " + Amp2[i].im());
                }
                // 判别变道的方向   频域上起始位置的实部减去第二个点的实部
                // 如果大于0是左变道 反之右变道
                double shibu1 = Amp[1].re();
                double shibu0 = Amp[0].re();
                double shibucha = shibu0 - shibu1;
                //--------------------------------------------------------------------------------------------------------
                            //原始幅值取绝对值
                double[] Amp2 = new double[64];
                for(int i = 0 ; i < 64; i++){
                    Amp2[i] = Amp[i].abs()/128;
                    }
                    // cha 用于区别变道和其他非变道行为
                // 当能量值超过阈值时，再计算cha，如果cha<0则为变道，反之不为变道(弯道 转弯)
                double  m = Amp2[0];
                double  n = (Amp2[2] + Amp2[3] + Amp2[4])/3;
                double  cha = m - n;
                //求能量
                Earray.add(Energy);
                double E2 = 0;
                List<Double> EnergyArray1=new ArrayList<>();
                int c=(int) Math.rint(5/(-0.0001542*speed*3.6*speed*3.6-0.1267*speed*3.6+38.12)/0.04);
                for (int j=c;j<c+3 ; j++){
                    //E2 = E2 + Amp2[j] * Amp2[j];
                    EnergyArray1.add(Amp2[j-1]);
                }

                E2= Collections.max(EnergyArray1);
                E2array.add(E2);
                Amp2array.add(Amp2[0]);
                double times=(Amp2[0]-E2)/E2;
                costArray.add(Amp2[0]-E2);
                costArray2.add(E2-Energy);
                timesArray.add(times);
//                            Log.e("Energy", "E2:" + E2);

                //--------------------------------------------------------------------------------------------------------
                // 方法1： 能量值第一次大于阈值时 即进行变道判断并播报（不会延迟2s播报）
//                                if (E2 >= Energy && EnergyArr.size() == 0) {
//                                    if(cha < 0.9*anInt){
//                                        if (shibucha > 0) {
//                                            leftOrRight = 0; //左变道
//                                        } else {
//                                            leftOrRight = 1;  //右变道
//                                        }
//                                    }
//
//                                    EnergyArr.add(E2);
//
//                                }
//
//                                //中间大于能量阈值的 不进行任何操作  直到小于能量阈值 清空EnergyArr 重新开始新的检测
//                                else if (E2 < Energy && EnergyArr.size() == 1) {
//                                    EnergyArr.clear();
//                                }
//
//
//                                else{
//
//                                }
                //--------------------------------------------------------------------------------------------------------


                //--------------------------------------------------------------------------------------------------------
                //  方法2：能量值第一次大于阈值时先不进行播报 判断第二次时再播报是否变道（延迟2s：每次截取10条数据，每1s钟传5条数据）
                if (Amp2[0] > 1.41){
                    SWturned = 1;     //进入弯道或转弯的标志位
                }

                if(E2>=Energy && EnergyArr.size() == 0 &&alarming==0){
                    EnergyArr.add(E2);
                    if ( times < timesarray[0]&&  SWturned == 0&&In==0){  //%一旦满足 就进入下一个elseif 即判为变道
                        predetect = 1;  //进入变道的标志位
                        double tempNum=timesArray.size()-1;
                        In=1;
                    }else if(times >= timesarray[0]){   //进入转弯或弯道的标志位，一旦进入就不会执行下一个elseif 即为非变道
                        SWturned = 1;
                        }
                }
                if(EnergyArr.size()==1&&predetect==1&&SWturned==0&&times<timesarray[0]&&tempBool==0){
                    tempBool=1;
                    swturned=1;
                }
                else if( timesArray.size() >=(tempNum+1)&& swturned==1&&In==1 ){
                    int tempA=tempNum;
                    while(tempA<timesArray.size()&&(tempA-tempNum)<3){
                        if(!boolArray[tempA+1-tempNum]){
                            if(costArray.get(tempA+1)<costArray.get(tempA)){
                                Sureturn=1;
                            }
                            if(tempA+1-tempNum==1&&timesArray.get(tempA+1)<-0.5){
                                Sureturn=1;
                            }
                        }
                        else if(boolArray[tempA+1-tempNum]){
                            if(timesArray.get(tempA+1)>timesarrayEvery[tempA+1-tempNum]){
                                swturn2=1;
                            }
                            if(costArray.get(tempA+1)<costArray.get(tempA)){
                                Sureturn=1;
                            }
                            if(tempA+1-tempNum==1&&timesArray.get(tempA+1)<-0.5){
                                Sureturn=1;
                            }
                        }
                        tempA=tempA+1;
                    }

                }

                if(Sureturn==1&&swturn2==0){
                    In=0;
                    swturned=0;
                    Sureturn=0;
                    alarming=1;
                    if(shibucha >= 0){
                        EnergyArr.add(E2);
                        leftOrRight = 0; //左变道
                    }else{
                        EnergyArr.add(E2);
                        leftOrRight = 1; //右变道
                    }
                }
                else{
                    predetect  = 0;
                    EnergyArr.clear();
                    }
                    if(E2 < Energy){
                    alarming=0;
                    SWturned = 0;
                }
                //            if (E2 >= Energy && EnergyArr.size() == 0) {
                //                EnergyArr.add(E2);
                //                shibuchaArr.add(shibucha);
                //            }

                //            //当第二个能量值大于阈值时
                //            //将第二个能量值插入能量数组
                //            else if (E2 >= Energy && EnergyArr.size() == 1) {
                //                if (shibuchaArr.get(0) > 0) {
                //                    leftOrRight = 0;
                //                    EnergyArr.add(E2);
                //                } else {
                //                    leftOrRight = 1;
                //                    EnergyArr.add(E2);
                //                }
                //            }
                //            //当能量值没有连续大于阈值时，即第一个大于阈值且第二个小于阈值时 做出变道判断  并清空能量数组
                //            else if (E2 < Energy && EnergyArr.size() == 1) {
                //                if (shibuchaArr.get(0) > 0) {
                //                    //                    disp('这是左变道')
                //                    leftOrRight = 0;
                //
                //                } else {
                //                    //                    disp('这是右变道')
                //                    leftOrRight = 1;
                //
                //                }
                //
                //                EnergyArr.clear();
                //                shibuchaArr.clear();
                //            }
                //            //当能量数组长度为2时，
                //            //再以后连续大于阈值的能量（第三个 第四个。。。）忽略不作处理 （很少出现）
                //            //直到能量值小于阈值时 清空能量数组
                //            else if (E2 < Energy  && EnergyArr.size() == 2) {
                //                EnergyArr.clear();
                //                shibuchaArr.clear();
                //            }
                //
                //
                //            else{
                //                //不做任何处理
                //            }

                //--------------------------------------------------------------------------------------------------------
             }
            else if(list != null && list.size() == 35&&speed>16) {  //高速的情况
                timeStart = CurentTimeString.getTime1();   //开始检测时间
                //Log.e("timeStart", "检测时间开始(毫秒):" + timeStart);

                Complex[] queue = new Complex[35];
                //Log.e("calculate", "mFristData:" + mFristData);
                //将原始数据平移到x轴 零点为起点 即第一个y减去他本身变为0 后面每一个数也减去第一个y
                for (int i = 0; i < list.size(); i++) {
                    queue[i] = new Complex(list.get(i).y, 0).minus(mFristData);
                    queue[i] = queue[i].divides(new Complex(100, 0));
                    //Log.e("calculate", "queue[i]:" + queue[i].re());
                }


                //傅里叶转换 得到的是一个复数数组 此时进行的是复数的操作
                double fs2 = 5; //自己设置采样频率

                int NFFT2 = 128;//转化为2的基数倍 DFT需要的采样点数为2的幂指数
                double[] F_domain2 = new double[64];
                for (int i = 0; i < NFFT2 / 2; i++) {
                    F_domain2[i] = (fs2 / 2) * 1.0 / (NFFT2 / 2 - 1) * i;
                }
                //--------------------------------------------------------------------------------------------------------
                // FFT变换 将时域上的原始数据queue转化到频域上 Y2为复数 a+b*i
                Complex[] Y2 = FFT.fft(queue, 0, NFFT2);
                //原始幅值
                Complex[] Amp = new Complex[64];
                for (int i = 0; i < 64; i++) {
                    Y2[i] = Y2[i].divides(new Complex(list.size(), 0));
                    // 幅值
                    Amp[i] = Y2[i].times(new Complex(2, 0));
                    //  Log.e("calculate", "Amp2:" + Amp2[i].re() + " , " + Amp2[i].im());
                }
                // 判别变道的方向   频域上起始位置的实部减去第二个点的实部
                // 如果大于0是左变道 反之右变道
                double shibu1 = Amp[1].re();
                double shibu0 = Amp[0].re();
                double shibucha = shibu0 - shibu1;
                //--------------------------------------------------------------------------------------------------------
                //原始幅值取绝对值
                double[] Amp2 = new double[64];
                for (int i = 0; i < 64; i++) {
                    Amp2[i] = Amp[i].abs() / 128;
                }
                // cha 用于区别变道和其他非变道行为
                // 当能量值超过阈值时，再计算cha，如果cha<0则为变道，反之不为变道(弯道 转弯)
                double m = Amp2[0];
                double n = (Amp2[2] + Amp2[3] + Amp2[4]) / 3;
                double cha = m - n;
                //求能量
                Earray.add(Energy);
                double E2 = 0;
                List<Double> EnergyArray1 = new ArrayList<>();
                int c = (int) Math.rint(5 / (-0.0001542 * speed * 3.6 * speed * 3.6 - 0.1267 * speed * 3.6 + 38.12) / 0.04);
                for (int j = c; j < c + 3; j++) {
                    //E2 = E2 + Amp2[j] * Amp2[j];
                    EnergyArray1.add(Amp2[j - 1]);
                }

                E2 = Collections.max(EnergyArray1);
                E2array.add(E2);
                Amp2array.add(Amp2[0]);
                double times = (Amp2[0] - E2) / E2;
                costArray.add(Amp2[0] - E2);
                costArray2.add(E2 - Energy);
                timesArray.add(times);
//                            Log.e("Energy", "E2:" + E2);

                //--------------------------------------------------------------------------------------------------------
                // 方法1： 能量值第一次大于阈值时 即进行变道判断并播报（不会延迟2s播报）
//                                if (E2 >= Energy && EnergyArr.size() == 0) {
//                                    if(cha < 0.9*anInt){
//                                        if (shibucha > 0) {
//                                            leftOrRight = 0; //左变道
//                                        } else {
//                                            leftOrRight = 1;  //右变道
//                                        }
//                                    }
//
//                                    EnergyArr.add(E2);
//
//                                }
//
//                                //中间大于能量阈值的 不进行任何操作  直到小于能量阈值 清空EnergyArr 重新开始新的检测
//                                else if (E2 < Energy && EnergyArr.size() == 1) {
//                                    EnergyArr.clear();
//                                }
//
//
//                                else{
//
//                                }
                //--------------------------------------------------------------------------------------------------------


                //--------------------------------------------------------------------------------------------------------
                //  方法2：能量值第一次大于阈值时先不进行播报 判断第二次时再播报是否变道（延迟2s：每次截取10条数据，每1s钟传5条数据）
                if (Amp2[0] > 1.41) {
                    SWturned = 1;     //进入弯道或转弯的标志位
                }

                if (E2 >= Energy && EnergyArr.size() == 0 && alarming == 0) {
                    EnergyArr.add(E2);
                    if (times < timesarray[0] && SWturned == 0 && In == 0) {  //%一旦满足 就进入下一个elseif 即判为变道
                        predetect = 1;  //进入变道的标志位
                        double tempNum = timesArray.size() - 1;
                        In = 1;
                    } else if (times >= timesarray[0]) {   //进入转弯或弯道的标志位，一旦进入就不会执行下一个elseif 即为非变道
                        SWturned = 1;
                    }
                }
                if (EnergyArr.size() == 1 && predetect == 1 && SWturned == 0 && times < timesarray[0] && tempBool == 0) {
                    tempBool = 1;
                    swturned = 1;
                } else if (timesArray.size() >= (tempNum + 1) && swturned == 1 && In == 1) {
                    int tempA = tempNum;
                    while (tempA < timesArray.size() && (tempA - tempNum) < 3) {
                        if (!boolArray[tempA + 1 - tempNum]) {
                            if (costArray.get(tempA + 1) < costArray.get(tempA)) {
                                Sureturn = 1;
                            }
                            if (tempA + 1 - tempNum == 1 && timesArray.get(tempA + 1) < -0.5) {
                                Sureturn = 1;
                            }
                        } else if (boolArray[tempA + 1 - tempNum]) {
                            if (timesArray.get(tempA + 1) > timesarrayEvery2[tempA + 1 - tempNum]) {
                                swturn2 = 1;
                            }
                            if (costArray.get(tempA + 1) < costArray.get(tempA)) {
                                Sureturn = 1;
                            }
                            if (tempA + 1 - tempNum == 1 && timesArray.get(tempA + 1) < -0.5) {
                                Sureturn = 1;
                            }
                        }
                        tempA = tempA + 1;
                    }

                }

                if (Sureturn == 1 && swturn2 == 0) {
                    In = 0;
                    swturned = 0;
                    Sureturn = 0;
                    alarming = 1;
                    if (shibucha >= 0) {
                        EnergyArr.add(E2);
                        leftOrRight = 0; //左变道
                    } else {
                        EnergyArr.add(E2);
                        leftOrRight = 1; //右变道
                    }
                } else {
                    predetect = 0;
                    EnergyArr.clear();
                }
                if (E2 < Energy) {
                    alarming = 0;
                    SWturned = 0;
                }
            }
            else{


            }

        if (leftOrRight != -1) {
            Message msg = new Message();
            msg.what = Config.STATUES_RECOGNIZE_SUCCESS;
            String  result = (leftOrRight == 0 ? "左变道" : "右变道");
            timeEnd =  CurentTimeString.getTime1(); //算法结束检测时间
            msg.obj = result +" 车速:"+(speedTempVar*3.6)+"码"+"  检测时长:" +(timeEnd - timeStart)+"毫秒";
            handler.sendMessage(msg);
            try {
                ResultwriteToFile( CurentTimeString.getTime() +  msg.obj );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private volatile int speedTempVar;

    public void setSpeedTempVar(int temp) {
        this.speedTempVar = temp;
    }


    public boolean PAUSE_WRITE = true;

    private Complex mFristData;

    //将原始数据存入手机根目录中
    private void writeToFile(String a) throws IOException {

        // 不将数据存入手机
        if (PAUSE_WRITE){

            //恢复区别变道和非变道的标志位 方便下一次检测
            SWturned = 0;
            predetect = 0;

            //清除上一次留下的加速度数据
            if(mCurrentMyData != null){
                if(mCurrentMyData.size() > 0){
                    mCurrentMyData.clear();
                }
            }

            //清除上一次的能量数组
            if(EnergyArr != null){

                if(EnergyArr.size() > 0){
//                    Log.e("calculate", "EnergyArr数组的长度:" + EnergyArr + EnergyArr.size());
                    EnergyArr.clear();
//                    Log.e("calculate", "EnergyArr数组1的长度:" + EnergyArr + EnergyArr.size());

                }

            }

            //清除上一次的实部差数组
//            if(shibuchaArr != null){
//                if(shibuchaArr.size() > 0){
//                    shibuchaArr.clear();
//                }
//
//            }

            //将上一次的起始点数据置空
            if(mFristData != null){
                mFristData = null;
                Log.e("mFristData", "mFristData置为空");
            }
            return;
        }


        if (mCurrentMyData == null){
            mCurrentMyData = new ArrayList<>();
        }

        if(EnergyArr == null){
            EnergyArr = new ArrayList<>();
        }

        if(shibuchaArr == null){
            shibuchaArr = new ArrayList<>();
        }


//        Log.e("calculate", "EnergyArr数组的长度:" + EnergyArr + EnergyArr.size());

        String[] dataLine = a.split(" ");

        MyData data = new MyData(Float.parseFloat(dataLine[2]) //每次读进来的 x y z轴加速度数据放进一个集合中
                , Float.parseFloat(dataLine[3])
                , Float.parseFloat(dataLine[4]));


        if (mFristData == null){
            mFristData = new Complex(data.y, 0);
            Log.e("mFristData", "mFristData不为空");
        }


        mCurrentMyData.add(data); //当数据集合里未装满40条数据 就继续装
//        Log.e("mCurrentMyData的长度", "mCurrentMyData:" + mCurrentMyData.size());


        //如果数据长度达到30 再检测速度 如果速度达到60码以上 再进行识别
        if(speedTempVar > 16 && mCurrentMyData.size() == 30) {
            handleRecognize(speedTempVar, mCurrentMyData);
            mCurrentMyData = mCurrentMyData.subList(10, mCurrentMyData.size());  //每次替换掉窗口内前10条数据
        }

        //如果数据长度达到40 再检测速度是否在60码以下
        else if(speedTempVar <= 16 && mCurrentMyData.size() == 35) { //当数据达到40条时 开始做测试
//            Log.e("speed", "车速:" + speedTempVar);
//            handleRecognize(mCurrentMyData);
            handleRecognize(speedTempVar, mCurrentMyData);
            mCurrentMyData = mCurrentMyData.subList(10, mCurrentMyData.size());
        }
        else{

        }
        Log.i("test","文件名Client"+ anInt);
        filename = new File(Environment.getExternalStorageDirectory(), "/sensor/" +number + ".txt");
        filename1 = new File(Environment.getExternalStorageDirectory(), "/sensor/陀螺仪" +number + ".txt");
        filename2 = new File(Environment.getExternalStorageDirectory(), "/sensor/线性加速度" +number + ".txt");
        //// TODO: 2015/12/23
        //创建三个文件 储存三个传感器的数据
        // OutputStream out=null;
        filename.getParentFile().mkdirs();
        if (!filename.exists()) {
            filename.createNewFile();
        }
        //        if (!filename1.exists())
        //            filename1.createNewFile();
        //        if (!filename2.exists())
        //            filename2.createNewFile();


        //这里的str就是服务端发送来的StringBuffer s ； 不进行数据判断了 全都是加速度传感器数据

        //    String[] list = a.split(" ");


        // 不进行数据判断了 全都是加速度传感器数据

        //        if (list[0].trim().equals("sensor1")) {
        //
        //            method3(filename, a);
        //        } else if (list[0].trim().equals("sensor2")) {
        //
        //            method3(filename1, a);
        //        } else if (list[0].trim().equals("sensor3"))
        //        {
        //
        //            method3(filename2, a);
        //        }
            method3(filename, a);


    }

    //存放识别结果的文件
    File filenameResult;

    //将识别结果存入result文件里
    private void ResultwriteToFile(String a) throws IOException {



        filenameResult = new File(Environment.getExternalStorageDirectory(), "/result/"  + "result.txt");

        //// TODO: 2015/12/23
        //创建三个文件 储存三个传感器的数据
        // OutputStream out=null;
        filenameResult.getParentFile().mkdirs();
        if (!filenameResult.exists()) {
            filenameResult.createNewFile();
        }


        methodresult(filenameResult, a);

//        FileOutputStream outStream = new FileOutputStream(filenameResult);
//        outStream.write(a.getBytes());
//        outStream.close();


    }

    /**
     * 追加文件：使用RandomAccessFile
     *
     * @param fileName 文件名
     * @param content  追加的内容
     */

    public static void methodresult(File fileName, String content) {
        //    if(!isWrite) return ;
        try {
            // 打开一个随机访问文件流，按读写方式
            RandomAccessFile randomFile = new RandomAccessFile(fileName, "rw");
            // 文件长度，字节数
            long fileLength = randomFile.length();
            // 将写文件指针移到文件尾。
            randomFile.seek(fileLength);
//            randomFile.writeBytes(content + "\r\anInt");
//            randomFile.writeChars(content + "\r\anInt");
            randomFile.writeUTF(content + "\r\n");
            randomFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


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


    public void cancel() {
        isStopReading = true;
    }
}
