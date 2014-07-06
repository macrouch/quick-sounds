package com.sleekcoder.quick_sounds;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class QuickSoundsActivity extends Activity {

    private static final String PREFS_NAME = "prefFile";
    private static final int MENU_ITEM_HELP = 1;
    private static final int MENU_ITEM_EXIT = 2;
    private static final int MENU_ITEM_IMPORT_EXPORT = 3;
    private static final String FIRST_RUN = "1";
    MediaPlayer mp;
    private DatabaseProvider database;
    private static final String colSoundText = "Text";
    private static final String colSoundPath = "Path";
    private static final String colSoundLoop = "Loop";
    DataAccess da = new DataAccess();
    DialogFragment buttonFragment;
    DialogFragment importExportFragment;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String firstrun = getStringPreferences("firstrun");
        if (firstrun.length() == 0) {
            showWelcome();

            setStringPreferences("firstrun", FIRST_RUN);
        }

        // Are the sounds still in preferences?
        migrateToSQLite();

        // check the app rater
        AppRater.app_launched(this);

        // Setup buttons
        setupButtons();
    }

    public void setupButtons() {
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
        DisplayMetrics displaymetrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int width = displaymetrics.widthPixels;
        int height = displaymetrics.heightPixels;

        TableLayout layout = new TableLayout(this);
        layout.setLayoutParams(new TableLayout.LayoutParams());
        layout.setPadding(1, 1, 1, 1);

        for (int i=0; i<= 5; i++) {
            TableRow tr = new TableRow(this);
            tr.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
            tr.setGravity(Gravity.FILL);
            for (int j=0; j<=3; j++) {
                final int bId = (i*4) + j;
                Button b = new Button(this);

                //b.setText(getStringPreferences("text"+bId));
                b.setText(da.getSound(bId, colSoundText, getApplicationContext()));
                b.setWidth(width/4);
                b.setHeight(height/7);

                //final String filepath = getStringPreferences("sound"+bId);
                final String filepath = da.getSound(bId, colSoundPath, getApplicationContext());

                //final boolean loopSound = getBooleanPreferences("loop"+bId);
                final boolean loopSound = (da.getSound(bId, colSoundLoop, getApplicationContext()).equals("true"));
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
                            showButtonDialog(bId);
                        }
                    }
                });
                b.setOnLongClickListener(new View.OnLongClickListener() {
                    public boolean onLongClick(View v) {
                        showButtonDialog(bId);
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
                showImportExportDialog();
                return true;
            default:
                return false;
        }
    }

    private void showButtonDialog(int bId) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        buttonFragment = ButtonDialogFragment.newInstance(bId);
        buttonFragment.show(ft, "dialog");
    }

    public void dismissButtonDialog() {
        buttonFragment.dismiss();
    }

    private void showImportExportDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        importExportFragment = new ImportExportDialogFragment();
        importExportFragment.show(ft, "dialog");
    }

    public void dismissImportExportDialog() {
        importExportFragment.dismiss();
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

    /** This method returns the result from the file chooser activity */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != 0) {
            super.onActivityResult(requestCode, resultCode, data);

            Uri audioUri = data.getData();
            String filepath = getPath(audioUri);

            //Set sound preferences
            da.setButton(requestCode, null, filepath, false, getApplicationContext());
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
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    /** This method gets the preference for the given prefName */
    public String getStringPreferences(String prefName) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        return settings.getString(prefName, "");
    }
    public boolean getBooleanPreferences(String prefName) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        return settings.getBoolean(prefName, false);
    }


    /** This method sets the given prefName with value */
    public void setStringPreferences(String prefName, String value) {
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(prefName, value);
        // Commit the edits!
        editor.apply();
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
                setupButtons();

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
                    da.setButton(i, text, path, loop, getApplicationContext());
                    didMigration = true;
                }
            }

            // Clear the preferences file
            if (didMigration) {
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.clear();
                editor.apply();

                setStringPreferences("firstrun", FIRST_RUN);
                setStringPreferences("migrateToSQL", "1");
            }
        }
    }

}