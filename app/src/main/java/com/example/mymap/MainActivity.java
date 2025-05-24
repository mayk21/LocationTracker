package com.example.mymap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LatLng currentLatLng;
    private List<LatLng> favoriteLocations = new ArrayList<>();
    private LatLng selectedLatLng;
    private TextView tvCurrentAddress;
    private TextView tvDistance;
    private DatabaseHelper databaseHelper;
    private boolean isFirstLoad = true; // To handle initial map centering
    private static final int FAVORITES_REQUEST_CODE = 100;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnSaveLocation = findViewById(R.id.btnSaveLocation);
        Button btnCalculateDistance = findViewById(R.id.btnCalculateDistance);
        Button btnViewFavorites = findViewById(R.id.btnViewFavorites);
        tvCurrentAddress = findViewById(R.id.tvCurrentAddress);
        tvDistance = findViewById(R.id.tvDistance);

        btnViewFavorites.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FavoritesActivity.class);
            startActivityForResult(intent, FAVORITES_REQUEST_CODE);
        });

        // Initialize the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize the location client
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        databaseHelper = new DatabaseHelper(this);

        // Set up location request
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        // Save current location as favorite when button is clicked
        btnSaveLocation.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                showSaveLocationDialog(selectedLatLng);
            } else {
                Toast.makeText(MainActivity.this, "Current location not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Calculate the distance between the current location and the selected point
        btnCalculateDistance.setOnClickListener(v -> {
            if (currentLatLng != null && selectedLatLng != null) {
                float distance = calculateDistance(currentLatLng, selectedLatLng);
                tvDistance.setText("Distance to selected location: " + distance + " km");
            } else {
                Toast.makeText(this, "Please select a point on the map", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            startLocationUpdates();
            // Set a long click listener on the map to select a second point
            mMap.setOnMapLongClickListener(latLng -> {
                favoriteLocations.add(latLng);
                mMap.clear(); // Clear previous markers
                mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Point"));
                selectedLatLng = latLng;
            });

            // Only move camera to current location on first load
            if (isFirstLoad && currentLatLng != null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                isFirstLoad = false;
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FAVORITES_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // Handle the selected location from FavoritesActivity
            LatLng selectedLocation = data.getParcelableExtra("selected_location");
            if (selectedLocation != null) {
                mMap.clear(); // Clear previous markers
                mMap.addMarker(new MarkerOptions().position(selectedLocation).title("Selected Location"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));
                getAddress(selectedLocation); // Update address
                selectedLatLng = selectedLocation; // Update selectedLatLng for further use if needed
                if (currentLatLng != null) {
                    float distance = calculateDistance(currentLatLng, selectedLatLng);
                    tvDistance.setText("Distance to selected favorite: " + distance + " km");
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {
                currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                // Update current address
                getAddress(currentLatLng);
            }
        }
    };

    private void showSaveLocationDialog(LatLng latLng) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save Location");

        // Set up the input
        final EditText input = new EditText(this);
        input.setHint("Enter location name");
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String locationName = input.getText().toString();
                if (!locationName.isEmpty()) {
                    saveFavoriteLocation(latLng, locationName);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a name", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void saveFavoriteLocation(LatLng location, String locationName) {
        favoriteLocations.add(location);
        mMap.addMarker(new MarkerOptions().position(location).title(locationName));
        // Update database
        databaseHelper.addFavoriteLocation(location, locationName);
        Toast.makeText(this, "Location saved as favorite", Toast.LENGTH_SHORT).show();
    }

    private void getAddress(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses.size() > 0) {
                Address address = addresses.get(0);
                tvCurrentAddress.setText(address.getAddressLine(0));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private float calculateDistance(LatLng point1, LatLng point2) {
        Location location1 = new Location("");
        location1.setLatitude(point1.latitude);
        location1.setLongitude(point1.longitude);

        Location location2 = new Location("");
        location2.setLatitude(point2.latitude);
        location2.setLongitude(point2.longitude);

        return (location1.distanceTo(location2))/1000;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    startLocationUpdates();
                    // Set a long click listener on the map to select a second point
                    mMap.setOnMapLongClickListener(latLng -> {
                        favoriteLocations.add(latLng);
                        mMap.clear(); // Clear previous markers
                        mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Point"));
                    });
                }
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
