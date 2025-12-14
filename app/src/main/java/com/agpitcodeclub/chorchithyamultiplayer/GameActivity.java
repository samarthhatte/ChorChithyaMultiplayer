package com.agpitcodeclub.chorchithyamultiplayer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GameActivity extends AppCompatActivity {

    TextView tvPlayerName, tvRole, tvHiddenText, tvResult, tvInstruction;
    CardView cardView;
    LinearLayout layoutSipahiGuess;
    ListView listViewSuspects;
    TextView tvPoliceName;
    LinearLayout cardLayout;
    String mode; // To store "host" or "joiner"
    ValueEventListener winnerListener;
    DatabaseReference winnerRef;
    String myMode;
    TextView tvRoundAnnounce; // Add this with other TextViews
    int currentRound = 1;
    int totalRounds = 1;

    String roomCode, playerName, myRole;
    boolean isRevealed = false;

    DatabaseReference roomRef;
    List<String> suspectsList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // 1. Initialize UI
        tvRoundAnnounce = findViewById(R.id.tvRoundAnnounce);
        tvInstruction = findViewById(R.id.tvInstruction);
        tvPlayerName = findViewById(R.id.tvPlayerName);
        tvRole = findViewById(R.id.tvRole);
        tvHiddenText = findViewById(R.id.tvHiddenText);
        tvResult = findViewById(R.id.tvResult);
        cardView = findViewById(R.id.cardViewRole);
        layoutSipahiGuess = findViewById(R.id.layoutSipahiGuess);
        listViewSuspects = findViewById(R.id.listViewSuspects);
        tvPoliceName = findViewById(R.id.tvPoliceName);
        cardLayout = findViewById(R.id.cardLayout);

        // 1. Get colors safely using ContextCompat
        int colorCardBack = androidx.core.content.ContextCompat.getColor(this, R.color.card_back);
        int colorCardFace = androidx.core.content.ContextCompat.getColor(this, R.color.card_face);

        // 2. Set Initial State
        cardLayout.setBackgroundColor(colorCardBack);

        // 2. Get Intent Data
        roomCode = getIntent().getStringExtra("roomCode");
        playerName = getIntent().getStringExtra("playerName");
        tvPlayerName.setText("Hi, " + playerName);
        myMode = getIntent().getStringExtra("mode");
        mode = getIntent().getStringExtra("mode"); // <--- ADD THIS LINE (Crucial!)

        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomCode);

        // Inside onCreate, after defining roomRef:
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChild("currentRound") && snapshot.hasChild("totalRounds")) {
                    currentRound = snapshot.child("currentRound").getValue(Integer.class);
                    totalRounds = snapshot.child("totalRounds").getValue(Integer.class);

                    // 1. Update Title
                    setTitle("Round " + currentRound + " / " + totalRounds);

                    // 2. TRIGGER ANIMATION HERE
                    playRoundAnimation(currentRound);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 3. Update Click Listener
        cardView.setOnClickListener(v -> {
            if (!isRevealed) {
                // REVEAL
                tvHiddenText.setVisibility(View.GONE);
                findViewById(R.id.imgHiddenIcon).setVisibility(View.GONE);
                tvRole.setVisibility(View.VISIBLE);

                cardLayout.setBackgroundColor(colorCardFace); // Use Dynamic Face Color
                isRevealed = true;
            } else {
                // HIDE
                tvHiddenText.setVisibility(View.VISIBLE);
                findViewById(R.id.imgHiddenIcon).setVisibility(View.VISIBLE);
                tvRole.setVisibility(View.GONE);

                cardLayout.setBackgroundColor(colorCardBack); // Use Dynamic Back Color
                isRevealed = false;
            }
        });

        // 4. Fetch My Role & Game Status
        fetchMyRole();
        findAndShowPolice();
        listenForWinner();
    }

    private void fetchMyRole() {
        roomRef.child("players").child(playerName).child("role").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    myRole = snapshot.getValue(String.class);
                    tvRole.setText(myRole);

                    // IF I AM SIPAHI, I need to catch the Chor!
                    if ("Sipahi".equals(myRole)) {
                        setupSipahiUI();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- SIPAHI LOGIC ---
    private void setupSipahiUI() {
        layoutSipahiGuess.setVisibility(View.VISIBLE); // Show the guessing area

        tvInstruction.setVisibility(View.GONE);

        // Load other players into the list
        roomRef.child("players").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                suspectsList.clear();
                for (DataSnapshot player : snapshot.getChildren()) {
                    String pName = player.getKey();
                    // Don't add myself to the suspect list
                    if (!pName.equals(playerName)) {
                        suspectsList.add(pName);
                    }
                }
                // Show list
                adapter = new ArrayAdapter<>(GameActivity.this, android.R.layout.simple_list_item_1, suspectsList);
                listViewSuspects.setAdapter(adapter);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Handle Click on Suspect
        listViewSuspects.setOnItemClickListener((parent, view, position, id) -> {
            String suspectedPerson = suspectsList.get(position);
            checkIfChor(suspectedPerson);
        });
    }

    private void checkIfChor(String suspectedPerson) {
        roomRef.child("players").child(suspectedPerson).child("role").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String realRole = snapshot.getValue(String.class);

                if ("Chor".equals(realRole)) {
                    // CORRECT GUESS!
                    roomRef.child("winner").setValue("Sipahi"); // Tell Firebase Sipahi won
                } else {
                    // WRONG GUESS!
                    roomRef.child("winner").setValue("Chor"); // Tell Firebase Chor won
                }
                // Hide buttons so I can't guess again
                layoutSipahiGuess.setVisibility(View.GONE);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showGameOverDialog(String winner) {
        String message = "Sipahi".equals(winner) ? "Sipahi Wins!" : "Chor Wins!";

        tvResult.setText(message);
        tvResult.setVisibility(View.VISIBLE);
        tvInstruction.setVisibility(View.GONE);

        if (myRole != null) updateScores(winner);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        // LOGIC: Is this the LAST round?
        if (currentRound < totalRounds) {
            // --- NOT FINISHED: Show "Next Round" ---
            builder.setTitle("Round " + currentRound + " Over");
            builder.setMessage(message);
            builder.setPositiveButton("Next Round", (dialog, which) -> {
                if (roomRef != null) {
                    roomRef.child("winner").removeValue();

                    // CRITICAL FIX: Everyone forces status to 'waiting' when leaving
                    // This ensures that when you land in the Lobby, it doesn't bounce you back.
                    roomRef.child("status").setValue("waiting");
                }

                // Host increments round number (Only Host Logic)
                if ("host".equals(myMode)) {
                    roomRef.child("currentRound").setValue(currentRound + 1);
                }

                goToLobby();
            });
        } else {
            // --- FINISHED: Show "See Results" ---
            builder.setTitle("Tournament Finished!");
            builder.setMessage(message + "\n\nGame Over!");
            builder.setPositiveButton("See Scoreboard", (dialog, which) -> {
                if (roomRef != null) roomRef.child("winner").removeValue();

                // Go to Dashboard
                Intent intent = new Intent(GameActivity.this, DashboardActivity.class);
                intent.putExtra("roomCode", roomCode);
                startActivity(intent);
                finish();
            });
        }

        if (!isFinishing() && !isDestroyed()) {
            builder.show();
        }
    }

    // Helper to keep code clean
    private void goToLobby() {
        Intent intent = new Intent(GameActivity.this, RoomActivity.class);
        intent.putExtra("playerName", playerName);
        intent.putExtra("roomCode", roomCode);
        intent.putExtra("mode", myMode);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // --- GLOBAL LISTENER (Everyone sees the result) ---
    private void listenForWinner() {
        winnerRef = roomRef.child("winner");

        winnerListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String winner = snapshot.getValue(String.class);

                    // CRITICAL CHECK: Is the activity valid?
                    if (!isFinishing() && !isDestroyed()) {
                        showGameOverDialog(winner);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        // Attach the listener
        winnerRef.addValueEventListener(winnerListener);
    }

    private void updateScores(String winnerRole) {
        if (myRole == null) return;

        int roundPoints = 0;
        // 1. Calculate Points
        if (myRole.equals("Raja")) roundPoints = 1000;
        else if (myRole.equals("Rani")) roundPoints = 900;
        else if (myRole.equals("Mantri")) roundPoints = 800;
        else if (myRole.equals("Senapati")) roundPoints = 500;
        else if (myRole.equals("Sipahi")) roundPoints = winnerRole.equals("Sipahi") ? 100 : 0;
        else if (myRole.equals("Chor")) roundPoints = winnerRole.equals("Chor") ? 100 : 0;

        // 2. Save to Firebase (Add to existing score)
        int finalPoints = roundPoints;
        DatabaseReference scoreRef = roomRef.child("players").child(playerName).child("score");

        scoreRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int oldScore = 0;
                if (snapshot.exists()) {
                    // Safely handle null or different types
                    try {
                        oldScore = snapshot.getValue(Integer.class);
                    } catch (Exception e) { oldScore = 0; }
                }
                scoreRef.setValue(oldScore + finalPoints);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        tvResult.append("\n\nYou got: " + roundPoints + " points!");
    }

    private void findAndShowPolice() {
        roomRef.child("players").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot player : snapshot.getChildren()) {
                    String r = player.child("role").getValue(String.class);
                    String name = player.getKey(); // Get Player Name

                    if ("Sipahi".equals(r)) {
                        tvPoliceName.setText("👮 Police is: " + name);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop listening when the screen closes!
        if (winnerRef != null && winnerListener != null) {
            winnerRef.removeEventListener(winnerListener);
        }
    }

    private void playRoundAnimation(int roundNum) {
        tvRoundAnnounce.setText("Round " + roundNum);
        tvRoundAnnounce.setVisibility(View.VISIBLE);
        tvRoundAnnounce.setAlpha(0f);
        tvRoundAnnounce.setScaleX(0.5f);
        tvRoundAnnounce.setScaleY(0.5f);

        // Animation: Fade In + Scale Up -> Wait -> Fade Out
        tvRoundAnnounce.animate()
                .alpha(1f)
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(800) // 0.8 seconds to appear
                .withEndAction(() -> {
                    // Wait a bit, then disappear
                    tvRoundAnnounce.animate()
                            .alpha(0f)
                            .scaleX(1.5f) // Keep growing while fading
                            .scaleY(1.5f)
                            .setStartDelay(500) // Stay visible for 0.5s
                            .setDuration(500)   // Fade out speed
                            .withEndAction(() -> tvRoundAnnounce.setVisibility(View.GONE))
                            .start();
                })
                .start();
    }

}