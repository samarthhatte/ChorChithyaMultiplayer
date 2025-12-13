package com.agpitcodeclub.chorchithyamultiplayer;

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

    TextView tvPlayerName, tvRole, tvHiddenText, tvResult;
    CardView cardView;
    LinearLayout layoutSipahiGuess;
    ListView listViewSuspects;

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
        tvPlayerName = findViewById(R.id.tvPlayerName);
        tvRole = findViewById(R.id.tvRole);
        tvHiddenText = findViewById(R.id.tvHiddenText);
        tvResult = findViewById(R.id.tvResult);
        cardView = findViewById(R.id.cardViewRole);
        layoutSipahiGuess = findViewById(R.id.layoutSipahiGuess);
        listViewSuspects = findViewById(R.id.listViewSuspects);

        // 2. Get Intent Data
        roomCode = getIntent().getStringExtra("roomCode");
        playerName = getIntent().getStringExtra("playerName");
        tvPlayerName.setText("Hi, " + playerName);

        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomCode);

        // 3. Reveal Card Logic
        cardView.setOnClickListener(v -> {
            if (!isRevealed) {
                tvHiddenText.setVisibility(View.GONE);
                tvRole.setVisibility(View.VISIBLE);
                isRevealed = true;
            } else {
                tvHiddenText.setVisibility(View.VISIBLE);
                tvRole.setVisibility(View.GONE);
                isRevealed = false;
            }
        });

        // 4. Fetch My Role & Game Status
        fetchMyRole();
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

    // --- GLOBAL LISTENER (Everyone sees the result) ---
    private void listenForWinner() {
        roomRef.child("winner").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String winner = snapshot.getValue(String.class);
                    showGameOverDialog(winner);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showGameOverDialog(String winner) {
        String message = "";
        if ("Sipahi".equals(winner)) {
            message = "Sipahi Caught the Thief!\nSipahi Wins!";
        } else {
            message = "Wrong Guess!\nChor Wins!";
        }

        tvResult.setText(message);
        tvResult.setVisibility(View.VISIBLE);

        new AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}