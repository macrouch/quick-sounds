package com.sleekcoder.quick_sounds;

import android.content.Context;
import android.database.Cursor;


public class DataAccess {
    private DatabaseProvider database;
    private static final String colSoundLoop = "Loop";

    /** This method sets the button values in the SQLite database */
    public void setButton(int buttonID, String text, String path, boolean loop, Context context) {
        database = new DatabaseProvider(context);
        database.openToWrite();
        database.updateButton(buttonID, text, path, loop);
        database.close();
    }

    /** This method gets the button property in the SQLite database */
    public String getSound(int buttonID, String property, Context context) {
        database = new DatabaseProvider(context);
        database.openToRead();
        Cursor c = database.getButton(buttonID);
        database.close();
        String value;
        if (property.equals(colSoundLoop)) {
            value = (c.getInt(c.getColumnIndex(property)))>0 ? "true" : "false";
        }
        else {
            value = c.getString(c.getColumnIndex(property));
        }
        c.close();
        return value;
    }
}
