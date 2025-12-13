package com.agpitcodeclub.chorchithyamultiplayer; // Update with your package name

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RoomActivity extends AppCompatActivity {

    TextView tvRoomTitle;
    ListView listViewPlayers;
    Button btnStart, btnShare;

    String playerName = "";
    String roomCode = "";
    String role = "joiner"; // default

    FirebaseDatabase database;
    DatabaseReference roomRef;

    List<String> playerList;
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        // 1. Initialize UI
        tvRoomTitle = findViewById(R.id.tvRoomTitle);
        listViewPlayers = findViewById(R.id.listViewPlayers);
        btnStart = findViewById(R.id.btnStartGame);
        btnShare = findViewById(R.id.btnShare);

        database = FirebaseDatabase.getInstance();

        // 2. Setup List Adapter
        playerList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, playerList);
        listViewPlayers.setAdapter(adapter);

        // 3. Get Data from Previous Screen
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            playerName = extras.getString("playerName");
            role = extras.getString("mode");
        }

        // 4. Check Role
        if (role.equals("host")) {
            // I am the Host -> Create the Room
            btnStart.setVisibility(View.VISIBLE); // Host can see Start button
            createRoom();
        } else {
            // I am a Joiner -> Ask for Code
            showJoinDialog();
        }

        // 5. WhatsApp Invite Button Logic
        btnShare.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "Join my Chor Chithya game! Room Code: " + roomCode);
            intent.setPackage("com.whatsapp");
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
            }
        });

        // 6. Start Game Logic (For later)
// 6. Start Game Logic (Updated)
        btnStart.setOnClickListener(v -> {
            // Check if we have enough players (Optional: remove check for testing)
            if (playerList.size() < 2) {
                Toast.makeText(this, "Need at least 2 players to start!", Toast.LENGTH_SHORT).show();
                return;
            }
            startGame();
        });
    }

    // --- HOST LOGIC ---
    private void createRoom() {
        // Generate random 4-digit code
        int code = new Random().nextInt(9000) + 1000;
        roomCode = String.valueOf(code);
        tvRoomTitle.setText("Room Code: " + roomCode);

        // Save Room to Firebase
        roomRef = database.getReference("rooms").child(roomCode);

        // Add Host details
        roomRef.child("players").child(playerName).setValue("host");
        roomRef.child("status").setValue("waiting");

        addRoomEventListener(); // Start listening for other players
    }

    // --- JOIN LOGIC ---
    private void showJoinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Room Code");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Join", (dialog, which) -> {
            roomCode = input.getText().toString();
            tvRoomTitle.setText("Room Code: " + roomCode);
            roomRef = database.getReference("rooms").child(roomCode);

            // Check if room exists before joining
            roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Room exists, add myself
                        roomRef.child("players").child(playerName).setValue("joiner");
                        addRoomEventListener(); // Start listening
                    } else {
                        Toast.makeText(RoomActivity.this, "Invalid Room Code", Toast.LENGTH_SHORT).show();
                        finish(); // Go back
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        });
        builder.setCancelable(false); // Force them to enter code
        builder.show();
    }

    // --- LISTENER (THE MAGIC) ---
    private void addRoomEventListener() {
        roomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 1. Update Player List
                playerList.clear();
                Iterable<DataSnapshot> players = snapshot.child("players").getChildren();
                for (DataSnapshot player : players) {
                    playerList.add(player.getKey()); // Get the player Name
                }
                adapter.notifyDataSetChanged();

                // 2. Check if Game Started
// ... inside onDataChange ...

                // 2. Check if Game Started
                String status = snapshot.child("status").getValue(String.class);
                if ("playing".equals(status)) {
                    // STOP listening to the room so we don't open the game twice
                    roomRef.removeEventListener(this);

                    Intent intent = new Intent(RoomActivity.this, GameActivity.class);
                    intent.putExtra("roomCode", roomCode);
                    intent.putExtra("playerName", playerName);
                    startActivity(intent);
                    finish(); // Close the lobby
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void startGame() {
        List<String> roles = new ArrayList<>();

        // --- TEST MODE FOR 2 PLAYERS ---
        if (playerList.size() == 2) {
            // Force these roles so you can test winning/losing
            roles.add("Sipahi");
            roles.add("Chor");
        }
        // --- NORMAL MODE FOR 3+ PLAYERS ---
        else {
            roles.add("Raja");
            roles.add("Sipahi");
            roles.add("Chor");
            roles.add("Mantri");
            if (playerList.size() > 4) roles.add("Rani");
            if (playerList.size() > 5) roles.add("Senapati");
        }

        // Shuffle the roles so we don't know who gets what
        java.util.Collections.shuffle(roles);

        // Assign Roles in Firebase
        for (int i = 0; i < playerList.size(); i++) {
            String pName = playerList.get(i);
            String assignedRole = roles.get(i);
            roomRef.child("players").child(pName).child("role").setValue(assignedRole);
        }

        // Trigger game start
        roomRef.child("status").setValue("playing");
    }
}