package com.example.mymap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "FavoritesDB";
    private static final String TABLE_FAVORITES = "favorites";
    private static final String KEY_ID = "id";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_NAME = "name";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_FAVORITES_TABLE = "CREATE TABLE " + TABLE_FAVORITES +
                "(" +
                KEY_ID + " INTEGER PRIMARY KEY," +
                KEY_LATITUDE + " REAL," +
                KEY_LONGITUDE + " REAL," +
                KEY_NAME + " TEXT" +
                ")";
        db.execSQL(CREATE_FAVORITES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
        onCreate(db);
    }

    public void addFavoriteLocation(LatLng latLng, String name) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_LATITUDE, latLng.latitude);
        values.put(KEY_LONGITUDE, latLng.longitude);
        values.put(KEY_NAME, name);

        db.insert(TABLE_FAVORITES, null, values);
        db.close();
    }

    public List<FavoriteLocation> getAllFavoriteLocations() {
        List<FavoriteLocation> favoriteLocations = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_FAVORITES;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                double latitude = cursor.getDouble(cursor.getColumnIndex(KEY_LATITUDE));
                double longitude = cursor.getDouble(cursor.getColumnIndex(KEY_LONGITUDE));
                String name = cursor.getString(cursor.getColumnIndex(KEY_NAME));
                LatLng latLng = new LatLng(latitude, longitude);
                FavoriteLocation favoriteLocation = new FavoriteLocation(latLng, name);
                favoriteLocations.add(favoriteLocation);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return favoriteLocations;
    }

    public static class FavoriteLocation {
        private LatLng latLng;
        private String name;

        public FavoriteLocation(LatLng latLng, String name) {
            this.latLng = latLng;
            this.name = name;
        }

        public LatLng getLatLng() {
            return latLng;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
