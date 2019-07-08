package com.tpddns.valderfields.home_ctrl;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import android.os.StrictMode;
import android.graphics.Color;
import java.net.InetAddress;  /* dns */
import java.net.NetworkInterface;  /* local ip */
import java.util.Enumeration;
import android.util.Log;
import android.os.Handler;
import android.content.Context;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    public Button btnSwitch1, btnSwitch2;
    public TextView textState;
    public TextView textTemp;

    public Socket socket;

    public OutputStream out;
    public InputStream in;
    public boolean connectFlag = false;

    public String ip; // = "192.168.0.43";
    public int port = 54300;

    public boolean state1 = false;
    public boolean state2 = false;

    byte[] getTemp1 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x01, (byte)0x05, (byte)0x00, (byte)0x00};
    byte[] getBuf1 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x01, (byte)0x03, (byte)0x00, (byte)0x00};
    byte[] openBuf1 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x00};
    byte[] closeBuf1 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x01, (byte)0x01, (byte)0x00, (byte)0x00};

    byte[] getBuf2 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x02, (byte)0x03, (byte)0x00, (byte)0x00};
    byte[] openBuf2 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x02, (byte)0x01, (byte)0x01, (byte)0x00};
    byte[] closeBuf2 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x02, (byte)0x01, (byte)0x00, (byte)0x00};

    public float temp;
    public int humi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSwitch1 = (Button) findViewById(R.id.btn_switch1);
        btnSwitch2 = (Button) findViewById(R.id.btn_switch2);
        textState = (TextView) findViewById(R.id.text_state);
        textTemp = (TextView) findViewById(R.id.text_dht11);

        //textState.setBackgroundColor(Color.RED);

        //允许主线程操作
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                String localIP =  getLocalIpAddress();
                //textState.setText(localIP);

                if (localIP.contains("192.168.0.") == true) {
                    ip = "192.168.0.43";
                } else {
                    try {
                        InetAddress addr = java.net.InetAddress.getByName("valderfields.tpddns.cn");
                        ip = addr.getHostAddress();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                try{
                    socket = new Socket(ip, port);
                    out = socket.getOutputStream();
                    in = socket.getInputStream();
                    connectFlag = true;

                    textState.setText("已连接");
                    //textState.setBackgroundColor(Color.GREEN);

                    tcp_recv();
                    //Toast.makeText (MainActivity.this, "已连接",Toast. LENGTH_SHORT ).show();
                } catch (Exception e){
                    //Toast.makeText (MainActivity.this, "未连接",Toast. LENGTH_SHORT ).show();
                    // Log.i("miao","#########################################"+"Exception");
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    /* 等待连接 */
                    while (!connectFlag) {
                        Thread.sleep(100);
                    }

                    out.write(getTemp1);
                    Thread.sleep(500);
                    out.write(getBuf1);
                    Thread.sleep(500);
                    out.write(getBuf2);
                    Thread.sleep(500);

                } catch (Exception e){

                }
            }
        }).start();

        btnSwitch1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    if (state1) {
                        out.write(closeBuf1);
                        btnSwitch1.setBackgroundColor(Color.parseColor("#8C8C8C"));
                    } else {
                        out.write(openBuf1);
                        btnSwitch1.setBackgroundColor(Color.parseColor("#8C8C8C"));
                    }
                } catch (Exception e) {

                }
            }
        });

        btnSwitch2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    if (state2) {
                        out.write(closeBuf2);
                        btnSwitch2.setBackgroundColor(Color.parseColor("#8C8C8C"));
                    } else {
                        out.write(openBuf2);
                        btnSwitch2.setBackgroundColor(Color.parseColor("#8C8C8C"));
                    }
                } catch (Exception e) {

                }
            }
        });
    }

    /* 获取本地ip地址 */
    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        //return inetAddress.getHostAddress().toString();
                        if (inetAddress.isSiteLocalAddress()) {
                            return inetAddress.getHostAddress().toString();
                        }
                    }
                }
            }
        } catch (Exception ex) {

        }
        return null;
    }

    public void tcp_recv() {
        while (connectFlag) {
            //mBtnOpen.setText("test");
            try {
                byte[] buf = new byte[128];
                int len = in.read(buf);
                if (len > 0) {
                    if (buf[4] == 0x6) {
                        temp = (float) (buf[7]*10 + buf[8]) / 10;
                        humi = buf[5];
                        //textTemp.setText(String.valueOf(temp) + "℃" + "    " + String.valueOf(humi) + "%RH");

                        runOnUiThread(new Runnable(){
                            @Override
                            public void run() {
                                textTemp.setText(String.valueOf(temp) + "℃" + "    " + String.valueOf(humi) + "%RH");
                            }
                        });

                    } else {
                        switch (buf[5]) {
                            case 0x0:
                                if (buf[3] == 0x1) {
                                    //btnSwitch1.setText("开");
                                    //btnSwitch1.setBackgroundColor(Color.parseColor("#FFD700"));
                                    runOnUiThread(new Runnable(){
                                        @Override
                                        public void run() {
                                            btnSwitch1.setText("开");
                                            btnSwitch1.setBackgroundColor(Color.parseColor("#FFD700"));
                                        }
                                    });
                                    state1 = false;
                                } else if (buf[3] == 0x2) {
                                    //btnSwitch2.setText("开");
                                    //btnSwitch2.setBackgroundColor(Color.parseColor("#FFD700"));
                                    runOnUiThread(new Runnable(){
                                        @Override
                                        public void run() {
                                            btnSwitch2.setText("开");
                                            btnSwitch2.setBackgroundColor(Color.parseColor("#FFD700"));
                                        }
                                    });
                                    state2 = false;
                                }
                                break;
                            case 0x1:
                                if (buf[3] == 0x1) {
                                    //btnSwitch1.setText("关");
                                    //btnSwitch1.setBackgroundColor(Color.parseColor("#ADD8E6"));
                                    runOnUiThread(new Runnable(){
                                        @Override
                                        public void run() {
                                            btnSwitch1.setText("关");
                                            btnSwitch1.setBackgroundColor(Color.parseColor("#ADD8E6"));
                                        }
                                    });
                                    state1 = true;
                                } else if (buf[3] == 0x2) {
                                    //btnSwitch2.setText("关");
                                    //btnSwitch2.setBackgroundColor(Color.parseColor("#ADD8E6"));
                                    runOnUiThread(new Runnable(){
                                        @Override
                                        public void run() {
                                            btnSwitch2.setText("关");
                                            btnSwitch2.setBackgroundColor(Color.parseColor("#ADD8E6"));
                                        }
                                    });
                                    state2 = true;
                                }
                                break;
                            case 0x2:
                                //btnOpen.setText("断线");
                                //btnOpen.setBackgroundColor(Color.parseColor("#8C8C8C"));
                                break;
                        }
                    }
                } else {
                    btnSwitch1.setText("断网");
                }
            } catch (Exception e) {
                Log.i(TAG, "fangying yichang");
                Log.d(TAG, "fangying yichang");
                Log.e(TAG, "fangying yichang", e);
                btnSwitch1.setText("异常");
            }
        }
    }
}
