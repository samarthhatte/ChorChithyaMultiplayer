package com.agpitcodeclub.chorchithyamultiplayer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    // 1. Create a helper class to handle sorting logic
    private static class PlayerScore implements Comparable<PlayerScore> {
        String name;
        int score;

        public PlayerScore(String name, int score) {
            this.name = name;
            this.score = score;
        }

        // This allows Collections.sort to sort by score (High to Low)
        @Override
        public int compareTo(PlayerScore other) {
            return Integer.compare(other.score, this.score); // Descending order
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        ListView listView = findViewById(R.id.listViewScores);
        Button btnBack = findViewById(R.id.btnBackToMenu);

        // This list will hold the formatted strings for the UI
        ArrayList<String> displayList = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        listView.setAdapter(adapter);

        String roomCode = getIntent().getStringExtra("roomCode");

        if (roomCode != null) {
            FirebaseDatabase.getInstance().getReference("rooms").child(roomCode).child("players")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            List<PlayerScore> rawList = new ArrayList<>();

                            // 2. Fetch data and store in object list
                            for (DataSnapshot p : snapshot.getChildren()) {
                                String name = p.getKey();
                                int score = 0;
                                if (p.hasChild("score")) {
                                    try {
                                        score = p.child("score").getValue(Integer.class);
                                    } catch (Exception e) { score = 0; }
                                }
                                rawList.add(new PlayerScore(name, score));
                            }

                            // 3. Sort numerically (Highest Score First)
                            Collections.sort(rawList);

                            // 4. Format the output with Ranks
                            displayList.clear();
                            for (int i = 0; i < rawList.size(); i++) {
                                PlayerScore p = rawList.get(i);
                                String rankPrefix;
                                String suffix = "";

                                // Logic for 1st, 2nd, 3rd, and Loser
                                if (i == 0) {
                                    rankPrefix = "🥇 1st";
                                    suffix = " (Winner!)";
                                } else if (i == 1) {
                                    rankPrefix = "🥈 2nd";
                                } else if (i == 2) {
                                    rankPrefix = "🥉 3rd";
                                } else if (i == rawList.size() - 1 && rawList.size() > 1) {
                                    // Only show 'Loser' if it's the very last person and there's more than 1 player
                                    rankPrefix = "🤡 Last";
                                    suffix = " (Better luck next time)";
                                } else {
                                    rankPrefix = "#" + (i + 1);
                                }

                                // Final String Format: "🥇 1st - Name: 1000 (Winner!)"
                                displayList.add(rankPrefix + " - " + p.name + ": " + p.score + suffix);
                            }

                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(DashboardActivity.this, "Failed to load scores", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}