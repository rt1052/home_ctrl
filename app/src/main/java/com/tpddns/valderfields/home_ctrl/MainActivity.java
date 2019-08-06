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

    //public Button btnSwitch[4];
    //public int state[4] = {2};
    public  Button btnSwitch[] = new Button[12];
    public byte state[] = new byte[]{(byte)0x2, (byte)0x2, (byte)0x2, (byte)0x2, (byte)0x2};

    public TextView textState;
    public TextView textTemp;

    public Socket socket, socketLocal, socketRemote;

    public OutputStream out;
    public InputStream in;
    public boolean connectFlag = false;
    public boolean getDataFlag = false;

    public int connextCnt = 0;
    public String ip; // = "192.168.0.43";
    public int port = 54300;

    byte[] getTemp1 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x01, (byte)0x05, (byte)0x00, (byte)0x00};
    byte[] getBuf1 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x01, (byte)0x03, (byte)0x00, (byte)0x00};
    byte[] openBuf1 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x00};
    byte[] closeBuf1 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x01, (byte)0x01, (byte)0x00, (byte)0x00};

    byte[] getBuf2 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x02, (byte)0x03, (byte)0x00, (byte)0x00};
    byte[] openBuf2 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x02, (byte)0x01, (byte)0x01, (byte)0x00};
    byte[] closeBuf2 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x02, (byte)0x01, (byte)0x00, (byte)0x00};

    byte[] getBuf3 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x03, (byte)0x03, (byte)0x00, (byte)0x00};
    byte[] openBuf3 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x03, (byte)0x01, (byte)0x01, (byte)0x00};
    byte[] closeBuf3 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x03, (byte)0x01, (byte)0x00, (byte)0x00};

    byte[] getBuf4 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x04, (byte)0x03, (byte)0x00, (byte)0x00};
    byte[] openBuf4 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x04, (byte)0x01, (byte)0x01, (byte)0x00};
    byte[] closeBuf4 =new byte[]{(byte)0x42, (byte)0x05, (byte)0x04, (byte)0x01, (byte)0x00, (byte)0x00};

    public float temp;
    public int humi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSwitch[1] = (Button) findViewById(R.id.btn_switch1);
        btnSwitch[2] = (Button) findViewById(R.id.btn_switch2);
        btnSwitch[3] = (Button) findViewById(R.id.btn_switch3);
        btnSwitch[4] = (Button) findViewById(R.id.btn_switch4);

        textState = (TextView) findViewById(R.id.text_state);
        textTemp = (TextView) findViewById(R.id.text_dht11);

        //textState.setBackgroundColor(Color.RED);

        //允许主线程操作
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        /*  本地连接 */
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    if (!connectFlag) {
                        tcp_connect_local();
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {

                        }
                    }
                }
            }
        }).start();

        /*  上级路由连接 */
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    if (!connectFlag) {
                        tcp_connect_route();
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {

                        }
                    }
                }
            }
        }).start();

        /*  外网连接 */
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    if (!connectFlag) {
                        tcp_connect_remote();
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {

                        }
                    }
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    if (connectFlag) {
                        tcp_recv();
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {

                        }
                    }
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {

                while(true) {
                    try {
                        if (getDataFlag) {
                            getDataFlag = false;

                            Thread.sleep(100);
                            out.write(getTemp1);
                            Thread.sleep(600);
                            out.write(getBuf1);
                            Thread.sleep(400);
                            out.write(getBuf2);
                            Thread.sleep(400);
                            out.write(getBuf3);
                            Thread.sleep(400);
                            out.write(getBuf4);
                            Thread.sleep(400);
                        } else {
                            Thread.sleep(100);
                        }
                    } catch (Exception e) {

                    }
                }
            }
        }).start();

        btnSwitch[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectFlag) {
                    try{
                        switch (state[1]) {
                        case 0x0:
                            out.write(openBuf1);
                            break;
                        case 0x1:
                            out.write(closeBuf1);
                            break;
                        case 0x2:
                            out.write(getBuf1);
                            break;
                        }
                        state[1] = 0x2;
                        btnSwitch[1].setBackgroundColor(Color.parseColor("#8C8C8C"));
                    } catch (Exception e) {

                    }
                }
            }
        });

        btnSwitch[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectFlag) {
                    try{
                        switch (state[2]) {
                        case 0x0:
                            out.write(openBuf2);
                            break;
                        case 0x1:
                            out.write(closeBuf2);
                            break;
                        case 0x2:
                            out.write(getBuf2);
                            break;
                        }
                        state[2] = 0x2;
                        btnSwitch[2].setBackgroundColor(Color.parseColor("#8C8C8C"));
                    } catch (Exception e) {

                    }
                }
            }
        });

        btnSwitch[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectFlag) {
                    try{
                        switch (state[3]) {
                            case 0x0:
                                out.write(openBuf3);
                                break;
                            case 0x1:
                                out.write(closeBuf3);
                                break;
                            case 0x2:
                                out.write(getBuf3);
                                break;
                        }
                        state[3] = 0x2;
                        btnSwitch[3].setBackgroundColor(Color.parseColor("#8C8C8C"));
                    } catch (Exception e) {

                    }
                }
            }
        });

        btnSwitch[4].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectFlag) {
                    try{
                        switch (state[4]) {
                            case 0x0:
                                out.write(openBuf4);
                                break;
                            case 0x1:
                                out.write(closeBuf4);
                                break;
                            case 0x2:
                                out.write(getBuf4);
                                break;
                        }
                        state[4] = 0x2;
                        btnSwitch[4].setBackgroundColor(Color.parseColor("#8C8C8C"));
                    } catch (Exception e) {

                    }
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

    public void tcp_connect() {
        while(true) {
            String localIP = getLocalIpAddress();
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

            try {
                socket = new Socket(ip, port);
                if (socket.isConnected()) {
                    out = socket.getOutputStream();
                    in = socket.getInputStream();
                    connectFlag = true;
                    getDataFlag = true;
                    textState.setText("已连接");
                    break;
                } else {
                    Thread.sleep(200);
                }

            } catch (Exception e) {

            }
        }
    }

    public void tcp_connect_local() {
        while (true) {
            try {
                ip = "192.168.0.43";
                socketLocal = new Socket(ip, port);
                if (socketLocal.isConnected()) {
                    socket = socketLocal;
                    out = socket.getOutputStream();
                    in = socket.getInputStream();
                    connectFlag = true;
                    getDataFlag = true;
                    textState.setText("已连接");
                    break;
                } else {
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void tcp_connect_route() {
        while (true) {
            try {
                ip = "192.168.1.2";
                socketLocal = new Socket(ip, port);
                if (socketLocal.isConnected()) {
                    socket = socketLocal;
                    out = socket.getOutputStream();
                    in = socket.getInputStream();
                    connectFlag = true;
                    getDataFlag = true;
                    textState.setText("已连接");
                    break;
                } else {
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void tcp_connect_remote() {
        while(true) {
            try {
                InetAddress addr = java.net.InetAddress.getByName("valderfields.tpddns.cn");
                ip = addr.getHostAddress();
                socketRemote = new Socket(ip, port);
                if (socketRemote.isConnected()) {
                    socket = socketRemote;
                    out = socket.getOutputStream();
                    in = socket.getInputStream();
                    connectFlag = true;
                    getDataFlag = true;
                    textState.setText("已连接");
                    break;
                } else {
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void tcp_recv() {
        while (true) {
            try {
                byte[] buf = new byte[128];
                int len = in.read(buf);
                //int id;
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
                        final int id = buf[3];
                        switch (buf[5]) {
                            case 0x0:
                                    runOnUiThread(new Runnable(){
                                        @Override
                                        public void run() {
                                            btnSwitch[id].setText("开");
                                            btnSwitch[id].setBackgroundColor(Color.parseColor("#FFD700"));
                                        }
                                    });
                                    state[id] = 0x0;
                                break;
                            case 0x1:
                                    runOnUiThread(new Runnable(){
                                        @Override
                                        public void run() {
                                            btnSwitch[id].setText("关");
                                            btnSwitch[id].setBackgroundColor(Color.parseColor("#ADD8E6"));
                                        }
                                    });
                                    state[id] = 0x1;     
                                break;
                        }
                    }
                } else {
                    textState.setText("未连接");
                    //connectFlag = false;
                    //socket.close();
                    break;
                }
            } catch (Exception e) {
                Log.i(TAG, "fangying yichang");
                Log.d(TAG, "fangying yichang");
                Log.e(TAG, "fangying yichang", e);
                textState.setText("异常");
                break;
            }
        }


        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                btnSwitch[1].setBackgroundColor(Color.parseColor("#8C8C8C"));
                state[1] = 0x2;
                btnSwitch[2].setBackgroundColor(Color.parseColor("#8C8C8C"));
                state[2] = 0x2;
                btnSwitch[3].setBackgroundColor(Color.parseColor("#8C8C8C"));
                state[3] = 0x2;
            }
        });

        try {
            socket.close();
            Thread.sleep(1000);
        } catch (Exception e) {

        }
        connectFlag = false;
    }
}
