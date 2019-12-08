package com.tpddns.valderfields.home_ctrl;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
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

import org.json.JSONObject;
import org.json.JSONArray;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    public CardView card[] = new CardView[12];
    public TextView text2[] = new TextView[12];
    public TextView text3[] = new TextView[12];
    public TextView text4[] = new TextView[12];
    public ImageView image[] = new ImageView[12];

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

        card[1] = (CardView) findViewById(R.id.card1);
        card[2] = (CardView) findViewById(R.id.card2);
        card[3] = (CardView) findViewById(R.id.card3);
        card[4] = (CardView) findViewById(R.id.card4);
        /* 开关状态 */
        text2[1] = (TextView) findViewById(R.id.card1_text2);
        text2[2] = (TextView) findViewById(R.id.card2_text2);
        text2[3] = (TextView) findViewById(R.id.card3_text2);
        text2[4] = (TextView) findViewById(R.id.card4_text2);
        /* 传感器信息 */
        text3[1] = (TextView) findViewById(R.id.card1_text3);
        text3[2] = (TextView) findViewById(R.id.card2_text3);
        text3[3] = (TextView) findViewById(R.id.card3_text3);
        text3[4] = (TextView) findViewById(R.id.card4_text3);
        /* 查看详情 */
        text4[1] = (TextView) findViewById(R.id.card1_text4);
        text4[2] = (TextView) findViewById(R.id.card2_text4);
        text4[3] = (TextView) findViewById(R.id.card3_text4);
        text4[4] = (TextView) findViewById(R.id.card4_text4);

        image[1] = (ImageView) findViewById(R.id.card1_image);
        image[2] = (ImageView) findViewById(R.id.card2_image);
        image[3] = (ImageView) findViewById(R.id.card3_image);
        image[4] = (ImageView) findViewById(R.id.card4_image);

        textState = (TextView) findViewById(R.id.text_state);

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

        card[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectFlag) {
                    card_click_event(1);
                }
            }
        });

        card[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectFlag) {
                    card_click_event(2);
                }
            }
        });

        card[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectFlag) {
                    card_click_event(3);
                }
            }
        });

        card[4].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectFlag) {
                    card_click_event(4);
                }
            }
        });

        text4[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                startActivity(intent);
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
    public void card_click_event(int id) {
        String cmd = "GET STATE REQUEST";
        String state = dev_state[id];

        switch (state) {
            case "on":
                state = "off";
                cmd = "SET STATE REQUEST";
                break;
            case "off":
                state = "on";
                cmd = "SET STATE REQUEST";
                break;
            case "update":
                state = "unknown";
                cmd = "GET STATE REQUEST";
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

        /* 每次发送后把状态设为unkonwn */
        image[id].setImageDrawable(getResources().getDrawable(R.drawable.unknown));
        dev_state[id] = "unknown";
    }

    public void set_card_state(final int id, String state, final int humidity, double temperature) {
        dev_state[id] = state;
        if (humidity != 0) {
            text3[id].setText(String.valueOf(temperature) + "℃" + "    " + String.valueOf(humidity) + "%RH");
        }

        /* 字符串比较必须用equals，用 == 可能会出错 */
        switch(state) {
            case "on":
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text2[id].setText("开");
                        image[id].setImageDrawable(getResources().getDrawable(R.drawable.on));
                    }
                });
                break;
            case "off":
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text2[id].setText("关");
                        image[id].setImageDrawable(getResources().getDrawable(R.drawable.off));
                    }
                });
                break;
            case "update":
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text2[id].setText("更新中");
                        image[id].setImageDrawable(getResources().getDrawable(R.drawable.update));
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
                        /* final类型可以在runOnUiThread中继续使用 */
                        final int id = dev.getInt("id");
                        String state = dev.getString("state");
                        final int humidity = dev.getInt("humidity");
                        double temperature = dev.getDouble("temperature");
                        set_card_state(id, state, humidity, temperature);
                    } else if (type.equals("dev list")) {
                        JSONArray arr = jsonObject.getJSONArray("dev");
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject dev = arr.getJSONObject(i);
                            /* final类型可以在runOnUiThread中继续使用 */
                            final int id = dev.getInt("id");
                            String state = dev.getString("state");
                            final int humidity = dev.getInt("humidity");
                            double temperature = dev.getDouble("temperature");
                            set_card_state(id, state, humidity, temperature);
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
                image[1].setImageDrawable(getResources().getDrawable(R.drawable.unknown));
                dev_state[1] = "unknown";
                image[2].setImageDrawable(getResources().getDrawable(R.drawable.unknown));
                dev_state[2] = "unknown";
                image[3].setImageDrawable(getResources().getDrawable(R.drawable.unknown));
                dev_state[3] = "unknown";
                image[4].setImageDrawable(getResources().getDrawable(R.drawable.unknown));
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
