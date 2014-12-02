package com.example.android.wifidirect;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vincentsantos on 11/10/14.
 */
public class FileDataDAO extends SQLiteOpenHelper {

    // Database version
    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "FileDB";


    // Data table name
    private static final String TABLE_DATA = "file";

    // Data Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_STATUS = "status";

    private static final String[] COLUMNS = {KEY_NAME,  KEY_STATUS};

    public FileDataDAO(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        // SQL statement to create data table
        String CREATE_FILE_TABLE = "CREATE TABLE file ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name VARCHAR(60), " +
                "status VARCHAR(10) DEFAULT 'QUEUED')";

        //create data table
        db.execSQL(CREATE_FILE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        //Drop older data table if existing
        db.execSQL("DROP TABLE IF EXISTS file");

        // create fresh data table
        this.onCreate(db);
    }

    public void addData(FileData fileData){
        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, fileData.getName());

        // 3. insert
        db.insert(TABLE_DATA, null, values);

        // 4. close
        db.close();
    }

    public FileData getFile(int id){

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_DATA, COLUMNS, " id = ?", new String[]{ String.valueOf(id)}, null, null, null, null);

        if(cursor != null)
            cursor.moveToFirst();

        FileData fileData = new FileData();
        fileData.setId(Integer.parseInt(cursor.getString(0)));
        fileData.setName(cursor.getString(1));
        fileData.setStatus(cursor.getString(2));
        Log.d("getData("+id+")", fileData.toString());

        return fileData;
    }


    public List<FileData> getAllFiles() {
        List<FileData> file_Data_list = new ArrayList<FileData>();

        // 1. build the query
        String query = "SELECT  * FROM " + TABLE_DATA;

        // 2. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        // 3. go over each row, build Data and add it to list
        FileData fileData = null;
        if (cursor.moveToFirst()) {
            do {
                fileData = new FileData();
                fileData.setId(Integer.parseInt(cursor.getString(0)));
                fileData.setName(cursor.getString(1));
                fileData.setStatus(cursor.getString(2));

                // Add Data to Data
                file_Data_list.add(fileData);
            } while (cursor.moveToNext());
        }

        Log.d(HybridMANETDTN.TAG, file_Data_list.toString());

        // return data_list
        return file_Data_list;
    }

    public int updateFile(FileData fileData) {

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, fileData.getName());
        values.put(KEY_STATUS, fileData.getStatus());

        // 3. updating row
        int i = db.update(TABLE_DATA, //table
                values, // column/value
                KEY_ID+" = ?", // selections
                new String[] { String.valueOf(fileData.getId()) }); //selection args

        // 4. close
        db.close();

        return i;

    }

    public void deleteFile(FileData fileData) {

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. delete
        db.delete(TABLE_DATA, //table name
                KEY_ID+" = ?",  // selections
                new String[] { String.valueOf(fileData.getId()) }); //selections args

        // 3. close
        db.close();

        //log
        Log.d(HybridMANETDTN.TAG, fileData.toString());

    }
}

