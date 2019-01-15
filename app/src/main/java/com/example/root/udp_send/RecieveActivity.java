package com.example.root.udp_send;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class RecieveActivity extends AppCompatActivity {

    EditText ip,port;
    Button recieveFile;
    TextView displayIP;
    String receivedFileName="";
    long recievedBufferSize;
    long totalRecievedFileSize;

    int PORT=1234;
    public String getmd5(MessageDigest digest, byte[] b){
        char[] hextable = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        digest.update(b);
        byte messageDigest[] = digest.digest();
        String hash = "";
        for (int i = 0; i < messageDigest.length; ++i)
        {
            int di = (messageDigest[i] + 256) & 0xFF;
            hash = hash + hextable[(di >> 4) & 0xF] + hextable[di & 0xF];
        }
        return hash;

    }
    ArrayList<String> md5SumList = new ArrayList<String>();
    public DatagramSocket getSocket(){
        DatagramSocket s = null;
        int server_port = PORT;
        byte[] message = new byte[1500];
        String size="";
        DatagramPacket p = new DatagramPacket(message, message.length);

        try {
            while(true) {
                System.out.println("Reciever Datagram packer buffer created");
                s = new DatagramSocket(null);
                s.setReuseAddress(true);
                //                            s.setBroadcast(true);
                s.bind(new InetSocketAddress(PORT));
                System.out.println("Waiting to recieve message ");
//                s.setSoTimeout(10000);
                s.receive(p);
                final String text = new String(message, 0, p.getLength());
                System.out.println("Message recieved ="+text);
                if(text.startsWith("Android")){
                    System.out.println("Sender Communication keyword Android Recieved");
                    System.out.println("Sending Acknowledgment to Sender keyword=Acknowledgement");
                    receivedFileName=text.split("@")[1];
                    System.out.println("Asking for Confirmation at the sender Side");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            confirmation=askForConfirmation();
                        }
                    });
                    System.out.println("Waiting for Confirmation");
                    while(confirmationString=="");
                    System.out.println("Confirmation yes/no recieved from Reciever");
                    byte[] sendMsg;

                    if(confirmation==true) {
                        System.out.println("Confirmation Yes Recieved from the Reciever");
                         sendMsg= ("Acknowledge@"+confirmationString).getBytes();
                         totalRecievedFileSize=Long.parseLong(text.split("@")[2]);
                         recievedBufferSize = Long.parseLong(text.split("@")[3]);
                        final byte[] recievedMessageBytes = new byte[(int)recievedBufferSize];
                        String downloadsLocation= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                        OutputStream os= new FileOutputStream(new File(downloadsLocation+"/"+receivedFileName));
                        DatagramPacket pSend = new DatagramPacket(sendMsg, sendMsg.length,p.getAddress(),p.getPort());
                        System.out.println("Sending to "+p.getAddress()+":"+p.getPort());
                        DatagramSocket sendSocket = new DatagramSocket();
                        for (int i = 0; i < 2; i++){
                            System.out.println("Sending True to Sender");
                            sendSocket.send(pSend);
                        }


                        long recievedBytes=0;
                       MessageDigest digest = MessageDigest.getInstance("MD5");

                        while(true) {
                            try {
                                DatagramPacket pRecived = new DatagramPacket(recievedMessageBytes, recievedMessageBytes.length);
                                System.out.println("Waiting for Sender to send data stream");
                                s.setSoTimeout(10000);
                                s.receive(pRecived);
                                recievedBytes+=recievedBufferSize;
                                System.out.println("DataStream Recieved");
                                os.write(recievedMessageBytes);
                                md5SumList.add(getmd5(digest,recievedMessageBytes));
                                final String t = new String(recievedMessageBytes, 0, pRecived.getLength());
                                if(recievedBytes>=totalRecievedFileSize){
                                    s.disconnect();
                                    s.close();
                                    break;
                                }
//                                else{
//                                    runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            displayIP.setText(t);
//                                        }
//                                    });
//                                }

                            }
                            catch(SocketTimeoutException e){
                                e.printStackTrace();
                                System.out.println("About to break the inner loop");
                                break;
                            }
                            catch (SocketException e) {
                                e.printStackTrace();
                                System.out.println("About to break the inner loop");
                                break;
                            }

                            catch (IOException e) {
                                e.printStackTrace();
                            }
                        }


                    }
                    else{
                        System.out.println("The Reciever Rejected the transfer");
                        sendMsg = "Acknowledge".getBytes();
                        sendMsg= ("Acknowledge@"+confirmationString).getBytes();
                        DatagramPacket pSend = new DatagramPacket(sendMsg, sendMsg.length,p.getAddress(),p.getPort());
                        System.out.println("Sending to "+p.getAddress()+":"+p.getPort());
                        DatagramSocket sendSocket = new DatagramSocket();
                        for (int i = 0; i < 3000; i++){
                            sendSocket.send(pSend);
                        }
                    }


                    System.out.println("The data recieved at the reciever is "+text);
                    System.out.println("Handshaking Send and Recieve loop completed at sender side");
                    break;
                }
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        displayIP.setText(text);
//                    }
//                });
                break;
            }

        } catch (SocketException e) {
            System.out.println("Socket error");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IO Error");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        System.out.println("The md5 Sums are :");
        for(String h:md5SumList){
            System.out.println(h);
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
                    s.close();
                    break;
                }
//                else{
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            displayIP.setText(text);
//                        }
//                    });
//                }

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
    boolean confirmation=false;
    String confirmationString="";
    public boolean askForConfirmation(){
        confirmationString="";
        confirmation=false;
        AlertDialog.Builder builder1 = new AlertDialog.Builder(RecieveActivity.this);
        builder1.setMessage("Do you want to recieve the file : "+receivedFileName);
        builder1.setCancelable(true);
        builder1.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        confirmation=true;
                        confirmationString="true";
                    }
                });
        builder1.setNegativeButton(
                "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        confirmation=false;
                        confirmationString="false";
                    }
                });
        AlertDialog alert11 = builder1.create();
        alert11.show();
        return confirmation;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recieve);
        recieveFile=(Button)findViewById(R.id.btnRecieveFile);
        displayIP = (TextView)findViewById(R.id.ipDIsplay);
        String displayText="Enter the following at the Sender Side\n";
        displayText+="ip: "+getDeviceIP()+"\n"+"port: "+PORT;
        displayIP.setText(displayText);

        recieve= new Thread(){
            public void run(){
                DatagramSocket s = null;
                System.out.println("Trying to get Socket");
                s=getSocket();
                System.out.println("Socket Object Recieved");
                System.out.println("Data Transfer Completed");


            }
        };
        recieve.start();




    }
}
