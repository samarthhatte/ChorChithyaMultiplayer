package com.agpitcodeclub.chorchithyamultiplayer;

import android.graphics.Color;

public enum GameRole {
    RAJA("raja", "King", "👑", "1000 PTS", "Rule the Kingdom", R.drawable.bg_card_raja, Color.parseColor("#FBBF24")),
    RANI("rani", "Queen", "👸", "900 PTS", "Royal Majesty", R.drawable.bg_card_rani, Color.parseColor("#F472B6")),
    MANTRI("mantri", "Minister", "📜", "800 PTS", "The Royal Advisor", R.drawable.bg_card_mantri, Color.parseColor("#34D399")),
    SENAPATI("senapati", "Commander", "⚔️", "500 PTS", "Defender of the Realm", R.drawable.bg_card_senapati, Color.parseColor("#818CF8")),
    SIPAHI("sipahi", "Police", "👮‍♂️", "100 PTS", "Catch the Thief", R.drawable.bg_card_sipahi, Color.parseColor("#38BDF8")),
    CHOR("chor", "Thief", "🥷", "100 PTS", "Stay Hidden & Survive", R.drawable.bg_card_chor, Color.parseColor("#EF4444")),
    PRAJA("praja", "Citizen", "👤", "0 PTS", "Loyal Subject", R.drawable.bg_card_praja, Color.parseColor("#94A3B8"));

    private final String key;
    private final String englishTitle;
    private final String icon;
    private final String pointsText;
    private final String subtitle;
    private final int backgroundRes;
    private final int strokeColor;

    GameRole(String key, String englishTitle, String icon, String pointsText, String subtitle, int backgroundRes, int strokeColor) {
        this.key = key;
        this.englishTitle = englishTitle;
        this.icon = icon;
        this.pointsText = pointsText;
        this.subtitle = subtitle;
        this.backgroundRes = backgroundRes;
        this.strokeColor = strokeColor;
    }

    public String getKey() { return key; }
    public String getEnglishTitle() { return englishTitle; }
    public String getIcon() { return icon; }
    public String getPointsText() { return pointsText; }
    public String getSubtitle() { return subtitle; }
    public int getBackgroundRes() { return backgroundRes; }
    public int getStrokeColor() { return strokeColor; }

    public static GameRole fromKey(String key) {
        if (key == null) return PRAJA;
        for (GameRole role : values()) {
            if (role.key.equalsIgnoreCase(key)) {
                return role;
            }
        }
        return PRAJA;
    }
}