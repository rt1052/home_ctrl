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
import org.json.JSONObject;
import org.json.JSONArray;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    //public Button btnSwitch[4];
    //public int state[4] = {2};
    public  Button btnSwitch[] = new Button[12];
    /* 设备的开关状态，有unknown off on 三种 */
    public String[] dev_state = {"unknown", "unknown", "unknown", "unknown", "unknown", "unknown"};

    /* tcp连接状态信息 */
    public TextView textState;
    /* 传感器信息 */
    public TextView textTemp;

    public Socket socket, socketLocal, socketRemote;

    /* tcp输入输出 */
    public OutputStream out;
    public InputStream in;

    public boolean connectFlag = false;

    public int port = 54200;

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

        /* 允许主线程操作 */
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

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

        /* tcp接收线程 */
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

        btnSwitch[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectFlag) {
                    button_click_event(1);
                }
            }
        });

        btnSwitch[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectFlag) {
                    button_click_event(2);
                }
            }
        });

        btnSwitch[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectFlag) {
                    button_click_event(3);
                }
            }
        });

        btnSwitch[4].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectFlag) {
                    button_click_event(4);
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

    /* 获取所有设备信息 */
    public void get_device_list() {
        try {
            JSONObject json_data = new JSONObject();
            json_data.put("cmd", "GET ALL STATE");
            out.write(json_data.toString().getBytes());
        } catch (Exception e) {

        }
    }

    /* 通过路由器内网转发 */
    public void tcp_connect_route() {
        while (true) {
            try {
                String ip = "192.168.1.2";
                socketLocal = new Socket(ip, port);
                if (socketLocal.isConnected()) {
                    socket = socketLocal;
                    out = socket.getOutputStream();
                    in = socket.getInputStream();
                    connectFlag = true;
                    textState.setText("已连接");
                    get_device_list();
                    break;
                } else {
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* 外网 */
    public void tcp_connect_remote() {
        while(true) {
            try {
                InetAddress addr = java.net.InetAddress.getByName("valderfields.tpddns.cn");
                String ip = addr.getHostAddress();
                socketRemote = new Socket(ip, port);
                if (socketRemote.isConnected()) {
                    socket = socketRemote;
                    out = socket.getOutputStream();
                    in = socket.getInputStream();
                    connectFlag = true;
                    textState.setText("已连接");
                    get_device_list();
                    break;
                } else {
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* 点击按键发送json数据 */
    public void button_click_event(int id) {
        String cmd = "GET STATE REQUEST";
        String state = dev_state[id];

        btnSwitch[id].setBackgroundColor(Color.parseColor("#8C8C8C"));
        switch (state) {
            case "on":
                state = "off";
                cmd = "SET STATE REQUEST";
                break;
            case "off":
                state = "on";
                cmd = "SET STATE REQUEST";
                break;
            case "unknown":
                state = "unknown";
                cmd = "GET STATE REQUEST";
                break;

        }

        try {
            JSONObject json_data = new JSONObject();
            json_data.put("cmd", cmd);
            json_data.put("state", state);
            json_data.put("id", id);
            out.write(json_data.toString().getBytes());
        } catch (Exception e) {

        }
    }

    public void set_button_state(final int id, String state) {
        dev_state[id] = state;
        /* 字符串比较必须用equals，用 == 可能会出错 */
        switch(state) {
            case "on":
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnSwitch[id].setText("开");
                        btnSwitch[id].setBackgroundColor(Color.parseColor("#FFD700"));
                    }
                });
                break;
            case "off":
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnSwitch[id].setText("关");
                        btnSwitch[id].setBackgroundColor(Color.parseColor("#ADD8E6"));
                    }
                });
                break;
        }
    }

    public void tcp_recv() {
        while (true) {
            try {
                byte[] buf = new byte[1024];
                int len = in.read(buf);

                if (len > 0) {
                    String str = new String(buf);
                    Log.d(TAG, "fangying "+str);
                    JSONObject jsonObject = new JSONObject(str);
                    String type = jsonObject.getString("type");

                    /* 根据不同的json类型进行不同处理 */
                    if (type.equals("dev info")) {
                        JSONObject dev =jsonObject.getJSONObject("dev");
                        final int id = dev.getInt("id");
                        set_button_state(id, dev.getString("state"));
                    } else if (type.equals("dev list")) {
                        JSONArray arr = jsonObject.getJSONArray("dev");
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject dev = arr.getJSONObject(i);
                            /* final类型可以在runOnUiThread中继续使用 */
                            final int id = dev.getInt("id");
                            Log.d(TAG, "fangying " + dev.getString("state"));
                            set_button_state(id, dev.getString("state"));
                        }
                    }
                } else {
                    textState.setText("未连接");
                    break;
                }
            } catch (Exception e) {
                //Log.i(TAG, "fangying yichang");
                Log.d(TAG, "fangying yichang");
                //Log.e(TAG, "fangying yichang", e);
                textState.setText("异常");
                break;
            }
        }

        /* 连接断开后把按键改成灰色 */
        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                btnSwitch[1].setBackgroundColor(Color.parseColor("#8C8C8C"));
                dev_state[1] = "unknown";
                btnSwitch[2].setBackgroundColor(Color.parseColor("#8C8C8C"));
                dev_state[2] = "unknown";
                btnSwitch[3].setBackgroundColor(Color.parseColor("#8C8C8C"));
                dev_state[3] = "unknown";
                btnSwitch[4].setBackgroundColor(Color.parseColor("#8C8C8C"));
                dev_state[4] = "unknown";
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
