package com.hacktothefuture.hermes;

import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Random;


public class MainActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback {
    private static final String TAG = "MainActivity";
    private static final int ZOOM_LEVEL = 20;
    private static final int GEOFENCE_RADIUS_IN_METERS = 40;
    private static final long GEOFENCE_EXPIRATION_DURATION = 180000;
    private static final long LOCATION_POLLING_INTERVAL_IN_MILLIS = 3000;

    GoogleApiClient m_GoogleApiClient;
    GoogleMap m_map;
    Marker m_marker;
    PendingIntent mGeofencePendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_GoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        m_GoogleApiClient.connect();

        MapFragment mf = (MapFragment) getFragmentManager().findFragmentById(R.id.fragment_map);
        mf.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                m_map = googleMap;
            }
        });

        Button leaveMessageButton = (Button) findViewById(R.id.leave_message_button);
        leaveMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeFence();
            }
        });
    }

    private void makeFence() {
        Location location = LocationServices.FusedLocationApi.getLastLocation(m_GoogleApiClient);
        Geofence geofence = new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId("KEY " + new Random().nextInt(2000))
                .setCircularRegion(
                        location.getLatitude(),
                        location.getLongitude(),
                        GEOFENCE_RADIUS_IN_METERS)
                .setExpirationDuration(GEOFENCE_EXPIRATION_DURATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        GeofencingRequest geofenceRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence).build();

        LocationServices.GeofencingApi.addGeofences(
                m_GoogleApiClient,
                geofenceRequest,
                getGeofencePendingIntent()
        ).setResultCallback(this);

        m_map.addMarker(new MarkerOptions()
                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                .title("Current location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (m_GoogleApiClient != null) {
            m_GoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (m_GoogleApiClient != null) {
            m_GoogleApiClient.connect();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "GoogleApiClient connected successfully.");
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(LOCATION_POLLING_INTERVAL_IN_MILLIS);
        LocationServices.FusedLocationApi.requestLocationUpdates(m_GoogleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "GoogleApiClient failed to connect.");

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Lat: " + location.getLatitude() + ", Long: " + location.getLongitude());
        if (m_map != null) {
            LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latlng, ZOOM_LEVEL);
            m_map.moveCamera(update);
            if (m_marker != null) m_marker.remove();
            m_marker = m_map.addMarker(new MarkerOptions()
                    .position(latlng)
                    .title("Current location"));

        }
    }

    @Override
    public void onResult(Result result) {
        Log.i(TAG, "Geofence registered.");
    }
}
