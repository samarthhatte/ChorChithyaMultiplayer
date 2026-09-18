package com.agpitcodeclub.chorchithyamultiplayer;// Check your own package name here

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.messaging.BuildConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static final String CHANNEL_ID = "special_events_channel";
    public static final String CHANNEL_NAME = "Special Events & Updates";
    private static final String NOTIF_TOPIC = "events_and_updates";

    EditText etPlayerName;
    Button btnCreate, btnJoin, btnInvite;
    android.widget.ImageButton btnSettings;
    String selectedAvatar = "🥷"; // Default avatar
    private static final String APP_URL = "https://play.google.com/store/apps/details?id=com.agpitcodeclub.chorchithyamultiplayer";

    private static final String PREFS_NAME = "player_prefs";
    private static final String KEY_PLAYER_NAME = "saved_player_name";

    private void savePlayerName(String name) {
        if (!TextUtils.isEmpty(name)) {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(KEY_PLAYER_NAME, name)
                    .apply();
        }
    }

    private String getSavedPlayerName() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_PLAYER_NAME, "");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    // 1. Declare Permission Request Launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notifications disabled. You might miss events!", Toast.LENGTH_SHORT).show();
                }
            });

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

        // 1. Create Notification Channel
        createNotificationChannel();

        // 2. Fetch Remote Config Kill-Switch and Handle Subscription
        setupRemoteConfigAndNotifications();
        // 2. Subscribe to Topic for Broadcasts
        FirebaseMessaging.getInstance().subscribeToTopic("events_and_updates");
        // 2. Check and request notification permission
//        checkNotificationPermission();

        etPlayerName = findViewById(R.id.etPlayerName);
        btnCreate = findViewById(R.id.btnCreateRoom);
        btnJoin = findViewById(R.id.btnJoinRoom);
        btnInvite = findViewById(R.id.btnInviteFriends);
        btnSettings = findViewById(R.id.btnSettings);

        // Pre-fill saved player name if available
        String savedName = getSavedPlayerName();
        if (!savedName.isEmpty() && etPlayerName.getText().toString().trim().isEmpty()) {
            etPlayerName.setText(savedName);
        }

        setupAvatarSelection();

        btnSettings.setOnClickListener(v -> showThemeSelectionDialog());

        // Logic for Create Room
        btnCreate.setOnClickListener(v -> {
            String playerName = etPlayerName.getText().toString().trim();
            if (TextUtils.isEmpty(playerName)) {
                Toast.makeText(MainActivity.this, R.string.toast_enter_name, Toast.LENGTH_SHORT).show();
            } else {
                savePlayerName(playerName);
                // Start the Room Activity - Host Mode
                Intent intent = new Intent(MainActivity.this, RoomActivity.class);
                intent.putExtra("playerName", playerName);
                intent.putExtra("avatar", selectedAvatar);
                intent.putExtra("mode", "host");
                startActivity(intent);
            }
        });

        // Logic for Join Room
        btnJoin.setOnClickListener(v -> {
            String playerName = etPlayerName.getText().toString().trim();
            if (TextUtils.isEmpty(playerName)) {
                Toast.makeText(MainActivity.this, R.string.toast_enter_name, Toast.LENGTH_SHORT).show();
            } else {
                savePlayerName(playerName);
                showJoinDialog(playerName, "");
            }
        });

        // Logic for Invite Friends
        btnInvite.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_msg, APP_URL));
            startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
        });

        // Handle Deep Link if app opened via shareable link
        handleDeepLink(getIntent());
    }

    private void setupRemoteConfigAndNotifications() {
        FirebaseRemoteConfig mRemoteConfig = FirebaseRemoteConfig.getInstance();

        // In development/testing, set interval to 0 for instant testing.
        // In production, 3600 (1 hour) is recommended.
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(BuildConfig.DEBUG ? 0 : 3600)
                .build();
        mRemoteConfig.setConfigSettingsAsync(configSettings);

        // Fallback default value if device is offline or first launch
        Map<String, Object> defaultMap = new HashMap<>();
        defaultMap.put("notifications_enabled", true);
        mRemoteConfig.setDefaultsAsync(defaultMap);

        mRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, task -> {
                    // Read developer flag from Firebase Console
                    boolean isEnabledByDev = mRemoteConfig.getBoolean("notifications_enabled");

                    // Read user preference from local settings
                    boolean isEnabledByUser = NotificationUtils.isNotificationsEnabled(this);

                    if (isEnabledByDev && isEnabledByUser) {
                        // Subscribe user to broadcast
                        FirebaseMessaging.getInstance().subscribeToTopic(NOTIF_TOPIC);
                        checkNotificationPermission();
                    } else {
                        // Unsubscribe user if developer killed it OR user turned it OFF
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(NOTIF_TOPIC);
                    }
                });
    }

    // --- PERMISSION CHECK METHOD ---
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // If permission is not granted, launch the system prompt
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String description = "Notifications for game events, tournaments, and updates";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void handleDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) return;

        Uri data = intent.getData();
        String extractedRoomCode = null;

        if (data.isHierarchical()) {
            extractedRoomCode = data.getQueryParameter("roomCode");
            if (TextUtils.isEmpty(extractedRoomCode)) {
                extractedRoomCode = data.getQueryParameter("code");
            }
        }

        if (TextUtils.isEmpty(extractedRoomCode)) {
            List<String> pathSegments = data.getPathSegments();
            if (pathSegments != null && !pathSegments.isEmpty()) {
                String last = pathSegments.get(pathSegments.size() - 1);
                if (last != null && last.matches("\\d+")) {
                    extractedRoomCode = last;
                }
            }
        }

        if (!TextUtils.isEmpty(extractedRoomCode)) {
            final String codeToJoin = extractedRoomCode;
            // Clear data intent to avoid re-triggering on rotation/recreation
            intent.setData(null);

            String currentInput = etPlayerName.getText().toString().trim();
            String savedName = getSavedPlayerName();
            String nameToUse = !currentInput.isEmpty() ? currentInput : savedName;

            if (!TextUtils.isEmpty(nameToUse)) {
                if (currentInput.isEmpty()) {
                    etPlayerName.setText(nameToUse);
                }
                savePlayerName(nameToUse);
                Toast.makeText(this, getString(R.string.toast_joining_room, codeToJoin), Toast.LENGTH_SHORT).show();

                Intent roomIntent = new Intent(MainActivity.this, RoomActivity.class);
                roomIntent.putExtra("playerName", nameToUse);
                roomIntent.putExtra("avatar", selectedAvatar);
                roomIntent.putExtra("mode", "join");
                roomIntent.putExtra("roomCode", codeToJoin);
                startActivity(roomIntent);
            } else {
                // Name missing, show join dialog with code prefilled
                showJoinDialog("", codeToJoin);
            }
        }
    }

    private void showJoinDialog(String playerName, String defaultRoomCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_enter_room_code);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (!TextUtils.isEmpty(defaultRoomCode)) {
            input.setText(defaultRoomCode);
        }
        input.setTextColor(Color.BLACK);
        input.setHintTextColor(Color.GRAY);
        builder.setView(input);

        builder.setPositiveButton(R.string.btn_join, (dialog, which) -> {
            String roomCode = input.getText().toString().trim();
            String pName = etPlayerName.getText().toString().trim();
            if (TextUtils.isEmpty(pName)) {
                pName = playerName;
            }
            if (TextUtils.isEmpty(pName)) {
                Toast.makeText(this, R.string.toast_enter_name, Toast.LENGTH_SHORT).show();
                return;
            }
            if (roomCode.isEmpty()) {
                Toast.makeText(this, R.string.toast_enter_code, Toast.LENGTH_SHORT).show();
                return;
            }
            savePlayerName(pName);
            // Start RoomActivity with the code
            Intent intent = new Intent(MainActivity.this, RoomActivity.class);
            intent.putExtra("playerName", pName);
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
        String[] options = {getString(R.string.dialog_select_theme), getString(R.string.dialog_select_language), getString(R.string.btn_rate_app)};
        
        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_settings)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showThemeChoice();
                    else if (which == 1) showLanguageChoice();
                    else RateAppManager.openPlayStore(this);
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