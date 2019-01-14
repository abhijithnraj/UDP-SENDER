package com.example.root.udp_send;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ChoiseActivity extends AppCompatActivity {

    Button send,recieve;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choise);
        send = (Button)findViewById(R.id.btnSend);
        recieve=(Button)findViewById(R.id.btnRecieve);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(ChoiseActivity.this,MainActivity.class);
                startActivity(i);
            }
        });
        recieve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(ChoiseActivity.this,RecieveActivity.class);
                startActivity(i);
            }
        });
    }
}
