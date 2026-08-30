package com.agpitcodeclub.chorchithyamultiplayer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup;
import android.view.LayoutInflater;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

    TextView tvRoomTitle, tvStatus, tvRoundsInfo; // Added tvRoundsInfo
    ListView listViewPlayers;
    Button btnStart, btnShare, btnReady;

    String playerName = "";
    String roomCode = "";
    String role = "joiner";
    String selectedAvatar = "🥷";

    FirebaseDatabase database;
    DatabaseReference roomRef;

    List<DataSnapshot> playerSnapshots;
    PlayerAdapter adapter;

    private class PlayerAdapter extends ArrayAdapter<DataSnapshot> {
        public PlayerAdapter(List<DataSnapshot> players) {
            super(RoomActivity.this, R.layout.player_list_item, players);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.player_list_item, parent, false);
            }
            DataSnapshot player = getItem(position);
            String name = player.getKey();
            String avatar = player.child("avatar").getValue(String.class);
            if (avatar == null) avatar = "🥷";

            TextView tvName = convertView.findViewById(R.id.playerNameText);
            TextView tvAvatar = convertView.findViewById(R.id.tvPlayerAvatar);
            TextView tvReady = convertView.findViewById(R.id.tvReadyStatus);

            tvName.setText(name);
            tvAvatar.setText(avatar);

            Boolean isReady = player.child("isReady").getValue(Boolean.class);
            if (isReady != null && isReady) {
                tvReady.setText("READY");
                tvReady.setTextColor(Color.parseColor("#4CAF50")); // Green
            } else {
                tvReady.setText("NOT READY");
                tvReady.setTextColor(Color.parseColor("#F44336")); // Red
            }

            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Initialize UI
        tvRoomTitle = findViewById(R.id.tvRoomTitle);
        tvStatus = findViewById(R.id.tvStatus);
        tvRoundsInfo = findViewById(R.id.tvRoundsInfo);
        listViewPlayers = findViewById(R.id.listViewPlayers);
        btnStart = findViewById(R.id.btnStartGame);
        btnShare = findViewById(R.id.btnShare);
        btnReady = findViewById(R.id.btnReady);

        database = FirebaseDatabase.getInstance();

        playerSnapshots = new ArrayList<>();
        adapter = new PlayerAdapter(playerSnapshots);
        listViewPlayers.setAdapter(adapter);

        // 3. Get Data Safely
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            playerName = extras.getString("playerName", "Player");
            selectedAvatar = extras.getString("avatar", "🥷");
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
                // Verify room exists before joining
                verifyAndJoinRoom();
            } else {
                // This shouldn't happen with the new flow, but as a fallback:
                Toast.makeText(this, "No Room Code provided", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        // 6. Share Button
        btnShare.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            String shareMessage = "Join my Chor Chithya game! \nRoom Code: " + roomCode + 
                                  "\n\nDownload App: https://play.google.com/store/apps/details?id=com.agpitcodeclub.chorchithyamultiplayer";
            intent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(intent, "Invite via"));
        });

        // 7. Start Game Button
        btnStart.setOnClickListener(v -> {
            if (playerSnapshots.size() < 2) {
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
                    List<String> notReadyPlayerNames = new ArrayList<>();

                    for (DataSnapshot player : snapshot.child("players").getChildren()) {
                        String pName = player.getKey();
                        currentLobbyPlayers.add(pName);
                        totalPlayersInLobby++;

                        Boolean isReady = player.child("isReady").getValue(Boolean.class);
                        if (isReady != null && isReady) {
                            readyPlayers++;
                        } else {
                            if (pName != null) notReadyPlayerNames.add(pName);
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
                        String names = TextUtils.join(", ", notReadyPlayerNames);
                        Toast.makeText(RoomActivity.this, names + " are not ready yet", Toast.LENGTH_SHORT).show();
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
            roomRef.child("players").child(playerName).child("avatar").setValue(selectedAvatar);

            // 🔑 FIX: Host is AUTOMATICALLY Ready
            roomRef.child("players").child(playerName).child("isReady").setValue(true);

            roomRef.child("status").onDisconnect().setValue("closed");
            addRoomEventListener();
        });
        // Change this in showCreateRoomDialog and showJoinDialog
        builder.setCancelable(true);
        builder.show();
    }

    private void setupExistingRoomAsHost() {
        tvRoomTitle.setText("Room Code: " + roomCode);
        roomRef = database.getReference("rooms").child(roomCode);
        roomRef.child("status").setValue("waiting");
        roomRef.child("players").child(playerName).child("role").setValue("host");
        roomRef.child("players").child(playerName).child("avatar").setValue(selectedAvatar);

        // 🔑 FIX: Host is AUTOMATICALLY Ready on return
        roomRef.child("players").child(playerName).child("isReady").setValue(true);

        roomRef.child("status").onDisconnect().setValue("closed");
        addRoomEventListener();
    }

    private void verifyAndJoinRoom() {
        roomRef = database.getReference("rooms").child(roomCode);
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    setupExistingRoomAsJoiner();
                } else {
                    Toast.makeText(RoomActivity.this, "Invalid Code", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RoomActivity.this, "Error connecting to room", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void setupExistingRoomAsJoiner() {
        tvRoomTitle.setText("Room Code: " + roomCode);
        roomRef = database.getReference("rooms").child(roomCode);

        // Joiners are NOT ready initially
        roomRef.child("players").child(playerName).child("isReady").setValue(false);
        roomRef.child("players").child(playerName).child("role").setValue("joiner");
        roomRef.child("players").child(playerName).child("avatar").setValue(selectedAvatar);
        roomRef.child("players").child(playerName).onDisconnect().removeValue();
        addRoomEventListener();
    }

    private void addRoomEventListener() {
        roomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot playersSnapshot = snapshot.child("players");
                DataSnapshot rosterSnapshot = snapshot.child("lastPlayerRoster");

                playerSnapshots.clear();
                int totalPlayersInLobby = 0;
                int readyPlayers = 0;
                List<String> requiredRoster = null;
                List<String> currentLobbyNames = new ArrayList<>();

                if (rosterSnapshot.exists()) {
                    requiredRoster = (List<String>) rosterSnapshot.getValue();
                }

                for (DataSnapshot player : playersSnapshot.getChildren()) {
                    playerSnapshots.add(player);
                    currentLobbyNames.add(player.getKey());
                    totalPlayersInLobby++;
                    Boolean isReady = player.child("isReady").getValue(Boolean.class);
                    if (isReady != null && isReady) readyPlayers++;
                }
                adapter.notifyDataSetChanged();

                // 🔑 FIX: Update tvStatus separately from tvRoomTitle
                tvRoomTitle.setText("Room Code: " + roomCode); // Keep title clean

                // Update Rounds Info
                Integer current = snapshot.child("currentRound").getValue(Integer.class);
                Integer total = snapshot.child("totalRounds").getValue(Integer.class);
                if (current != null && total != null) {
                    tvRoundsInfo.setText("Round " + current + " / " + total);
                }

                if (requiredRoster != null && !requiredRoster.isEmpty()) {
                    List<String> missingPlayers = new ArrayList<>(requiredRoster);
                    missingPlayers.removeAll(currentLobbyNames);

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
// Inside addRoomEventListener()
                    } else {
                        tvStatus.setText("Waiting for players...");
                        tvStatus.setTextColor(Color.parseColor("#3E2723")); // Use your theme's dark brown
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
                    intent.putExtra("avatar", selectedAvatar);
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
        java.util.Map<String, Object> updates = new java.util.HashMap<>();

        List<String> finalRoles = new ArrayList<>();
        finalRoles.add("Sipahi");
        finalRoles.add("Chor");
        List<String> extraRoles = new ArrayList<>();
        extraRoles.add("Raja");
        extraRoles.add("Mantri");
        extraRoles.add("Rani");
        extraRoles.add("Senapati");
        Collections.shuffle(extraRoles);

        int playersNeeded = playerSnapshots.size() - 2;
        for (int i = 0; i < playersNeeded; i++) {
            if (i < extraRoles.size()) finalRoles.add(extraRoles.get(i));
            else finalRoles.add("Praja");
        }

        Collections.shuffle(finalRoles);

        for (int i = 0; i < playerSnapshots.size(); i++) {
            String pName = playerSnapshots.get(i).getKey();
            if (pName == null) continue;

            // Update roles and reset readiness in one go
            boolean isHost = pName.equals(playerName) && role.equals("host");
            updates.put("players/" + pName + "/role", finalRoles.get(i));
            updates.put("players/" + pName + "/isReady", isHost);
        }

        updates.put("winner", null);
        updates.put("status", "playing");

        // Single batch update for faster start and less network overhead
        roomRef.updateChildren(updates).addOnFailureListener(e -> {
            Toast.makeText(RoomActivity.this, "Failed to start game!", Toast.LENGTH_SHORT).show();
        });
    }
}