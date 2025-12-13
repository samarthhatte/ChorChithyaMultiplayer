package com.agpitcodeclub.chorchithyamultiplayer;// Check your own package name here

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    EditText etPlayerName;
    Button btnCreate, btnJoin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etPlayerName = findViewById(R.id.etPlayerName);
        btnCreate = findViewById(R.id.btnCreateRoom);
        btnJoin = findViewById(R.id.btnJoinRoom);

        // Logic for Create Room
        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String playerName = etPlayerName.getText().toString();
                if (TextUtils.isEmpty(playerName)) {
                    Toast.makeText(MainActivity.this, "Please enter your name", Toast.LENGTH_SHORT).show();
                } else {
                    // Start the Room Activity - Host Mode
                    Intent intent = new Intent(MainActivity.this, RoomActivity.class);
                    intent.putExtra("playerName", playerName);
                    intent.putExtra("mode", "host");
                    startActivity(intent);
                }
            }
        });

        // Logic for Join Room
        btnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String playerName = etPlayerName.getText().toString();
                if (TextUtils.isEmpty(playerName)) {
                    Toast.makeText(MainActivity.this, "Please enter your name", Toast.LENGTH_SHORT).show();
                } else {
                    // Start the Room Activity - Join Mode
                    Intent intent = new Intent(MainActivity.this, RoomActivity.class);
                    intent.putExtra("playerName", playerName);
                    intent.putExtra("mode", "join");
                    startActivity(intent);
                }
            }
        });
    }
}