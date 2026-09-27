package com.agpitcodeclub.chorchithyamultiplayer;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;

public class RateAppManager {

    public static void showRateDialogEveryMatch(Activity activity, Runnable onDismiss) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (onDismiss != null) onDismiss.run();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.rate_dialog_title);
        builder.setMessage(R.string.rate_dialog_msg);
        builder.setCancelable(false);

        builder.setPositiveButton(R.string.rate_now, (dialog, which) -> {
            dialog.dismiss();
            openPlayStore(activity);
            if (onDismiss != null) onDismiss.run();
        });

        builder.setNeutralButton(R.string.rate_later, (dialog, which) -> {
            dialog.dismiss();
            if (onDismiss != null) onDismiss.run();
        });

        builder.setNegativeButton(R.string.rate_never, (dialog, which) -> {
            dialog.dismiss();
            if (onDismiss != null) onDismiss.run();
        });

        builder.show();
    }

    public static void openPlayStore(Context context) {
        String packageName = context.getPackageName();
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK |
                    Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}