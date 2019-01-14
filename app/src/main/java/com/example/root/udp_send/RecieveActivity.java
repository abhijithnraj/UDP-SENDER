package com.example.root.udp_send;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class RecieveActivity extends AppCompatActivity {

    EditText ip,port;
    Button recieveFile;
    TextView displayIP;
    int PORT=1234;

    public DatagramSocket getSocket(){
        DatagramSocket s = null;
        int server_port = PORT;
        byte[] message = new byte[1500];
        String size="";
        DatagramPacket p = new DatagramPacket(message, message.length);

        try {
            while(true) {
                System.out.println("inside try");
                s = new DatagramSocket(null);
                s.setReuseAddress(true);
                //                            s.setBroadcast(true);
                s.bind(new InetSocketAddress(PORT));
                System.out.println("Bound");
//                s.setSoTimeout(10000);
                s.receive(p);
                System.out.println("Recieved");
                final String text = new String(message, 0, p.getLength());
                if(text.equals("Android")){
                    System.out.println("Sending Acknowledgment");
                    byte[] sendMsg = "Acknowledge".getBytes();
                    DatagramPacket pSend = new DatagramPacket(sendMsg, sendMsg.length,p.getAddress(),p.getPort());
                    DatagramSocket sendSocket = new DatagramSocket();
                    for (int i = 0; i < 3000; i++){
                        sendSocket.send(pSend);
                    }
                    break;
                }
                if(text.startsWith("size:")){
                    size=text.split(":")[1];
                }
                Log.d("Udp tutorial", "message:" + text);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayIP.setText(text);
                    }
                });
            }

        } catch (SocketException e) {
            System.out.println("Socket error");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IO Error");
        }
        return s;

    }
    public void recieveData(DatagramSocket s){
        final byte[] message = new byte[1500];

        while(true) {
            try {
                DatagramPacket p = new DatagramPacket(message, message.length);
//                s.setSoTimeout(10000);
                s.receive(p);
                final String text = new String(message, 0, p.getLength());
                if(text.equals("Completed")) {
                    break;
                }
                else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            displayIP.setText(text);
                        }
                    });
                }

            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }
    public String getDeviceIP(){
        ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        WifiManager wm = (WifiManager)this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo connectionInfo = wm.getConnectionInfo();
        int ipAddress = connectionInfo.getIpAddress();
        String ipString = Formatter.formatIpAddress(ipAddress);
        return ipString;

    }
    Thread recieve=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recieve);
        recieveFile=(Button)findViewById(R.id.btnRecieveFile);
        displayIP = (TextView)findViewById(R.id.ipDIsplay);
        String displayText="Enter the following at the Sender Side\n";
        displayText+="ip: "+getDeviceIP()+"\n"+"port: "+PORT;
        displayIP.setText(displayText);


        recieveFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recieve= new Thread(){
                    public void run(){
                        DatagramSocket s = null;
                        s=getSocket();
                        System.out.println("Socket Recieved");
                        recieveData(s);
                        System.out.println("Data Recieved");


                    }
                };

                recieve.start();



            }
        });

    }
}
