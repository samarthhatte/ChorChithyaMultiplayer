package com.agpitcodeclub.chorchithyamultiplayer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        ListView listView = findViewById(R.id.listViewScores);
        Button btnBack = findViewById(R.id.btnBackToMenu);
        ArrayList<String> scoresList = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, scoresList);
        listView.setAdapter(adapter);

        // Get Room Code passed from GameActivity
        String roomCode = getIntent().getStringExtra("roomCode");

        if (roomCode != null) {
            // Fetch Scores from Firebase
            FirebaseDatabase.getInstance().getReference("rooms").child(roomCode).child("players")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            scoresList.clear();
                            // Loop through all players
                            for (DataSnapshot p : snapshot.getChildren()) {
                                String name = p.getKey();
                                // Handle score safely (default to 0 if null)
                                int score = 0;
                                if (p.hasChild("score")) {
                                    try {
                                        score = p.child("score").getValue(Integer.class);
                                    } catch (Exception e) {}
                                }
                                scoresList.add(name + ": " + score + " pts");
                            }
                            // Sort scores (Highest at top)
                            // This is a simple string sort; for better integer sorting you'd need a custom object,
                            // but this works for basic names.
                            Collections.sort(scoresList, Collections.reverseOrder());
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }

        // Back to Main Menu Logic
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}