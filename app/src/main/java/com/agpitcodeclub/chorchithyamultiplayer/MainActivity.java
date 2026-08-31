package com.agpitcodeclub.chorchithyamultiplayer;// Check your own package name here

import android.content.Context;
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
    android.widget.ImageButton btnSettings;
    String selectedAvatar = "🥷"; // Default avatar
    private static final String APP_URL = "https://play.google.com/store/apps/details?id=com.agpitcodeclub.chorchithyamultiplayer";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
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
        btnSettings = findViewById(R.id.btnSettings);

        setupAvatarSelection();

        btnSettings.setOnClickListener(v -> showThemeSelectionDialog());

        // Logic for Create Room
        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String playerName = etPlayerName.getText().toString();
                if (TextUtils.isEmpty(playerName)) {
                    Toast.makeText(MainActivity.this, R.string.toast_enter_name, Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(MainActivity.this, R.string.toast_enter_name, Toast.LENGTH_SHORT).show();
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
                intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_msg, APP_URL));
                startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
            }
        });
    }

    private void showJoinDialog(String playerName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_enter_room_code);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setTextColor(Color.BLACK);
        input.setHintTextColor(Color.GRAY);
        builder.setView(input);

        builder.setPositiveButton(R.string.btn_join, (dialog, which) -> {
            String roomCode = input.getText().toString().trim();
            if (roomCode.isEmpty()) {
                Toast.makeText(this, R.string.toast_enter_code, Toast.LENGTH_SHORT).show();
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

        builder.setNegativeButton(R.string.btn_cancel, (dialog, which) -> dialog.cancel());
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

    private void showThemeSelectionDialog() {
        String[] options = {getString(R.string.dialog_select_theme), getString(R.string.dialog_select_language)};
        
        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_settings)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showThemeChoice();
                    else showLanguageChoice();
                })
                .show();
    }

    private void showThemeChoice() {
        String[] themes = {
                getString(R.string.theme_default),
                getString(R.string.theme_blue),
                getString(R.string.theme_purple),
                getString(R.string.theme_teal)
        };
        int checkedItem = ThemeUtils.getSelectedTheme(this);

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_select_theme)
                .setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
                    ThemeUtils.setSelectedTheme(MainActivity.this, which);
                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void showLanguageChoice() {
        String[] languages = {"English", "हिन्दी (Hindi)", "मराठी (Marathi)"};
        String[] codes = {"en", "hi", "mr"};
        
        String currentLang = LocaleHelper.getLanguage(this);
        int checkedItem = 0;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(currentLang)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_select_language)
                .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                    LocaleHelper.setLocale(MainActivity.this, codes[which]);
                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}