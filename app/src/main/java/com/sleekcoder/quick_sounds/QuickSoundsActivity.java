package com.sleekcoder.quick_sounds;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.Toast;
import android.widget.ToggleButton;

public class QuickSoundsActivity extends Activity {

  private static final String PREFS_NAME = "prefFile";
  private static final int MENU_ITEM_HELP = 1;
  private static final int MENU_ITEM_EXIT = 2;
  private static final int MENU_ITEM_IMPORT_EXPORT = 3;
  private static final int SELECTION_DIALOG = 0;
  private static final int IMPORT_EXPORT_DIALOG = 1;
  private static final int IMPORT_DIALOG = 2;
  private static final String FIRST_RUN = "1";
  MediaPlayer mp;
  private DatabaseProvider database;
  private static final String colSoundText = "Text";
  private static final String colSoundPath = "Path";
  private static final String colSoundLoop = "Loop";


  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String firstrun = getStringPreferences("firstrun");
    if(firstrun.length() == 0) {
      showWelcome();

      setStringPreferences("firstrun",FIRST_RUN);
    }

    // Are the sounds still in preferences?
    migrateToSQLite();

    // check the app rater
    AppRater.app_launched(this);

    //refresh widget if needed
    Handler handler = new Handler();
    handler.post(new Runnable(){
      public void run() {
        Context context = getApplicationContext();
        ComponentName name = new ComponentName(context, QuickSoundsWidgetProvider.class);
        Intent intent = new Intent(context, QuickSoundsWidgetProvider.class);
        intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(name);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
      }
    });

    mp = new MediaPlayer();

    //find out how big the display is
    Display display = getWindowManager().getDefaultDisplay();
    int width = display.getWidth();
    int height = display.getHeight();

    TableLayout layout = new TableLayout(this);
    layout.setLayoutParams(new TableLayout.LayoutParams());
    layout.setPadding(1, 1, 1, 1);

    for (int i=0; i<= 5; i++) {
      TableRow tr = new TableRow(this);
      tr.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
      tr.setGravity(Gravity.FILL);
      for (int j=0; j<=3; j++) {
        int bId = (i*4) + j;
        Button b = new Button(this);

        //b.setText(getStringPreferences("text"+bId));
        b.setText(getSound(bId, colSoundText));
        b.setWidth(width/4);
        b.setHeight(height/7);

        //final String filepath = getStringPreferences("sound"+bId);
        final String filepath = getSound(bId, colSoundPath);

        //final boolean loopSound = getBooleanPreferences("loop"+bId);
        final boolean loopSound = (getSound(bId, colSoundLoop) == "true") ? true : false;
        final Bundle bundle = new Bundle();
        bundle.putInt("bId", bId);
        b.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {
            if (filepath.length() > 0) {

              //check to see if media player is already playing
              if (mp.isPlaying()) {
                mp.stop();
              } else {
                SoundPlayer.prepareMedia(filepath, loopSound, mp);
                //More than one sound at a time
                //SoundPlayer.playSound(filepath, loopSound);

                mp.start();
              }
            } else { // if button has not been set
              showDialog(SELECTION_DIALOG,bundle);
            }
          }
        });
        b.setOnLongClickListener(new View.OnLongClickListener() {
          public boolean onLongClick(View v) {
            showDialog(SELECTION_DIALOG,bundle);

            return false;
          }
        });

        tr.addView(b);
      } //for
      layout.addView(tr);
    } //for

    super.setContentView(layout);
  }

  /** This method creates the menu options */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(Menu.NONE, MENU_ITEM_HELP, Menu.NONE, R.string.helpOption);
    menu.add(Menu.NONE, MENU_ITEM_IMPORT_EXPORT, Menu.NONE, R.string.importExportOption);
    menu.add(Menu.NONE, MENU_ITEM_EXIT, Menu.NONE, R.string.exitOption);

    return true;
  }

  /** This method handles the menu options */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch(item.getItemId()) {
      case MENU_ITEM_HELP:
        showWelcome();
        return true;
      case MENU_ITEM_EXIT:
        this.finish();
        return true;
      case MENU_ITEM_IMPORT_EXPORT:
        showDialog(IMPORT_EXPORT_DIALOG);
        return true;
      default:
        return false;
    }
  }

  /** This method releases the media player when the sound is completed */
  public void onCompletion(MediaPlayer player) {
    player.release();
  }

  /** This method releases the media player when the app is stopped */
  @Override
  protected void onStop() {
    super.onStop();
    mp.release();
  }

  /** This method prepares dialogs */
  @Override
  protected void onPrepareDialog(int id, final Dialog dialog, Bundle args) {
    super.onPrepareDialog(id, dialog, args);
    switch(id) {
      case SELECTION_DIALOG:
        final int bId = args.getInt("bId");
        getButtonDialog(bId, dialog);

        break;
      case IMPORT_EXPORT_DIALOG:
        getImportExportDialog(dialog);
        break;
    }
  }

  /** This method creates the dialog initially */
  protected Dialog onCreateDialog(int id, Bundle args) {
    Dialog dialog = new Dialog(this);
    switch(id) {
      case SELECTION_DIALOG:
        dialog = getButtonDialog(args.getInt("bId"), dialog);
        break;
      case IMPORT_EXPORT_DIALOG:
        dialog = getImportExportDialog(dialog);
        break;
      case IMPORT_DIALOG:

        break;
      default:
        dialog = null;
    }
    return dialog;
  }

  /** This method sets up the dialog where the users chooses sound and text */
  private Dialog getButtonDialog(final int bId, final Dialog d) {
    d.setContentView(R.layout.set_button_dialog);
    d.setTitle(R.string.buttonDialogTitle);
    EditText text = (EditText)d.findViewById(R.id.editButtonText);

    //text.setText(getStringPreferences("text"+bId));
    text.setText(getSound(bId, colSoundText));

    //handles the select sound button
    Button selectButton = (Button)d.findViewById(R.id.buttonSelectSound);
    selectButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        Intent intent = new Intent();
        intent.setType("audio/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        //only use the Google/Android music choosers
        String type1 = "google";
        String type2 = "android";
        boolean found = false;
        //gets the list of intents that can be loaded
        List<ResolveInfo> resInfo = getPackageManager().queryIntentActivities(intent, 0);
        if (!resInfo.isEmpty())
        {
          for (ResolveInfo info : resInfo)
          {
            if (info.activityInfo.packageName.toLowerCase().contains(type1) ||
                info.activityInfo.name.toLowerCase().contains(type1) ||
                info.activityInfo.packageName.toLowerCase().contains(type2) ||
                info.activityInfo.name.toLowerCase().contains(type2))
            {
              intent.setPackage(info.activityInfo.packageName);
              found = true;
              break;
            }
          }
          if (!found)
            return;
        }

        startActivityForResult(Intent.createChooser(intent, "Select file"), bId);
      }
    });

    //handles the loop toggle button
    ToggleButton loopButton = (ToggleButton)d.findViewById(R.id.loopButton);
    //loopButton.setChecked(getBooleanPreferences("loop"+bId));
    loopButton.setChecked((getSound(bId, colSoundLoop) == "true") ? true : false);
    loopButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        //setBooleanPreferences("loop"+bId,isChecked);
        setButton(bId, null, null, isChecked);
      }
    });

    //handles the finish button
    Button finishButton = (Button)d.findViewById(R.id.finishButton);
    finishButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        //set text preference
        EditText editButtonText = (EditText)d.findViewById(R.id.editButtonText);
        String buttonText = editButtonText.getText().toString();

        //find loop button
        ToggleButton loopButton = (ToggleButton)d.findViewById(R.id.loopButton);
        boolean loop = loopButton.isChecked() ? true : false;

        //setStringPreferences("text"+bId, buttonText);
        setButton(bId, buttonText, null, loop);

        //reload preferences
        onCreate(new Bundle());
        d.cancel();
      }
    });
    return d;
  }

  /** This method sets up the dialog where the user can import/export settings */
  private Dialog getImportExportDialog(final Dialog d) {
    d.setContentView(R.layout.import_export_dialog);
    d.setTitle(R.string.importExportDialogTitle);

    //handles the export button
    Button exportButton = (Button)d.findViewById(R.id.buttonExport);
    exportButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/quick_sounds");
        myDir.mkdirs();
        Date date = new Date();
        DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String fname = "backup-" + df.format(date);
        File file = new File(myDir, fname);

        database = new DatabaseProvider(getApplicationContext());
        database.openToRead();
        Cursor sounds = database.getAllSounds();
        Cursor buttons = database.getAllButtons();
        database.close();

        try {
          FileOutputStream fos = new FileOutputStream(file);
          OutputStreamWriter out = new OutputStreamWriter(fos);

          String soundsString = "";
          for(int i = 0; i < sounds.getCount(); i++) {
            soundsString = String.format("Sounds," +sounds.getInt(sounds.getColumnIndex("ID"))+ ","
                    +"'" +sounds.getString(sounds.getColumnIndex("Text"))+ "',"
                    +"'" +sounds.getString(sounds.getColumnIndex("Path"))+ "',"
                    +sounds.getString(sounds.getColumnIndex("Loop"))+ "%s",
                System.getProperty("line.separator"));
            soundsString = Base64.encodeToString(soundsString.getBytes(), Base64.DEFAULT);
            out.write(soundsString);
            sounds.moveToNext();
          }
          sounds.close();

          String buttonsString = "";
          for(int i=0; i < buttons.getCount(); i++) {
            buttonsString = String.format("Buttons,"+buttons.getInt(buttons.getColumnIndex("ID"))+ ","
                    +buttons.getInt(buttons.getColumnIndex("ButtonID"))+ ","
                    +buttons.getInt(buttons.getColumnIndex("SoundID"))+ "%s",
                System.getProperty("line.separator"));
            buttonsString = Base64.encodeToString(buttonsString.getBytes(), Base64.DEFAULT);
            out.write(buttonsString);
            buttons.moveToNext();
          }
          buttons.close();
          out.flush();
          out.close();

        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }

        //Let the user know everything worked
        Toast.makeText(getApplicationContext(), "Sounds exported successfully!", Toast.LENGTH_SHORT).show();

        d.dismiss();
      }
    });

    // handles the import button
    Button importButton = (Button)d.findViewById(R.id.buttonImport);
    importButton.setOnClickListener(new View.OnClickListener() {

      public void onClick(View v) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/quick_sounds");
        myDir.mkdirs();
        String[] fileList;
        if(myDir.exists()) {
          fileList = myDir.list();
        }
        else {
          fileList = new String[0];
        }

        showImportFiles(fileList, myDir);

        d.dismiss();
      }
    });

    return d;

  }

  /** This method returns the result from the file chooser activity */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if(resultCode != 0) {
      super.onActivityResult(requestCode, resultCode, data);
      int bId = requestCode;

      Uri audioUri = data.getData();
      String filepath = getPath(audioUri);

      //Set sound preferences
      //setStringPreferences("sound"+bId, filepath);
      setButton(bId, null, filepath, false);
    }
  }

  /** This method re-creates the media player when the user brings back up the app */
  @Override
  protected void onResume() {
    mp = new MediaPlayer();
    super.onResume();
  }

  /** This method returns a string path from a given Uri */
  public String getPath(Uri uri) {
    String[] projection = { MediaStore.Images.Media.DATA };
    Cursor cursor = managedQuery(uri, projection, null, null, null);
    int column_index = cursor
        .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
    cursor.moveToFirst();
    return cursor.getString(column_index);
  }

  /** This method gets the preference for the given prefName */
  public String getStringPreferences(String prefName) {
    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    String value = settings.getString(prefName, "");
    return value;
  }
  public boolean getBooleanPreferences(String prefName) {
    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    boolean value = settings.getBoolean(prefName, false);
    return value;
  }

  /** This method gets the button property in the SQLite database */
  public String getSound(int buttonID, String property) {
    database = new DatabaseProvider(getApplicationContext());
    database.openToRead();
    Cursor c = database.getButton(buttonID);
    database.close();
    String value;
    if (property == colSoundLoop) {
      value = (c.getInt(c.getColumnIndex(property)))>0 ? "true" : "false";
    }
    else {
      value = c.getString(c.getColumnIndex(property));
    }
    c.close();
    return value;
  }

  /** This method sets the given prefName with value */
  public void setStringPreferences(String prefName, String value) {
    // We need an Editor object to make preference changes.
    // All objects are from android.context.Context
    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    SharedPreferences.Editor editor = settings.edit();
    editor.putString(prefName, value);
    // Commit the edits!
    editor.commit();
  }
  public void setBooleanPreferences(String prefName, boolean value) {
    // We need an Editor object to make preference changes.
    // All objects are from android.context.Context
    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    SharedPreferences.Editor editor = settings.edit();
    editor.putBoolean(prefName, value);
    // Commit the edits!
    editor.commit();
  }

  /** This method sets the button values in the SQLite database */
  public void setButton(int buttonID, String text, String path, boolean loop) {
    database = new DatabaseProvider(getApplicationContext());
    database.openToWrite();
    database.updateButton(buttonID, text, path, loop);
    database.close();
  }

  /** This method shows the Welcome/Help dialog */
  public void showWelcome() {
    new AlertDialog.Builder(this).setTitle("Welcome").setMessage(R.string.welcome).setNeutralButton("OK", null).show();
  }

  public void showImportFiles(final String[] fileList, final File myDir) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);

    builder.setTitle("Choose file");
    builder.setItems(fileList, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        String file = fileList[which];

        database = new DatabaseProvider(getApplicationContext());
        database.openToWrite();
        database.removeAllButtons();
        database.removeAllSounds();

        Scanner scanner = null;
        try {
          scanner = new Scanner(new File(myDir,file));
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        }
        String text = "";
        while(scanner.hasNext()) {
          text = text + new String(Base64.decode(scanner.nextLine(), Base64.DEFAULT));
          if (text.endsWith(System.getProperty("line.separator")))
          {
            database.importSQL(text.split(","));
            text = "";
          }
        }

        database.close();

        //reload preferences
        onCreate(new Bundle());

        //Let the user know everything worked
        Toast.makeText(getApplicationContext(), "Sounds imported successfully!", Toast.LENGTH_SHORT).show();
      }
    });
    builder.setNeutralButton("Cancel", null);
    builder.show();
  }


  /** This method migrates from SharedPreferences to SQLite */
  private void migrateToSQLite(){
    if (getStringPreferences("migrateToSQL").length() == 0) {
      boolean didMigration = false;
      for(int i = 0; i < 24; i++) {
        String text = getStringPreferences("text"+i);
        String path = getStringPreferences("sound"+i);
        boolean loop = getBooleanPreferences("loop"+i);

        // Is there a preference?
        if (path.length() > 0) {
          setButton(i, text, path, loop);
          didMigration = true;
        }
      }

      // Clear the preferences file
      if (didMigration) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.commit();

        setStringPreferences("firstrun", FIRST_RUN);
        setStringPreferences("migrateToSQL", "1");
      }
    }
  }

}