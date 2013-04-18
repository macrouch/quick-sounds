package com.sleekcoder.quick_sounds;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseProvider {
	private static final String dbName = "quickSoundsDB";
	private static final int dbVersion = 1;
	
	// Sounds Table
	private static final String soundsTable = "Sounds";
	private static final String colSoundID = "ID";
	private static final String colSoundText = "Text";
	private static final String colSoundPath = "Path";
	private static final String colSoundLoop = "Loop";
	
	// Buttons Table
	private static final String buttonsTable = "Buttons";
	private static final String colButtonID = "ID";
	private static final String colButtonPlaceID = "ButtonID";
	private static final String colButtonSoundID = "SoundID";
	
	public class DatabaseHelper extends SQLiteOpenHelper {
		public DatabaseHelper(Context context) {
			super(context, dbName, null, dbVersion);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			// Create the Sounds Table
			db.execSQL("CREATE TABLE " +soundsTable+ "(" 
					+colSoundID+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+colSoundText+ " TEXT, "
					+colSoundPath+ " TEXT, "
					+colSoundLoop+ " INTEGER);");
			
			// Create the Buttons Table
			db.execSQL("CREATE TABLE " +buttonsTable+ "("
					+colButtonID+ " INTEGER PRIMARY KEY AUTOINCREMENT," 
					+colButtonPlaceID+ " INTEGER,"
					+colButtonSoundID+ " INTEGER);");
			
			// create all the buttons		
			for(int i=0; i < 24; i++) {
				//addButton(i, "", "", false);
				String insertSound = "INSERT INTO " +soundsTable+ " VALUES(" +i+ "," +"''"+ "," +"''"+ ", 0);";
				String insertButton = "INSERT INTO " +buttonsTable+ " VALUES(" +i+ "," +i+ "," +i+ ");";
				db.execSQL(insertSound);
				db.execSQL(insertButton);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			//	drop tables
			db.execSQL("DROP TABLE IF EXISTS " + soundsTable+ ";");
			db.execSQL("DROP TABLE IF EXISTS " + buttonsTable+ ";");
			
			// recreate tables
			onCreate(db);
		}
	}
	
	private DatabaseHelper databaseHelper;
	private SQLiteDatabase database;
	private Context context;
	
	public DatabaseProvider(Context c) {
		context = c;
	}
	
	public DatabaseProvider openToRead() throws android.database.SQLException {
		databaseHelper = new DatabaseHelper(context);
		database = databaseHelper.getReadableDatabase();
		return this;
	}
	
	public DatabaseProvider openToWrite() throws android.database.SQLException {
		databaseHelper = new DatabaseHelper(context);
		database = databaseHelper.getWritableDatabase();
		return this;
	}
	
	public void close() {
		databaseHelper.close();
	}
	
	public long addButton(int buttonID, String text, String path, boolean loop) {
		// Add the sound first
		ContentValues contentValues = new ContentValues();
		contentValues.put(colSoundText, text);
		contentValues.put(colSoundPath, path);
		contentValues.put(colSoundLoop, (loop)? 1 : 0); //convert to integer 1 or 0
		long soundID = database.insert(soundsTable, null, contentValues);
		
		// Then add the button
		contentValues = new ContentValues();
		contentValues.put(colButtonPlaceID, buttonID);
		contentValues.put(colButtonSoundID, soundID);
		return database.insert(buttonsTable, null, contentValues);
	}
	
	public int updateButton(int buttonID, String text, String path, boolean loop) {
		// update the sound
		ContentValues contentValues = new ContentValues();
		if (text != null)
			contentValues.put(colSoundText, text);
		if (path != null)
			contentValues.put(colSoundPath, path);
		contentValues.put(colSoundLoop, (loop)? 1 : 0);
		
		int soundID = getSoundID(buttonID);
		database.update(soundsTable, contentValues, colSoundID + " = ?", new String[]{Integer.toString(soundID)});
		
		// then the button
		contentValues = new ContentValues();
		contentValues.put(colButtonPlaceID, buttonID);
		contentValues.put(colButtonSoundID, soundID);
		return database.update(buttonsTable, contentValues, colButtonID + " = ?", new String[]{Integer.toString(buttonID)});
	}
	
	public Cursor getButton(int buttonID) {
		int soundID = getSoundID(buttonID);
		
		String[] columns = new String[]{colSoundText,colSoundPath,colSoundLoop};		
		Cursor cursor = database.query(soundsTable, columns, colSoundID + " = ?", new String[]{Integer.toString(soundID)},null, null, null);
		cursor.moveToFirst();
		return cursor;
	}
	
	// Need method to remove a specific button
	public int removeButton(int buttonID) {
		int soundID = getSoundID(buttonID);
		
		database.delete(soundsTable, colSoundID + " = ?", new String[]{Integer.toString(soundID)});
		return database.delete(buttonsTable, colButtonID, new String[]{Integer.toString(buttonID)});
	}
	
	private int getSoundID(int buttonID) {
		Cursor cursor = database.query(buttonsTable, new String[]{colButtonSoundID}, 
			colButtonPlaceID + "= ?", new String[]{Integer.toString(buttonID)}, null, null, null);
		cursor.moveToFirst();
		int soundID = cursor.getInt(cursor.getColumnIndex(colButtonSoundID));	
		cursor.close();
		return soundID;
	}
	
	public Cursor getAllSounds() {
		Cursor c = database.query(soundsTable, null, null, null, null, null, null);
		c.moveToFirst();
		return c;
	}
	
	public Cursor getAllButtons() {
		Cursor c = database.query(buttonsTable, null, null, null, null, null, null);
		c.moveToFirst();
		return c;
	}
	
	public void removeAllSounds() {
		database.delete(soundsTable, null, null);
	}
	
	public void removeAllButtons() {
		database.delete(buttonsTable, null, null);
	}
	
	public void importSQL(String[] args) {
		String sql = "INSERT INTO " + args[0] + " VALUES (";
		for(int i = 1; i < args.length; i++)
		{
			sql += args[i];
			if (i != args.length-1)
				sql += ",";
		}
		sql += ");";
		database.execSQL(sql);
	}

}
