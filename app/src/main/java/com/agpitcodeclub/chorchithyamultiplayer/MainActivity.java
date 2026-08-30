package com.agpitcodeclub.chorchithyamultiplayer;// Check your own package name here

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    EditText etPlayerName;
    Button btnCreate, btnJoin, btnInvite;
    String selectedAvatar = "🥷"; // Default avatar
    private static final String APP_URL = "https://play.google.com/store/apps/details?id=com.agpitcodeclub.chorchithyamultiplayer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etPlayerName = findViewById(R.id.etPlayerName);
        btnCreate = findViewById(R.id.btnCreateRoom);
        btnJoin = findViewById(R.id.btnJoinRoom);
        btnInvite = findViewById(R.id.btnInviteFriends);

        setupAvatarSelection();

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
                    intent.putExtra("avatar", selectedAvatar);
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
                    showJoinDialog(playerName);
                }
            }
        });

        // Logic for Invite Friends
        btnInvite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, "Hey! Download Chor Chithya Multiplayer and play with me: " + APP_URL);
                startActivity(Intent.createChooser(intent, "Share via"));
            }
        });
    }

    private void showJoinDialog(String playerName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Room Code");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setTextColor(Color.BLACK);
        input.setHintTextColor(Color.GRAY);
        builder.setView(input);

        builder.setPositiveButton("Join", (dialog, which) -> {
            String roomCode = input.getText().toString().trim();
            if (roomCode.isEmpty()) {
                Toast.makeText(this, "Please enter a code", Toast.LENGTH_SHORT).show();
                return;
            }
            // Start RoomActivity with the code
            Intent intent = new Intent(MainActivity.this, RoomActivity.class);
            intent.putExtra("playerName", playerName);
            intent.putExtra("avatar", selectedAvatar);
            intent.putExtra("mode", "join");
            intent.putExtra("roomCode", roomCode);
            startActivity(intent);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void setupAvatarSelection() {
        TextView[] avatars = {
                findViewById(R.id.avatar1),
                findViewById(R.id.avatar2),
                findViewById(R.id.avatar3),
                findViewById(R.id.avatar4),
                findViewById(R.id.avatar5)
        };

        for (TextView tv : avatars) {
            tv.setOnClickListener(v -> {
                // Reset all
                for (TextView a : avatars) a.setBackground(null);
                // Select this one
                tv.setBackgroundResource(R.drawable.rounded_bg);
                selectedAvatar = tv.getText().toString();
            });
        }
        // Initial selection
        avatars[0].setBackgroundResource(R.drawable.rounded_bg);
    }
}