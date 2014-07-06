package com.sleekcoder.quick_sounds;

import android.app.DialogFragment;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class ImportExportDialogFragment extends DialogFragment {
    private DatabaseProvider database;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        final View view = inflater.inflate(R.layout.import_export_dialog, container);

        getDialog().setTitle(R.string.importExportDialogTitle);

        //handles the export button
        Button exportButton = (Button)view.findViewById(R.id.buttonExport);
        exportButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String root = Environment.getExternalStorageDirectory().toString();
                File myDir = new File(root + "/quick_sounds");
                myDir.mkdirs();
                Date date = new Date();
                DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
                String fname = "backup-" + df.format(date);
                File file = new File(myDir, fname);

                database = new DatabaseProvider(view.getContext());
                database.openToRead();
                Cursor sounds = database.getAllSounds();
                Cursor buttons = database.getAllButtons();
                database.close();

                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    OutputStreamWriter out = new OutputStreamWriter(fos);

                    String soundsString;
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

                    String buttonsString;
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
                Toast.makeText(view.getContext(), "Sounds exported successfully!", Toast.LENGTH_SHORT).show();

                ((QuickSoundsActivity)getActivity()).dismissImportExportDialog();
            }
        });

        // handles the import button
        Button importButton = (Button)view.findViewById(R.id.buttonImport);
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

                ((QuickSoundsActivity)getActivity()).showImportFiles(fileList, myDir);
                ((QuickSoundsActivity)getActivity()).dismissImportExportDialog();
            }
        });

        return view;
    }
}
