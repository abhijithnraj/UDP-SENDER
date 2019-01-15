package com.example.root.udp_send;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.MessageQueue;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.SyncStateContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;

/*
* Flow of Control of the Android App
* ChoiseActivity either moves into MainActivity(Send Button) or RecieveActivity(Recieve Button)
* MainActivity-> Send Button => has button = button picker implementation has readFileThread and getFileThread
* First getFileThread provides filename and a while loop is executed for waiting until file name is not null
* Then read ip and port from user and press submit button
* button_picker button performs following things  UDPSCAN,establishUDPConnection,readAndSendFile
* UDPSCAN()=> SCAN FOR ALL OPEN PORTS
* establishUDPConnection(ip,PORT) => handshake and return DataGramSocket
* readAndSendFile(fileName,s) => Read data and Send then using Socket
* submit button performs following things just adjust the view
*
* */
public class MainActivity extends AppCompatActivity {
    String ip;
    Button button;
    ProgressBar progressBar;
    long totalProgress=0;
    long currentProgress=0;
    TextView progressDisplay;
    String PORT="1234";
    EditText ipEdit,portEdit;
    Button submit;
    ListView ipListDisplay;
    int pingTimeout=1000;
    @Override
    public void onDestroy() {

        MainActivity.this.finish();
        super.onDestroy();
        if(getFileThread!=null) {
//            getFileThread.stop();
        }
        if(readFileThread!=null) {
//            readFileThread.stop();
        }

    }
    private void displayTerminal(String data){
        System.out.println(data);
    }
    private void showFileChooser() {
        displayTerminal("File Chooser internt started");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), 7);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            displayTerminal("Please install a File Manager");
        }
    }

    public String getmd5(MessageDigest digest,byte[] b){
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
    public String getDeviceIP(){
        ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        WifiManager wm = (WifiManager)this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo connectionInfo = wm.getConnectionInfo();
        int ipAddress = connectionInfo.getIpAddress();
        String ipString = Formatter.formatIpAddress(ipAddress);
        return ipString;

    }
    ArrayList<String> availableIPList= new ArrayList<String>();
    ArrayAdapter<String> ipItemsAdapter=null;
    public void UDPScan(int timeout){
        String ipString= null;
        ipString = getDeviceIP();
        if(ipString.equals("0.0.0.0")){
            ipString="192.168.43.1";
        }
        System.out.println("Sender IP address is "+ipString);
        String[] ipComponents=ipString.split("\\.");
        String ipConst=ipComponents[0]+"."+ipComponents[1]+"."+ipComponents[2]+".";

        try {
            DatagramSocket s = new DatagramSocket();
            int server_port = 1234;
            displayTerminal("About to scan all the ip address ranging from "+ipConst+"0 to "+ipConst+"255");
            ipItemsAdapter =new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,availableIPList);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ipListDisplay.setAdapter(ipItemsAdapter);
                }
            });

            for(int i=0;i<=255;i++){
                String actualIP=ipConst+i;
//                displayTerminal("Searching for ip :"+actualIP);
                if(InetAddress.getByName(actualIP).isReachable(pingTimeout) && !actualIP.equals(ipString)){
                    availableIPList.add(actualIP);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ipItemsAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, availableIPList);
                            ipItemsAdapter.notifyDataSetChanged();
                            ipListDisplay.setAdapter(ipItemsAdapter);

                        }
                    });

//                    System.out.println("ip address: "+actualIP+" available");
                }
            }
            for(String ipAdress:availableIPList){
                displayTerminal("Available ip address : "+ipAdress);
            }


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ipListDisplay.setAdapter(ipItemsAdapter);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ipListDisplay.setVisibility(ListView.VISIBLE);
                        }
                    });
                }
            });


        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    int counter=0;
    String confirmationString="";
    public DatagramSocket establishUDPConnection(String address,String port){
        DatagramSocket s =null;
        displayTerminal("UDP Connection Sender Side Handshake Started");

        while(true) {
            try {
                counter+=1;
                if(counter>10){
                    break;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDisplay.setText("Waiting for authentification from Reciever Side :"+counter);
                    }
                });
                displayTerminal("Waiting for Authentification from the Reciever Side");
                String message = "Android";
                String f[]=fileName.split("/");
                message+= "@"+f[f.length-1];
                File fi = new File(fileName);
                message+="@"+fi.length();
                int BUFFER_SIZE= (int) (totalProgress*0.1);
                if(BUFFER_SIZE> 1000000000){
                    BUFFER_SIZE=1000000000;
                }
                BUFFER_SIZE=60000;
                message+="@"+BUFFER_SIZE;
                s = new DatagramSocket(null);
                s.setReuseAddress(true);
                s.bind(new InetSocketAddress(Integer.parseInt(PORT)));
                InetAddress local = InetAddress.getByName(address);
                int msg_length = message.length();
                byte[] m = message.getBytes();
                DatagramPacket p = new DatagramPacket(m, msg_length, local, Integer.parseInt(port));
                s.send(p);
                displayTerminal("Sending keyword Android to the Receiver");
                final byte[] recMessage = new byte[1500];
                final DatagramPacket pRec = new DatagramPacket(recMessage, recMessage.length);
                displayTerminal("Waiting for the Sender to Send back Acknowledgement");
                s.setSoTimeout(10000);
                s.receive(pRec);
                displayTerminal("The Reciever has send some text and Sender has recieved it");

                final String text = new String(recMessage, 0, pRec.getLength());
                System.out.println("Text recieved is "+text);
                displayTerminal("Handshake Completed break executed");
                if (text.startsWith( "Acknowledge")) {
                    confirmationString=text.split("@")[1];
                    if(confirmationString.equals("true")){
                        displayTerminal("Started readAndSendFile Function");
                        readAndSendFile(fi, s);
                        displayTerminal("Completed readAndSendFile Function");

                    }
                    else{
                        progressDisplay.setText("The Reciever Rejected the Transfer");
                    }
                }
                break;
//
//                showToastInThread(this.getBaseContext(), "Message Send");

            } catch (SocketException e) {
                e.printStackTrace();
                continue;
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return s;

    }

    ArrayList<String>md5SumList=new ArrayList<String>();
    public void sendUDP(String hash,byte[] b){


    }
    int perc=0;
    public void readAndSendFile(File fi,DatagramSocket s) throws IOException {

        totalProgress=fi.length();
        FileInputStream in = null;
        currentProgress=0;

        int BUFFER_SIZE= (int) (totalProgress*0.1);
        if(BUFFER_SIZE> 1000000000){
            BUFFER_SIZE=1000000000;
        }
        BUFFER_SIZE=60000;   ;
        byte[] b = new byte[BUFFER_SIZE];
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        try {
            in = new FileInputStream(fileName);
            long availableBytes=totalProgress;
            System.out.println("Started");
            String hash;
            while((availableBytes  = in.read(b)) != -1){
                InetAddress local = InetAddress.getByName(ip);
                DatagramPacket pSend = new DatagramPacket(b, b.length, local,Integer.parseInt(PORT));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                s.send(pSend);

                currentProgress+=BUFFER_SIZE;
                perc= (int) ((currentProgress*100)/totalProgress);
                progressBar.setProgress(perc);
                runOnUiThread(new Runnable() {
                 @Override
                    public void run() {
                        if(perc>100) {
                            perc = 100;
                        }
                        progressDisplay.setText("Completed :" + perc + "%");

                    }
                });
                hash=getmd5(digest,b);
                md5SumList.add(hash);

            }
            displayTerminal("The File has been read completely and all the md5 sums calculated and displayed");
            Runtime.getRuntime().gc();
            String deviceIP=getDeviceIP();
            System.out.println("The md5 Sums are :");
            for(String h:md5SumList){
                System.out.println(h);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            displayTerminal("File not Found exception error has occured");

        }

    }

    public static String getPathFromURI(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
    String fileName="";
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        System.out.println("File Picker returned with result code "+resultCode);
        switch (requestCode) {
            case 7:
                if (resultCode == RESULT_OK && data!=null) {
                    System.out.println("Data has returned");
                    String PathHolder = getPathFromURI(getBaseContext(),data.getData());
                    fileName=PathHolder;
                    System.out.print("");
//                    Toast.makeText(MainActivity.this, PathHolder, Toast.LENGTH_LONG).show();
                }
                else{
                    System.out.println("Some Data error occured after file picking");
                }
                break;
        }

    }


    public void showToastInThread(final Context context,final String str){
        Looper.prepare();
        MessageQueue queue = Looper.myQueue();
        queue.addIdleHandler(new MessageQueue.IdleHandler() {
            int mReqCount = 0;

            @Override
            public boolean queueIdle() {
                if (++mReqCount == 4) {
                    Looper.myLooper().quit();
                    return false;
                } else
                    return true;
            }
        });
        Toast.makeText(context, str,Toast.LENGTH_LONG).show();
        Looper.loop();
    }

    Thread readFileThread=null;
    Thread getFileThread=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar=(ProgressBar)findViewById(R.id.pgbarFile);
        button = (Button) findViewById(R.id.btn_picker);
        progressDisplay = (TextView)findViewById(R.id.txtvwProgress);
        progressDisplay.setVisibility(progressDisplay.INVISIBLE);
        ipListDisplay = (ListView)findViewById(R.id.listvwIPs);
//        ipListDisplay.setVisibility(ListView.INVISIBLE);
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        progressDisplay.setVisibility(progressDisplay.INVISIBLE);

        ipListDisplay.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ip= availableIPList.get(i);
                progressDisplay.setVisibility(progressDisplay.VISIBLE);
                readFileThread.start();
//                ipListDisplay.setVisibility(ListView.INVISIBLE);
            }
        });

        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
//                button.setVisibility(button.INVISIBLE);
                displayTerminal("File Picker button clicked");
                button.setVisibility(button.INVISIBLE);
                  readFileThread = new Thread(){
                    public void run(){
                        //                            progressBar.setVisibility(progressBar.VISIBLE);
                        displayTerminal("Handshake Function establish udp connection called");
                        DatagramSocket s=null;
                        s=establishUDPConnection(ip,PORT);
                        displayTerminal("UDP Connection Established and Socket Recieved");
                        if(s==null){
                            displayTerminal("Socket Recieved from establishUDPConnection() is null");
                            displayTerminal("Sender didn't Acknowledge the Reciever Message");
                        }
                        {
                            displayTerminal("Socket object created successfully");

                        }

//                            Intent i = new Intent(MainActivity.this,MainActivity.class);
//                            startActivity(i);

                    }
                };
                final Thread udpScannerThread = new Thread(){
                    @Override
                    public void run() {
                        System.out.println("Started UDP Scan to identify all active ips -> could be erraneous");
                        UDPScan(20);
                    }
                };


                getFileThread = new Thread(){
                    public void run(){
                        fileName="";
                        displayTerminal("About to display File Chooser");
                        showFileChooser();
                        udpScannerThread.start();
                        displayTerminal("FileName not null loop started");
                        while(fileName.equals("")){
                            displayTerminal("Waiting for File Name to be not null");
                        };
                        displayTerminal("File Choiser Wait loop completed");
                        displayTerminal("File choser completed");
                        displayTerminal("File chosen: "+fileName);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ipListDisplay.setVisibility(ListView.VISIBLE);
                            }
                        });

                    }

                };

                getFileThread.start();
                displayTerminal("File Picker File Thread Started");

            }
        });
    }


}