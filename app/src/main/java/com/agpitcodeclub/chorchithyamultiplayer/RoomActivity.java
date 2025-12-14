package com.agpitcodeclub.chorchithyamultiplayer;

import android.content.Intent;
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

        // 3. Get Data Safely
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            playerName = extras.getString("playerName", "Player");

            if (extras.containsKey("mode")) {
                role = extras.getString("mode");
            }
            if (role == null) role = "joiner";

            // CHECK: Did we come from "Next Round"?
            if (extras.containsKey("roomCode")) {
                roomCode = extras.getString("roomCode");
            }
        }

        // 4. Host Logic
        if (role.equals("host")) {
            btnStart.setVisibility(View.VISIBLE);

            // CASE A: Returning from a game (Next Round) -> Reuse Room
            if (roomCode != null && !roomCode.isEmpty()) {
                setupExistingRoomAsHost();
            }
            // CASE B: Brand New Game -> Ask for Rounds
            else {
                showCreateRoomDialog();
            }
        }
        // 5. Joiner Logic
        else {
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
            startGame();
        });
    }

    // --- NEW: ASK FOR ROUNDS DIALOG ---
    private void showCreateRoomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tournament Settings");
        builder.setMessage("How many rounds do you want to play?");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText("5"); // Default to 5 rounds
        builder.setView(input);

        builder.setPositiveButton("Create Room", (dialog, which) -> {
            String roundsStr = input.getText().toString().trim();
            int totalRounds = roundsStr.isEmpty() ? 5 : Integer.parseInt(roundsStr);

            // 1. Generate Code
            int code = new Random().nextInt(9000) + 1000;
            roomCode = String.valueOf(code);
            tvRoomTitle.setText("Room Code: " + roomCode);

            // 2. Initialize Firebase for Tournament
            roomRef = database.getReference("rooms").child(roomCode);

            roomRef.child("status").setValue("waiting");
            roomRef.child("totalRounds").setValue(totalRounds);
            roomRef.child("currentRound").setValue(1); // Start at Round 1

            // Add Host
            roomRef.child("players").child(playerName).child("role").setValue("host");
            roomRef.child("players").child(playerName).child("score").setValue(0); // Init Score

            addRoomEventListener();
        });

        builder.setCancelable(false);
        builder.show();
    }

    // --- REUSE EXISTING ROOM (NEXT ROUND) ---
    private void setupExistingRoomAsHost() {
        tvRoomTitle.setText("Room Code: " + roomCode);
        roomRef = database.getReference("rooms").child(roomCode);

        // Reset status so joiners don't auto-start immediately
        roomRef.child("status").setValue("waiting");

        // Ensure Host is present
        roomRef.child("players").child(playerName).child("role").setValue("host");

        addRoomEventListener();
    }

    // --- JOIN LOGIC ---
    private void showJoinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Room Code");
        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Join", (dialog, which) -> {
            roomCode = input.getText().toString().trim();
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
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void setupExistingRoomAsJoiner() {
        tvRoomTitle.setText("Room Code: " + roomCode);
        roomRef = database.getReference("rooms").child(roomCode);

        roomRef.child("players").child(playerName).child("role").setValue("joiner");
        // Initialize score safely (using updateChildren to avoid overwriting if exists)
        // For simplicity here, we just set it if it doesn't exist logic is handled by game flow

        addRoomEventListener();
    }

    // --- LISTENER ---
    private void addRoomEventListener() {
        roomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                playerList.clear();
                for (DataSnapshot player : snapshot.child("players").getChildren()) {
                    playerList.add(player.getKey());
                }
                adapter.notifyDataSetChanged();

                String status = snapshot.child("status").getValue(String.class);
                if ("playing".equals(status)) {
                    roomRef.removeEventListener(this);

                    Intent intent = new Intent(RoomActivity.this, GameActivity.class);
                    intent.putExtra("roomCode", roomCode);
                    intent.putExtra("playerName", playerName);
                    intent.putExtra("mode", role); // Pass Host/Joiner status
                    startActivity(intent);
                    finish();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- START GAME (DYNAMIC ROLES) ---
    private void startGame() {
        List<String> finalRoles = new ArrayList<>();

        // 1. Core
        finalRoles.add("Sipahi");
        finalRoles.add("Chor");

        // 2. Extra
        List<String> extraRoles = new ArrayList<>();
        extraRoles.add("Raja");
        extraRoles.add("Mantri");
        extraRoles.add("Rani");
        extraRoles.add("Senapati");
        Collections.shuffle(extraRoles);

        // 3. Fill
        int playersNeeded = playerList.size() - 2;
        for (int i = 0; i < playersNeeded; i++) {
            if (i < extraRoles.size()) finalRoles.add(extraRoles.get(i));
            else finalRoles.add("Praja");
        }

        // 4. Shuffle & Assign
        Collections.shuffle(finalRoles);
        for (int i = 0; i < playerList.size(); i++) {
            String pName = playerList.get(i);
            String assignedRole = finalRoles.get(i);
            roomRef.child("players").child(pName).child("role").setValue(assignedRole);

            // Ensure every player has a score field if they are new
            if (role.equals("host")) {
                // Host initializes scores to 0 if this is Round 1,
                // but since we track scores in GameActivity, we leave existing scores alone.
            }
        }

        // 5. Trigger Start
        roomRef.child("winner").removeValue();
        roomRef.child("status").setValue("playing");
    }
}