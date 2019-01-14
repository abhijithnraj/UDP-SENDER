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
import android.widget.Button;
import android.widget.EditText;
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
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;


public class MainActivity extends AppCompatActivity {
    String ip;
    Button button;
    ProgressBar progressBar;
    long totalProgress=0;
    long currentProgress=0;
    TextView ipDisplay;
    TextView progressDisplay;
    String PORT="1234";
    EditText ipEdit,portEdit;
    Button submit;
    @Override
    public void onDestroy() {

        MainActivity.this.finish();
        super.onDestroy();
        if(getFileThread!=null) {
            getFileThread.stop();
        }
        if(readFileThread!=null) {
            readFileThread.stop();
        }

    }
    private void displayTerminal(String data){
        System.out.println(data);
    }
    private void showFileChooser() {
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

    public void UDPScan(){
        String ipString="1.2.3.4";
        System.out.println(ipString);
        String[] ipComponents=ipString.split("\\.");
        for(String s:ipComponents){
            System.out.println(s);
        }
        String ipConst=ipComponents[0]+"."+ipComponents[1]+"."+ipComponents[2]+".";
        try {
            DatagramSocket s = new DatagramSocket();
            String messageStr="Android";
            int server_port = 12345;
            for(int i=0;i<=255;i++){
                String actualIP=ipConst+i;
                InetAddress local = InetAddress.getByName(actualIP);
                int msg_length=messageStr.length();
                byte[] message = messageStr.getBytes();
                DatagramPacket p = new DatagramPacket(message, msg_length,local,server_port);
                s.send(p);
                byte[] mes = new byte[1500];
                DatagramPacket p2 = new DatagramPacket(message, message.length);
                DatagramSocket s2 = new DatagramSocket(server_port);
                s2.receive(p);
                String text = new String(message, 0, p.getLength());
                Log.d("Udp tutorial","message:" + text);
                s.close();
                System.out.println(actualIP);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public DatagramSocket establishUDPConnection(String address,String port){
        DatagramSocket s =null;
        while(true) {
            try {
                String message = "Android";
                s = new DatagramSocket();
                InetAddress local = InetAddress.getByName(address);
                int msg_length = message.length();
                byte[] m = message.getBytes();
                DatagramPacket p = new DatagramPacket(m, msg_length, local, Integer.parseInt(port));

                s.send(p);
                displayTerminal("Sending");
                final byte[] recMessage = new byte[1500];
                final DatagramPacket pRec = new DatagramPacket(recMessage, recMessage.length);
                s.setSoTimeout(2000);
                s.receive(pRec);

                final String text = new String(recMessage, 0, pRec.getLength());
                System.out.println("Text recieved is "+text);
                break;
//                if (text.equals( "Acknowledge")) {
//                    break;
//                }

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

    public void sendUDP(String hash,byte[] b){


    }
    int perc=0;
    public void readFile(String fileName) throws IOException {
        File fi = new File(fileName);
        totalProgress=fi.length();
        FileInputStream in = null;
        currentProgress=0;

        int BUFFER_SIZE= (int) (totalProgress*0.1);
        if(BUFFER_SIZE> 1000000000){
            BUFFER_SIZE=1000000000;
        }
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
                System.out.println(hash);

            }
            displayTerminal("File Reading Completed");
            Runtime.getRuntime().gc();
            String deviceIP=getDeviceIP();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            displayTerminal("Failed");

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

        switch (requestCode) {
            case 7:
                if (resultCode == RESULT_OK) {
                    String PathHolder = getPathFromURI(getBaseContext(),data.getData());
                    fileName=PathHolder;
//                    Toast.makeText(MainActivity.this, PathHolder, Toast.LENGTH_LONG).show();
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
        ipDisplay = (TextView)findViewById(R.id.txtvwIPDisplay);
        ipDisplay.setVisibility(ipDisplay.INVISIBLE);
        progressDisplay = (TextView)findViewById(R.id.txtvwProgress);
        ipEdit = (EditText)findViewById(R.id.edtIP2);
        portEdit=(EditText)findViewById(R.id.edtPort2);
        ipEdit.setVisibility(ipEdit.INVISIBLE);
        portEdit.setVisibility(portEdit.INVISIBLE);
        progressDisplay.setVisibility(progressDisplay.INVISIBLE);
        submit = (Button)findViewById(R.id.btnSubmit);
        submit.setVisibility(submit.INVISIBLE);


        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

                ip=ipEdit.getText().toString();
                PORT=portEdit.getText().toString();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.animate().setDuration(1000).translationYBy(-800);
                    }
                });

                ipEdit.setVisibility(ipEdit.INVISIBLE);
                portEdit.setVisibility(portEdit.INVISIBLE);
                submit.setVisibility(submit.INVISIBLE);
                progressDisplay.setVisibility(progressDisplay.VISIBLE);
                readFileThread.start();



            }
        });

        button.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
//                button.setVisibility(button.INVISIBLE);
                displayTerminal("CLicked");
                button.setVisibility(button.INVISIBLE);
                  readFileThread = new Thread(){
                    public void run(){
                        try {
//                            progressBar.setVisibility(progressBar.VISIBLE);
                            displayTerminal("Started Reading Files");
                            establishUDPConnection(ip,PORT);
                            readFile(fileName);
                            displayTerminal("Completed reading Files");
//                            Intent i = new Intent(MainActivity.this,MainActivity.class);
//                            startActivity(i);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                };
                  getFileThread = new Thread(){
                    public void run(){
                        fileName="";
                        showFileChooser();
                        displayTerminal("About to read files");
                        while(fileName==""){
//                            displayTerminal("Waiting for File Name to be not null");
                        };
                        displayTerminal("completed loop");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ipEdit.setVisibility(ipEdit.VISIBLE);
                                portEdit.setVisibility(portEdit.VISIBLE);
                                submit.setVisibility(submit.VISIBLE);

                            }
                        });


                    }

                };


                getFileThread.start();
                Toast.makeText(MainActivity.this, "COMPLETED", Toast.LENGTH_LONG).show();

            }
        });
    }


}