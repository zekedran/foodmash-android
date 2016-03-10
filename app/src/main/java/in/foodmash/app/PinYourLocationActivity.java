package in.foodmash.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.Bind;
import butterknife.ButterKnife;


/**
 * Created by Zeke on Aug 08 2015.
 */
public class PinYourLocationActivity extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback {

    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;
    @Bind(R.id.proceed) FloatingActionButton proceed;
    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.main_layout) RelativeLayout mainLayout;

    Intent intent;
    LocationManager locationManager;
    LocationListener locationListener;
    JSONObject jsonObject;

    boolean edit = false;
    boolean cart = false;

    MapFragment mapFragment;
    LatLng initialLocation = new LatLng(13.0220501,80.2437108);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_your_location);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        try {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) { e.printStackTrace(); }

        cart = getIntent().getBooleanExtra("cart",false);
        if(getIntent().getBooleanExtra("edit",false)) {
            edit = true;
            try {
                jsonObject = new JSONObject(getIntent().getStringExtra("json"));
                JSONObject geolocationJson = jsonObject.getJSONObject("geolocation");
                initialLocation = new LatLng(geolocationJson.getDouble("latitude"),geolocationJson.getDouble("longitude"));
            } catch (JSONException e) { Snackbar.make(mainLayout, "No location chosen before!", Snackbar.LENGTH_LONG); }
        }

        if(!(isPlayServicesAvailable() && isGpsAvailable()))
            proceed(initialLocation);

        if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions( this, new String[] {  Manifest.permission.ACCESS_FINE_LOCATION  },
                    MY_PERMISSION_ACCESS_FINE_LOCATION );
        }
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if(!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))) enableGpsAlert();

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        proceed.setOnClickListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == MY_PERMISSION_ACCESS_FINE_LOCATION) {
            if(!(grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED))
                proceed(initialLocation);
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.proceed:
                if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED )
                    locationManager.removeUpdates(locationListener);
                intent = new Intent(this, AddAddressActivity.class);
                CameraPosition cameraPosition = mapFragment.getMap().getCameraPosition();
                LatLng latLng = cameraPosition.target;
                proceed(latLng);
                break;
        }
    }

    private void proceed(LatLng latLng) {
        intent = new Intent(this, AddAddressActivity.class);
        intent.putExtra("latitude",latLng.latitude);
        intent.putExtra("longitude",latLng.longitude);
        if(edit) {
            intent.putExtra("edit",true);
            intent.putExtra("json",jsonObject.toString());
        } if(cart) intent.putExtra("cart",true);
        startActivity(intent);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED )
            map.setMyLocationEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, (edit)?16:14));
        locationListener = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                initialLocation = new LatLng(location.getLatitude(),location.getLongitude());
                mapFragment.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation,15));
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {  }
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) { }
        };
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
    }

    private void enableGpsAlert() {
        new AlertDialog.Builder(PinYourLocationActivity.this)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle("GPS Turned Off")
                .setMessage("Enabling GPS helps pinpoint your location on map. Enable GPS from Settings.")
                .setPositiveButton("Enable GPS", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if ( ContextCompat.checkSelfPermission( PinYourLocationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED )
                    locationManager.removeUpdates(locationListener);
            }
        }).show();
    }

    private boolean isPlayServicesAvailable() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        return result == ConnectionResult.SUCCESS;
    }

    private boolean isGpsAvailable() {
        PackageManager pm = this.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }
}
