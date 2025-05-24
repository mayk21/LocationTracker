package com.example.mymap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private ListView listViewFavorites;
    private ArrayAdapter<DatabaseHelper.FavoriteLocation> adapter;
    private List<DatabaseHelper.FavoriteLocation> favoriteLocations;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        listViewFavorites = findViewById(R.id.listViewFavorites);
        databaseHelper = new DatabaseHelper(this);
        favoriteLocations = databaseHelper.getAllFavoriteLocations();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, favoriteLocations);

        listViewFavorites.setAdapter(adapter);

        listViewFavorites.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DatabaseHelper.FavoriteLocation selectedLocation = favoriteLocations.get(position);
                LatLng latLng = selectedLocation.getLatLng();
                Intent intent = new Intent();
                intent.putExtra("selected_location", latLng);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }
}
