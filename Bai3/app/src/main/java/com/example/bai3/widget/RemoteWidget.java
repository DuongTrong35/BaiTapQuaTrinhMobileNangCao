package com.example.bai3.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.bai3.MainActivity;
import com.example.bai3.R;

/**
 * Home screen widget providing quick access to power, volume, and mute controls.
 * Clicking any button opens the main app for actual command execution.
 */
public class RemoteWidget extends AppWidgetProvider {

    private static final String ACTION_POWER = "com.com.example.bai3.widget.ACTION_POWER";
    private static final String ACTION_VOL_UP = "com.com.example.bai3.widget.ACTION_VOL_UP";
    private static final String ACTION_VOL_DOWN = "com.com.example.bai3.widget.ACTION_VOL_DOWN";
    private static final String ACTION_MUTE = "com.com.example.bai3.widget.ACTION_MUTE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_remote);

        // Set click intents — open main app
        views.setOnClickPendingIntent(R.id.widget_btn_power,
                createPendingIntent(context, ACTION_POWER));
        views.setOnClickPendingIntent(R.id.widget_btn_vol_up,
                createPendingIntent(context, ACTION_VOL_UP));
        views.setOnClickPendingIntent(R.id.widget_btn_vol_down,
                createPendingIntent(context, ACTION_VOL_DOWN));
        views.setOnClickPendingIntent(R.id.widget_btn_mute,
                createPendingIntent(context, ACTION_MUTE));

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private PendingIntent createPendingIntent(Context context, String action) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(action);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, action.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onEnabled(Context context) {
        // Widget first added to home screen
    }

    @Override
    public void onDisabled(Context context) {
        // Last widget removed from home screen
    }
}
