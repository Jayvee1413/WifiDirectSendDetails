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
public class DataDAO extends SQLiteOpenHelper {

    // Database version
    private static final int DATABASE_VERSION = 6;
    // Database Name
    private static final String DATABASE_NAME = "DataDB";


    // Data table name
    private static final String TABLE_DATA = "data";

    // Data Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_NUMBER = "number";
    private static final String KEY_NAME = "name";
    private static final String KEY_AGE = "age";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_IMAGE = "image";
    private static final String KEY_STATUS = "status";

    private static final String[] COLUMNS = {KEY_ID, KEY_NUMBER, KEY_NAME, KEY_AGE, KEY_ADDRESS, KEY_MESSAGE, KEY_LATITUDE, KEY_LONGITUDE, KEY_IMAGE, KEY_STATUS};

    public DataDAO(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        // SQL statement to create data table
        String CREATE_DATA_TABLE = "CREATE TABLE data ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "number VARCHAR(60), " +
                "name VARCHAR(60), " +
                "age INT(3), " +
                "address VARCHAR(100), " +
                "message VARCHAR(200)," +
                "latitude VARCHAR(20)," +
                "longitude VARCHAR(20)," +
                "image BLOB," +
                "status VARCHAR(10) DEFAULT 'QUEUED')";

        //create data table
        db.execSQL(CREATE_DATA_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        //Drop older data table if existing
        db.execSQL("DROP TABLE IF EXISTS data");

        // create fresh data table
        this.onCreate(db);
    }

    public void addData(Data data){
        Log.d("addData", data.toString());

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(KEY_NUMBER, data.getNumber());
        values.put(KEY_NAME, data.getName());
        values.put(KEY_AGE, data.getAge());
        values.put(KEY_ADDRESS, data.getAddress());
        values.put(KEY_MESSAGE, data.getMessage());
        values.put(KEY_LONGITUDE, data.getLongitude());
        values.put(KEY_LATITUDE, data.getLatitude());
        values.put(KEY_IMAGE, data.getImage());

        // 3. insert
        db.insert(TABLE_DATA, null, values);

        // 4. close
        db.close();
    }

    public Data getData(int id){

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_DATA, COLUMNS, " id = ?", new String[]{ String.valueOf(id)}, null, null, null, null);

        if(cursor != null)
            cursor.moveToFirst();

        Data data = new Data();
        data.setId(Integer.parseInt(cursor.getString(0)));
        data.setNumber(cursor.getString(1));
        data.setName(cursor.getString(2));
        data.setAge(cursor.getString(3));
        data.setAddress(cursor.getString(4));
        data.setMessage(cursor.getString(5));
        data.setLatitude(cursor.getDouble(6));
        data.setLongitude(cursor.getDouble(7));
        data.setImage(cursor.getString(8));
        data.setStatus(cursor.getString(9));

        Log.d("getData("+id+")", data.toString());

        return data;
    }


    public List<Data> getAllData() {
        List<Data> data_list = new ArrayList<Data>();

        // 1. build the query
        String query = "SELECT  * FROM " + TABLE_DATA;

        // 2. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        // 3. go over each row, build Data and add it to list
        Data data = null;
        if (cursor.moveToFirst()) {
            do {
                data = new Data();
                data.setId(Integer.parseInt(cursor.getString(0)));
                data.setName(cursor.getString(1));
                data.setNumber(cursor.getString(2));
                data.setAge(cursor.getString(3));
                data.setAddress(cursor.getString(4));
                data.setMessage(cursor.getString(5));
                data.setLatitude(cursor.getDouble(6));
                data.setLongitude(cursor.getDouble(7));
                data.setImage(cursor.getString(8));
                data.setStatus(cursor.getString(9));

                // Add Data to Data
                data_list.add(data);
            } while (cursor.moveToNext());
        }

        Log.d("getAllData()", data_list.toString());

        // return data_list
        return data_list;
    }

    public int updateData(Data data) {

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, data.getName());
        values.put(KEY_NUMBER, data.getNumber());
        values.put(KEY_AGE, data.getAge());
        values.put(KEY_ADDRESS, data.getAddress());
        values.put(KEY_MESSAGE, data.getMessage());
        values.put(KEY_LONGITUDE, data.getLongitude());
        values.put(KEY_LATITUDE, data.getLatitude());
        values.put(KEY_IMAGE, data.getImage());
        values.put(KEY_STATUS, data.getStatus());

        // 3. updating row
        int i = db.update(TABLE_DATA, //table
                values, // column/value
                KEY_ID+" = ?", // selections
                new String[] { String.valueOf(data.getId()) }); //selection args

        // 4. close
        db.close();

        return i;

    }

    public void deleteData(Data data) {

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. delete
        db.delete(TABLE_DATA, //table name
                KEY_ID+" = ?",  // selections
                new String[] { String.valueOf(data.getId()) }); //selections args

        // 3. close
        db.close();

        //log
        Log.d("deleteData", data.toString());

    }
}

