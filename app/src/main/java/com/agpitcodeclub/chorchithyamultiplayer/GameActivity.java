package com.agpitcodeclub.chorchithyamultiplayer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GameActivity extends AppCompatActivity {

    // Views
    TextView tvPlayerName, tvRole, tvHiddenText, tvResult, tvInstruction, tvPlayerAvatar;
    TextView tvRoleIcon, tvRolePoints, tvRoleSubtitle, tvRoleTag;
    ImageView imgHiddenIcon;
    MaterialCardView cardView;
    LinearLayout cardHiddenLayout;
    ConstraintLayout cardRevealedLayout;
    LinearLayout layoutSipahiGuess;
    ListView listViewSuspects;
    TextView tvPoliceName;
    TextView tvRoundAnnounce;

    // Game Data
    String mode;
    ValueEventListener winnerListener;
    DatabaseReference winnerRef;
    String myMode;
    int currentRound = 1;
    int totalRounds = 1;

    String roomCode, playerName, myRole;
    boolean isRevealed = false;

    DatabaseReference roomRef;
    List<String> suspectsList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Initialize Views
        tvRoundAnnounce = findViewById(R.id.tvRoundAnnounce);
        tvInstruction = findViewById(R.id.tvInstruction);
        tvPlayerName = findViewById(R.id.tvPlayerName);
        tvPlayerAvatar = findViewById(R.id.tvPlayerAvatar);
        tvRole = findViewById(R.id.tvRole);
        tvHiddenText = findViewById(R.id.tvHiddenText);
        imgHiddenIcon = findViewById(R.id.imgHiddenIcon);
        tvResult = findViewById(R.id.tvResult);
        cardView = findViewById(R.id.cardViewRole);
        cardHiddenLayout = findViewById(R.id.cardHiddenLayout);
        cardRevealedLayout = findViewById(R.id.cardRevealedLayout);
        tvRoleIcon = findViewById(R.id.tvRoleIcon);
        tvRolePoints = findViewById(R.id.tvRolePoints);
        tvRoleSubtitle = findViewById(R.id.tvRoleSubtitle);
        tvRoleTag = findViewById(R.id.tvRoleTag);

        layoutSipahiGuess = findViewById(R.id.layoutSipahiGuess);
        listViewSuspects = findViewById(R.id.listViewSuspects);
        tvPoliceName = findViewById(R.id.tvPoliceName);

        // 2. Intent Data
        roomCode = getIntent().getStringExtra("roomCode");
        playerName = getIntent().getStringExtra("playerName");
        String avatar = getIntent().getStringExtra("avatar");
        if (avatar == null) avatar = "🥷";
        tvPlayerAvatar.setText(avatar);

        if (playerName == null || playerName.isEmpty()) {
            Toast.makeText(this, R.string.toast_name_missing, Toast.LENGTH_SHORT).show();
            playerName = "Player";
        }
        tvPlayerName.setText(getString(R.string.label_hi_player, playerName));
        myMode = getIntent().getStringExtra("mode");
        mode = getIntent().getStringExtra("mode");

        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomCode);

        // Fetch Round Data
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer fetchedCurrent = snapshot.child("currentRound").getValue(Integer.class);
                Integer fetchedTotal = snapshot.child("totalRounds").getValue(Integer.class);

                if (fetchedCurrent != null && fetchedTotal != null) {
                    currentRound = fetchedCurrent;
                    totalRounds = fetchedTotal;
                    setTitle(getString(R.string.label_round_count, currentRound, totalRounds));
                    playRoundAnimation(currentRound);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 3. Card Tap-to-Reveal Logic
        cardView.setOnClickListener(v -> {
            if (myRole == null) {
                Toast.makeText(this, R.string.toast_loading_role, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isRevealed) {
                revealCardUI(myRole);
                isRevealed = true;
            } else {
                hideCardUI();
                isRevealed = false;
            }
        });

        // 4. Fetch Role & Game State
        fetchMyRole();
        findAndShowPolice();
        listenForWinner();
    }

    private void revealCardUI(String roleKey) {
        GameRole role = GameRole.fromKey(roleKey);

        // Set Content
        tvRole.setText(role.getEnglishTitle());
        tvRoleIcon.setText(role.getIcon());
        tvRolePoints.setText(role.getPointsText());
        tvRoleSubtitle.setText(role.getSubtitle());

        // Apply Gradient & Stroke
        cardRevealedLayout.setBackgroundResource(role.getBackgroundRes());
        cardView.setStrokeColor(role.getStrokeColor());

        // Switch Visibilities
        cardHiddenLayout.setVisibility(View.GONE);
        cardRevealedLayout.setVisibility(View.VISIBLE);
        tvInstruction.setText(getString(R.string.label_keep_secret));
    }

    private void hideCardUI() {
        cardRevealedLayout.setVisibility(View.GONE);
        cardHiddenLayout.setVisibility(View.VISIBLE);
        cardView.setStrokeColor(Color.parseColor("#33FFFFFF"));
    }

    private void fetchMyRole() {
        roomRef.child("players").child(playerName).child("role").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    myRole = snapshot.getValue(String.class);

                    // Pre-fill card content so it's ready when tapped
                    GameRole role = GameRole.fromKey(myRole);
                    tvRole.setText(role.getEnglishTitle());

                    if ("sipahi".equals(myRole)) {
                        setupSipahiUI();
                    } else {
                        layoutSipahiGuess.setVisibility(View.GONE);
                        tvInstruction.setVisibility(View.VISIBLE);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- SIPAHI LOGIC ---
    private void setupSipahiUI() {
        layoutSipahiGuess.setVisibility(View.VISIBLE);
        tvInstruction.setVisibility(View.GONE);

        roomRef.child("players").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                suspectsList.clear();
                for (DataSnapshot player : snapshot.getChildren()) {
                    String pName = player.getKey();
                    if (pName != null && !pName.equals(playerName)) {
                        suspectsList.add(pName);
                    }
                }

                if (adapter == null) {
                    adapter = new ArrayAdapter<String>(GameActivity.this, android.R.layout.simple_list_item_1, suspectsList) {
                        @NonNull
                        @Override
                        public View getView(int position, View convertView, @NonNull android.view.ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
                            TextView textView = view.findViewById(android.R.id.text1);
                            textView.setTextColor(androidx.core.content.ContextCompat.getColor(GameActivity.this, R.color.text_primary));
                            return view;
                        }
                    };
                    listViewSuspects.setAdapter(adapter);
                } else {
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

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

                if ("chor".equals(realRole)) {
                    roomRef.child("winner").setValue("sipahi");
                } else {
                    roomRef.child("winner").setValue("chor");
                }
                layoutSipahiGuess.setVisibility(View.GONE);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- GAME OVER & NEXT ROUND LOGIC ---
    private void showGameOverDialog(String winner) {
        String message = "sipahi".equals(winner) ? getString(R.string.msg_sipahi_wins) : getString(R.string.msg_chor_wins);

        tvResult.setText(message);
        tvResult.setVisibility(View.VISIBLE);
        tvInstruction.setVisibility(View.GONE);

        if (myRole != null) updateScores(winner);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        if (currentRound < totalRounds) {
            builder.setTitle(getString(R.string.dialog_round_over, currentRound));
            builder.setMessage(message);

            builder.setPositiveButton(R.string.btn_next_round, (dialog, which) -> {
                if (roomRef != null) {
                    roomRef.child("winner").removeValue();
                    roomRef.child("status").setValue("waiting");
                }

                roomRef.child("players").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> lastPlayerRoster = new ArrayList<>();
                        for (DataSnapshot player : snapshot.getChildren()) {
                            lastPlayerRoster.add(player.getKey());
                        }

                        roomRef.child("lastPlayerRoster").setValue(lastPlayerRoster)
                                .addOnCompleteListener(task -> {
                                    if ("host".equals(myMode)) {
                                        roomRef.child("currentRound").setValue(currentRound + 1);
                                    }

                                    Intent intent = new Intent(GameActivity.this, RoomActivity.class);
                                    intent.putExtra("playerName", playerName);
                                    intent.putExtra("roomCode", roomCode);
                                    intent.putExtra("avatar", tvPlayerAvatar.getText().toString());
                                    intent.putExtra("mode", myMode);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                });
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Intent intent = new Intent(GameActivity.this, RoomActivity.class);
                        intent.putExtra("playerName", playerName);
                        intent.putExtra("roomCode", roomCode);
                        intent.putExtra("avatar", tvPlayerAvatar.getText().toString());
                        intent.putExtra("mode", myMode);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
            });

        } else {
            RateAppManager.showRateDialogEveryMatch(GameActivity.this, () -> {
                builder.setTitle(R.string.dialog_tournament_finished);
                builder.setMessage(message + "\n\n" + getString(R.string.msg_game_over));
                builder.setPositiveButton(R.string.btn_see_scoreboard, (dialog, which) -> {
                    if (roomRef != null) roomRef.child("winner").removeValue();

                    Intent intent = new Intent(GameActivity.this, DashboardActivity.class);
                    intent.putExtra("roomCode", roomCode);
                    startActivity(intent);
                    finish();
                });

                if (!isFinishing() && !isDestroyed()) {
                    builder.show();
                }
            });
            return;
        }

        if (!isFinishing() && !isDestroyed()) {
            builder.show();
        }
    }

    private void listenForWinner() {
        winnerRef = roomRef.child("winner");

        winnerListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String winner = snapshot.getValue(String.class);
                    if (!isFinishing() && !isDestroyed()) {
                        showGameOverDialog(winner);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        winnerRef.addValueEventListener(winnerListener);
    }

    private void updateScores(String winnerRole) {
        if (myRole == null) return;

        int roundPoints = 0;
        if (myRole.equals("raja")) roundPoints = 1000;
        else if (myRole.equals("rani")) roundPoints = 900;
        else if (myRole.equals("mantri")) roundPoints = 800;
        else if (myRole.equals("senapati")) roundPoints = 500;
        else if (myRole.equals("sipahi")) roundPoints = winnerRole.equals("sipahi") ? 100 : 0;
        else if (myRole.equals("chor")) roundPoints = winnerRole.equals("chor") ? 100 : 0;

        int finalPoints = roundPoints;
        DatabaseReference scoreRef = roomRef.child("players").child(playerName).child("score");

        scoreRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int oldScore = 0;
                if (snapshot.exists()) {
                    try {
                        oldScore = snapshot.getValue(Integer.class);
                    } catch (Exception e) { oldScore = 0; }
                }
                scoreRef.setValue(oldScore + finalPoints);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        tvResult.append("\n\n" + getString(R.string.msg_points_gained, roundPoints));
    }

    private void findAndShowPolice() {
        roomRef.child("players").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot player : snapshot.getChildren()) {
                    String r = player.child("role").getValue(String.class);
                    if ("sipahi".equals(r)) {
                        String name = player.getKey();
                        tvPoliceName.setText(getString(R.string.label_police_is, name));
                        break;
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
        if (winnerRef != null && winnerListener != null) {
            winnerRef.removeEventListener(winnerListener);
        }
    }

    private void playRoundAnimation(int roundNum) {
        tvRoundAnnounce.setText(getString(R.string.label_round_announce, roundNum));
        tvRoundAnnounce.setVisibility(View.VISIBLE);
        tvRoundAnnounce.setAlpha(0f);
        tvRoundAnnounce.setScaleX(0.5f);
        tvRoundAnnounce.setScaleY(0.5f);

        tvRoundAnnounce.animate()
                .alpha(1f)
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(800)
                .withEndAction(() -> tvRoundAnnounce.animate()
                        .alpha(0f)
                        .scaleX(1.5f)
                        .setStartDelay(500)
                        .setDuration(500)
                        .withEndAction(() -> tvRoundAnnounce.setVisibility(View.GONE))
                        .start())
                .start();
    }
}