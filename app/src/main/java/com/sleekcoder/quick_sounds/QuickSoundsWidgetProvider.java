package com.sleekcoder.quick_sounds;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;

public class QuickSoundsWidgetProvider extends AppWidgetProvider {
  //private static final String PREFS_NAME = "prefFile";
  public static String PLAY_1 = "1";
  public static String PLAY_2 = "2";
  public static String PLAY_3 = "3";
  private DatabaseProvider database;

  public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                       int[] appWidgetIds) {

    final int N = appWidgetIds.length;
    //SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);

    // Perform this loop procedure for each App Widget that belongs to this provider
    for (int i=0; i<N; i++) {
      int appWidgetId = appWidgetIds[i];
      Intent openIntent = new Intent(context, QuickSoundsActivity.class);
      PendingIntent openPendingIntent = PendingIntent.getActivity(context, 0, openIntent, 0);

      RemoteViews views = new RemoteViews(context.getPackageName(),R.layout.appwidget);
      views.setOnClickPendingIntent(R.id.imageView1, openPendingIntent);

      // Set up each button to use the onReceive method (broadcast)
      Intent playIntent1 = new Intent(context, QuickSoundsWidgetProvider.class);
      playIntent1.setAction(PLAY_1);
      PendingIntent playPendingIntent1 = PendingIntent.getBroadcast(context, 0, playIntent1, 0);
      views.setOnClickPendingIntent(R.id.button1, playPendingIntent1);
      //views.setTextViewText(R.id.button1, settings.getString("text"+0, "empty"));
      views.setTextViewText(R.id.button1, getSound(0, "Text", context));

      Intent playIntent2 = new Intent(context, QuickSoundsWidgetProvider.class);
      playIntent2.setAction(PLAY_2);
      PendingIntent playPendingIntent2 = PendingIntent.getBroadcast(context, 0, playIntent2, 0);
      views.setOnClickPendingIntent(R.id.button2, playPendingIntent2);
      //views.setTextViewText(R.id.button2, settings.getString("text"+1, "empty"));
      views.setTextViewText(R.id.button2, getSound(1, "Text", context));

      Intent playIntent3 = new Intent(context, QuickSoundsWidgetProvider.class);
      playIntent3.setAction(PLAY_3);
      PendingIntent playPendingIntent3 = PendingIntent.getBroadcast(context, 0, playIntent3, 0);
      views.setOnClickPendingIntent(R.id.button3, playPendingIntent3);
      //views.setTextViewText(R.id.button3, settings.getString("text"+2, "empty"));
      views.setTextViewText(R.id.button3, getSound(2, "Text", context));

      appWidgetManager.updateAppWidget(appWidgetId, views);
    }
  }

  @Override
  public void onReceive(Context context, Intent intent){
    super.onReceive(context, intent);

    //SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);

    if (intent.getAction().equals(PLAY_1)){
      // Get the button's configuration
      //String filepath = settings.getString("sound"+0, "");
      //boolean loop = settings.getBoolean("loop"+0, false);
      String filepath = getSound(0, "Path", context);
      boolean loop = (getSound(0, "Loop", context) == "true")? true : false;

      // Play the first button's sound
      SoundPlayer.playSound(filepath, loop);
    } else if (intent.getAction().equals(PLAY_2)){
      // Get the button's configuration
      //String filepath = settings.getString("sound"+1, "");
      //boolean loop = settings.getBoolean("loop"+1, false);
      String filepath = getSound(1, "Path", context);
      boolean loop = (getSound(1, "Loop", context) == "true")? true : false;

      // Play the button's sound
      SoundPlayer.playSound(filepath, loop);
    } else if (intent.getAction().equals(PLAY_3)){
      // Get the button's configuration
      //String filepath = settings.getString("sound"+2, "");
      //boolean loop = settings.getBoolean("loop"+2, false);
      String filepath = getSound(2, "Path", context);
      boolean loop = (getSound(2, "Loop", context) == "true")? true : false;

      // Play the button's sound
      SoundPlayer.playSound(filepath, loop);
    } else {

    }
  }



  /** This method gets the button property in the SQLite database */
  public String getSound(int buttonID, String property, Context context) {
    database = new DatabaseProvider(context);
    database.openToRead();
    Cursor c = database.getButton(buttonID);
    database.close();
    String value;
    if (property == "Loop") {
      //value = (c.getInt(c.getColumnIndex(property)))>0 ? "true" : "false";
      value = "false"; //looping in the widget is very bad, I dont know how to stop the media player!
    }
    else {
      value = c.getString(c.getColumnIndex(property));
    }
    c.close();
    return value;
  }

  /** This method sets the button values in the SQLite database */
  public void setButton(int buttonID, String text, String path, boolean loop, Context context) {
    database = new DatabaseProvider(context);
    database.openToWrite();
    database.updateButton(buttonID, text, path, loop);
    database.close();
  }

}