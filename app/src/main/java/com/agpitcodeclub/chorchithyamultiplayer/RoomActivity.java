package com.agpitcodeclub.chorchithyamultiplayer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
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
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RoomActivity extends AppCompatActivity {

    TextView tvRoomTitle, tvStatus; // Added tvStatus
    ListView listViewPlayers;
    Button btnStart, btnShare, btnReady;

    String playerName = "";
    String roomCode = "";
    String role = "joiner";

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
        tvStatus = findViewById(R.id.tvStatus); // <--- NEW
        listViewPlayers = findViewById(R.id.listViewPlayers);
        btnStart = findViewById(R.id.btnStartGame);
        btnShare = findViewById(R.id.btnShare);
        btnReady = findViewById(R.id.btnReady);

        database = FirebaseDatabase.getInstance();

        playerList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, playerList);
        listViewPlayers.setAdapter(adapter);

        // 3. Get Data Safely
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            playerName = extras.getString("playerName", "Player");
            if (extras.containsKey("mode")) role = extras.getString("mode");
            if (role == null) role = "joiner";
            if (extras.containsKey("roomCode")) roomCode = extras.getString("roomCode");
        }

        // 4. Host Logic
        if (role.equals("host")) {
            btnStart.setVisibility(View.VISIBLE);
            btnReady.setVisibility(View.GONE); // Host is auto-ready

            if (roomCode != null && !roomCode.isEmpty()) {
                setupExistingRoomAsHost();
            } else {
                showCreateRoomDialog();
            }
        }
        // 5. Joiner Logic
        else {
            btnStart.setVisibility(View.GONE);
            btnReady.setVisibility(View.VISIBLE);

            if (roomCode != null && !roomCode.isEmpty()) {
                setupExistingRoomAsJoiner();
            } else {
                showJoinDialog();
            }
        }

        // 6. Share Button
        btnShare.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "Join my game! Code: " + roomCode);
            intent.setPackage("com.whatsapp");
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
            }
        });

        // 7. Start Game Button
        btnStart.setOnClickListener(v -> {
            if (playerList.size() < 2) {
                Toast.makeText(this, "Need at least 2 players!", Toast.LENGTH_SHORT).show();
                return;
            }

            roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<String> requiredRoster = null;
                    if (snapshot.hasChild("lastPlayerRoster")) {
                        requiredRoster = (List<String>) snapshot.child("lastPlayerRoster").getValue();
                    }

                    int totalPlayersInLobby = 0;
                    int readyPlayers = 0;
                    List<String> currentLobbyPlayers = new ArrayList<>();

                    for (DataSnapshot player : snapshot.child("players").getChildren()) {
                        currentLobbyPlayers.add(player.getKey());
                        totalPlayersInLobby++;

                        Boolean isReady = player.child("isReady").getValue(Boolean.class);
                        if (isReady != null && isReady) {
                            readyPlayers++;
                        }
                    }

                    if (requiredRoster != null && !requiredRoster.isEmpty()) {
                        List<String> missingPlayers = new ArrayList<>(requiredRoster);
                        missingPlayers.removeAll(currentLobbyPlayers);

                        if (!missingPlayers.isEmpty()) {
                            Toast.makeText(RoomActivity.this, "Still missing players!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    if (readyPlayers < totalPlayersInLobby) {
                        Toast.makeText(RoomActivity.this, "Wait! Someone is not ready.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    startGame();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        });

        // 8. Ready Button Logic (Joiners only)
        btnReady.setOnClickListener(v -> {
            roomRef.child("players").child(playerName).child("isReady").setValue(true);
            btnReady.setVisibility(View.GONE); // Intended behavior: Hide after clicking
            Toast.makeText(this, "Marked as Ready!", Toast.LENGTH_SHORT).show();
        });
    }

    private void showCreateRoomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tournament Settings");
        builder.setMessage("How many rounds?");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText("5");
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String roundsStr = input.getText().toString().trim();
            int totalRounds = roundsStr.isEmpty() ? 5 : Integer.parseInt(roundsStr);

            int code = new Random().nextInt(9000) + 1000;
            roomCode = String.valueOf(code);
            tvRoomTitle.setText("Room Code: " + roomCode);

            roomRef = database.getReference("rooms").child(roomCode);
            roomRef.child("status").setValue("waiting");
            roomRef.child("totalRounds").setValue(totalRounds);
            roomRef.child("currentRound").setValue(1);

            roomRef.child("players").child(playerName).child("role").setValue("host");
            roomRef.child("players").child(playerName).child("score").setValue(0);

            // 🔑 FIX: Host is AUTOMATICALLY Ready
            roomRef.child("players").child(playerName).child("isReady").setValue(true);

            roomRef.child("status").onDisconnect().setValue("closed");
            addRoomEventListener();
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void setupExistingRoomAsHost() {
        tvRoomTitle.setText("Room Code: " + roomCode);
        roomRef = database.getReference("rooms").child(roomCode);
        roomRef.child("status").setValue("waiting");
        roomRef.child("players").child(playerName).child("role").setValue("host");

        // 🔑 FIX: Host is AUTOMATICALLY Ready on return
        roomRef.child("players").child(playerName).child("isReady").setValue(true);

        roomRef.child("status").onDisconnect().setValue("closed");
        addRoomEventListener();
    }

    private void showJoinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Room Code");
        final EditText input = new EditText(this);
        builder.setView(input);
        builder.setPositiveButton("Join", (dialog, which) -> {
            roomCode = input.getText().toString().trim();
            if (roomCode.isEmpty()) {
                showJoinDialog();
                return;
            }
            roomRef = database.getReference("rooms").child(roomCode);
            roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) setupExistingRoomAsJoiner();
                    else {
                        Toast.makeText(RoomActivity.this, "Invalid Code", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void setupExistingRoomAsJoiner() {
        tvRoomTitle.setText("Room Code: " + roomCode);
        roomRef = database.getReference("rooms").child(roomCode);

        // Joiners are NOT ready initially
        roomRef.child("players").child(playerName).child("isReady").setValue(false);
        roomRef.child("players").child(playerName).child("role").setValue("joiner");
        roomRef.child("players").child(playerName).onDisconnect().removeValue();
        addRoomEventListener();
    }

    private void addRoomEventListener() {
        roomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot playersSnapshot = snapshot.child("players");
                DataSnapshot rosterSnapshot = snapshot.child("lastPlayerRoster");

                playerList.clear();
                int totalPlayersInLobby = 0;
                int readyPlayers = 0;
                List<String> requiredRoster = null;

                if (rosterSnapshot.exists()) {
                    requiredRoster = (List<String>) rosterSnapshot.getValue();
                }

                for (DataSnapshot player : playersSnapshot.getChildren()) {
                    playerList.add(player.getKey());
                    totalPlayersInLobby++;
                    Boolean isReady = player.child("isReady").getValue(Boolean.class);
                    if (isReady != null && isReady) readyPlayers++;
                }
                adapter.notifyDataSetChanged();

                // 🔑 FIX: Update tvStatus separately from tvRoomTitle
                tvRoomTitle.setText("Room Code: " + roomCode); // Keep title clean

                if (requiredRoster != null && !requiredRoster.isEmpty()) {
                    List<String> missingPlayers = new ArrayList<>(requiredRoster);
                    missingPlayers.removeAll(playerList);

                    if (!missingPlayers.isEmpty()) {
                        tvStatus.setText("⚠️ MISSING: " + String.join(", ", missingPlayers));
                        tvStatus.setTextColor(Color.RED);
                        btnStart.setEnabled(false);
                    } else if (readyPlayers < totalPlayersInLobby) {
                        tvStatus.setText("Waiting for " + (totalPlayersInLobby - readyPlayers) + " to click READY");
                        tvStatus.setTextColor(Color.parseColor("#FFA500")); // Orange
                        btnStart.setEnabled(false);
                    } else {
                        tvStatus.setText("ALL PLAYERS READY!");
                        tvStatus.setTextColor(Color.GREEN);
                        btnStart.setEnabled(true);
                    }
                } else {
                    // Initial Lobby Logic
                    if (totalPlayersInLobby >= 2 && readyPlayers == totalPlayersInLobby) {
                        tvStatus.setText("Ready to Start");
                        tvStatus.setTextColor(Color.GREEN);
                        btnStart.setEnabled(true);
                    } else {
                        tvStatus.setText("Waiting for players...");
                        tvStatus.setTextColor(Color.BLACK);
                        btnStart.setEnabled(false);
                    }
                }

                String status = snapshot.child("status").getValue(String.class);
                if ("closed".equals(status)) {
                    Toast.makeText(RoomActivity.this, "Host closed room", Toast.LENGTH_SHORT).show();
                    finish();
                }
                if ("playing".equals(status)) {
                    roomRef.removeEventListener(this);
                    Intent intent = new Intent(RoomActivity.this, GameActivity.class);
                    intent.putExtra("roomCode", roomCode);
                    intent.putExtra("playerName", playerName);
                    intent.putExtra("mode", role);
                    startActivity(intent);
                    finish();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void startGame() {
        // Reset readiness for next round
        for (String pName : playerList) {
            // Host stays ready, Joiners must re-ready
            boolean isHost = pName.equals(playerName) && role.equals("host");
            roomRef.child("players").child(pName).child("isReady").setValue(isHost);
        }

        List<String> finalRoles = new ArrayList<>();
        finalRoles.add("Sipahi");
        finalRoles.add("Chor");
        List<String> extraRoles = new ArrayList<>();
        extraRoles.add("Raja");
        extraRoles.add("Mantri");
        extraRoles.add("Rani");
        extraRoles.add("Senapati");
        Collections.shuffle(extraRoles);

        int playersNeeded = playerList.size() - 2;
        for (int i = 0; i < playersNeeded; i++) {
            if (i < extraRoles.size()) finalRoles.add(extraRoles.get(i));
            else finalRoles.add("Praja");
        }

        Collections.shuffle(finalRoles);
        for (int i = 0; i < playerList.size(); i++) {
            roomRef.child("players").child(playerList.get(i)).child("role").setValue(finalRoles.get(i));
        }

        roomRef.child("winner").removeValue();
        roomRef.child("status").setValue("playing");
    }
}