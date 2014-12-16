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
public class PacketDataDAO extends SQLiteOpenHelper {

    // Database version
    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "PacketDB";


    // Data table name
    private static final String TABLE_DATA = "packet";

    // Data Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_FILE_NAME = "file_name";
    private static final String KEY_PACKET_NO = "packet_no";
    private static final String KEY_PACKET = "packet_no";
    private static final String KEY_TOTAL_PACKET_CNT = "total_packet_cnt";
    private static final String KEY_STATUS = "status";
    private static final String KEY_PEER_STATUS = "status";

    private static final String[] COLUMNS = {KEY_ID, KEY_FILE_NAME, KEY_PACKET_NO, KEY_TOTAL_PACKET_CNT, KEY_PACKET, KEY_STATUS, KEY_PEER_STATUS};

    public PacketDataDAO(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        // SQL statement to create data table
        String CREATE_FILE_TABLE = "CREATE TABLE packet ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "file_name VARCHAR(60), " +
                "packet_no int(11), " +
                "total_packet_cnt int(11), " +
                "packet varchar(200), " +
                "status VARCHAR(10) DEFAULT 'QUEUED', " +
                "peer_status VARCHAR(10) DEFAULT 'QUEUED')";
        String CREATE_UNIQUE_INDEX = "CREATE UNIQUE INDEX packet_index on packet (file_name, packet_no)";
        //create data table
        db.execSQL(CREATE_FILE_TABLE);
        db.execSQL(CREATE_UNIQUE_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        //Drop older data table if existing
        db.execSQL("DROP TABLE IF EXISTS packet");

        // create fresh data table
        this.onCreate(db);
    }

    public void addData(PacketData packetData){
        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(KEY_FILE_NAME, packetData.getFile_name());
        values.put(KEY_PACKET_NO, packetData.getPacket_no());
        values.put(KEY_TOTAL_PACKET_CNT, packetData.getTotal_packet_cnt());
        values.put(KEY_PACKET, packetData.getPacket());

        // 3. insert
        db.insert(TABLE_DATA, null, values);

        // 4. close
        db.close();
    }

    public FileData getPacket(int id){

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


    public List<PacketData> getAllPackets(String fileName) {
        List<PacketData> packetDataList = new ArrayList<PacketData>();

        // 1. build the query
        String query = "SELECT  * FROM " + TABLE_DATA + " WHERE " + KEY_FILE_NAME + " = '" + fileName + "'";

        // 2. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        // 3. go over each row, build Data and add it to list
        PacketData packetData = null;
        if (cursor.moveToFirst()) {
            do {
                packetData = new PacketData();
                packetData.setId(Integer.parseInt(cursor.getString(0)));
                packetData.setFile_name(cursor.getString(cursor.getColumnIndex(KEY_FILE_NAME)));
                packetData.setPacket(cursor.getString(cursor.getColumnIndex(KEY_PACKET)));
                packetData.setStatus(cursor.getString(cursor.getColumnIndex(KEY_STATUS)));
                packetData.setPacket_no(Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_PACKET_NO))));
                packetData.setTotal_packet_cnt(Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_TOTAL_PACKET_CNT))));


                // Add Data to Data
                packetDataList.add(packetData);
            } while (cursor.moveToNext());
        }

        Log.d(HybridMANETDTN.TAG, packetDataList.toString());

        // return data_list
        return packetDataList;
    }

    public List<PacketData> getAllPackets(int limit) {
        List<PacketData> packetDataList = new ArrayList<PacketData>();

        // 1. build the query
        String query = "SELECT  * FROM " + TABLE_DATA + " LIMIT " + limit;

        // 2. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        // 3. go over each row, build Data and add it to list
        PacketData packetData = null;
        if (cursor.moveToFirst()) {
            do {
                packetData = new PacketData();
                packetData.setId(Integer.parseInt(cursor.getString(0)));
                packetData.setFile_name(cursor.getString(cursor.getColumnIndex(KEY_FILE_NAME)));
                packetData.setPacket(cursor.getString(cursor.getColumnIndex(KEY_PACKET)));
                packetData.setStatus(cursor.getString(cursor.getColumnIndex(KEY_STATUS)));
                packetData.setPacket_no(Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_PACKET_NO))));
                packetData.setTotal_packet_cnt(Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_TOTAL_PACKET_CNT))));


                // Add Data to Data
                packetDataList.add(packetData);
            } while (cursor.moveToNext());
        }

        Log.d(HybridMANETDTN.TAG, packetDataList.toString());

        // return data_list
        return packetDataList;
    }

    public int updatePacket(PacketData packetData) {

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(KEY_FILE_NAME, packetData.getFile_name());
        values.put(KEY_PACKET, packetData.getPacket());
        values.put(KEY_PACKET_NO, packetData.getPacket_no());
        values.put(KEY_TOTAL_PACKET_CNT, packetData.getTotal_packet_cnt());
        values.put(KEY_STATUS, packetData.getStatus());
        values.put(KEY_PEER_STATUS, packetData.getPeer_status());

        // 3. updating row
        int i = db.update(TABLE_DATA, //table
                values, // column/value
                KEY_ID+" = ?", // selections
                new String[] { String.valueOf(packetData.getId()) }); //selection args

        // 4. close
        db.close();

        return i;

    }

    public void deletePacket(PacketData packetData) {

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. delete
        db.delete(TABLE_DATA, //table name
                KEY_ID+" = ?",  // selections
                new String[] { String.valueOf(packetData.getId()) }); //selections args

        // 3. close
        db.close();

        //log
        Log.d(HybridMANETDTN.TAG, "DELETE: " + packetData.toString());

    }
}

