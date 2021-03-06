package com.hacktothefuture.hermes;

import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;





public class MainActivity extends ActionBarActivity implements
        Callback<List<Board>>, NewMessageDialogFragment.NewMessageDialogListener {
    private static final String TAG = "MainActivity";
    private static final int ZOOM_LEVEL = 19;
    private static final int GEOFENCE_RADIUS_IN_METERS = 15;
    public static final String EXTRA_BOARD_ID = "BOARD_ID";

    TextView m_debugTextView;

    LocationCheckService m_service;
    boolean m_bound;
    boolean m_isMapTouch = false;
    LatLng m_lastMapTouch;
    LatLng m_lastLocation;

    GoogleMap m_map;
    List<Marker> m_markers = new ArrayList<>();
    RestAdapter m_restAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_restAdapter = new RestAdapter.Builder()
                .setEndpoint(AppClient.API_URL)
                .build();

        MapFragment mf = (MapFragment) getFragmentManager().findFragmentById(R.id.fragment_map);
        mf.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                m_map = googleMap;
                m_map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latlng) {
                        m_lastMapTouch = latlng;
                        m_isMapTouch = true;
                        showNewMessageDialog();
                    }
                });
                m_map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker m) {
                        if(!m.getTitle().equals("Current location")) {
                            Intent notificationIntent = new Intent(getApplicationContext(), MessageViewActivity.class);
                            notificationIntent.putExtra(EXTRA_BOARD_ID, m.getTitle());
                            startActivity(notificationIntent);
                        }
                        return false;
                    }
                });
            }
        });


        Button leaveMessageButton = (Button) findViewById(R.id.leave_message_button);
        leaveMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewMessageDialog();
            }
        });



//        Button getMessagesButton = (Button) findViewById(R.id.get_messages_button);
//        getMessagesButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                getMessages(getLastLocation());
//            }
//        });



    }

    private void getMessages(LatLng latlng) {
        AppClient.MyApp client = m_restAdapter.create(AppClient.MyApp.class);

        client.getBoards((float) latlng.latitude, (float) latlng.longitude, this);
    }

    private void leaveMessage(final LatLng latlng, String message) {
        Log.i(TAG, "Adding wall at lat= " + latlng.latitude + ", long= " + latlng.longitude);

        AppClient.MyApp client = m_restAdapter.create(AppClient.MyApp.class);

        JsonLocation loc = new JsonLocation();
        List<Float> coords = new ArrayList<>();
        coords.add((float) latlng.latitude);
        coords.add((float) latlng.longitude);
        loc.setCoordinates(coords);

        CreateBoardBundle bundle = new CreateBoardBundle();
        bundle.setContent(message);
        bundle.setLocation(loc);

        client.createBoard(bundle, new Callback<String>() {
            @Override
            public void success(String id, Response response) {
                Log.i(TAG, "Retrofit POST successful. Received id " + id);
                Board board = new Board();
                board.set_id(id);
                board.set_latlng(latlng);

                m_markers.add(m_map.addMarker(new MarkerOptions()
                        .position(latlng)
                        .title(board.get_id())
                        .icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.icon), 60, 60, false)))));

                CircleOptions circleOptions = new CircleOptions()
                        .center(latlng)
                        .radius(GEOFENCE_RADIUS_IN_METERS); // In meters

                m_map.addCircle(circleOptions);

                LocationBus.getInstance().post(board);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Retrofit POST failed. Body: " + error.getBody() + ", message: " + error.getMessage() + ", kind: " + error.getKind());
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        // Bind to our service
        Intent intent = new Intent(this, LocationCheckService.class);
        startService(intent);
        bindService(intent, m_connection, Context.BIND_AUTO_CREATE);

        LocationBus.getInstance().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(m_connection);
        LocationBus.getInstance().unregister(this);
    }

    private ServiceConnection m_connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Connected to service.");
            // We've bound to the service. Now cast the IBinder to get an instance of the service
            LocationCheckService.LocationCheckBinder binder = (LocationCheckService.LocationCheckBinder) service;
            m_service = binder.getService();
            m_bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Disconnected from service.");
        }
    };

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

    public void success(List<Board> boards, Response response) {
        if (boards != null && boards.size() > 0) {
            Log.i(TAG, "Retrofit GET successful.");
        } else {
            Log.e(TAG, "Retrofit GET failed. Reponse was " + response.getBody());
            return;
        }
        for (Board board : boards) {
            List<String> message_list = board.getMessages();
            for (String s : message_list) {
                Log.i(TAG, "Receieved message: " + s);
            }
            JsonLocation loc = board.getLocation();
            List<Float> coords = loc.getCoordinates();
            Log.i(TAG, "Location: lat = " + coords.get(0) + ", long = " + coords.get(1) + ", board_id = " + board.get_id());
        }
    }

    @Subscribe
    public void populateMap(ArrayList<Board> boards) {
        Log.i(TAG, "Populating map with " + boards.size() + " boards");
        LatLng latlng = m_lastLocation;
        if (latlng == null) {
            Log.e(TAG, "Can't zoom to a null LatLng!");
        }
        List<Marker> newMarkers = new ArrayList<>();

        for (Board board : boards) {
            newMarkers.add(m_map.addMarker(new MarkerOptions()
                    .position(board.get_latlng())
                    .title(board.get_id())
                    .icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.icon), 60, 60, false)))));

            Log.i(TAG, "Marker added at " + board.get_latlng().latitude + ", " + board.get_latlng().longitude);
        }
        if (m_map != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latlng)             // Sets the center of the map to current location
                    .zoom(ZOOM_LEVEL)                   // Sets the zoom
                    .tilt(0)                   // Sets the tilt of the camera to 0 degrees
                    .build();                   // Creates a CameraPosition from the builder
            m_map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            if (m_markers != null) {
                for (int i = 0; i < m_markers.size(); i++) {
                    m_markers.get(i).remove();
                }
                m_markers.clear();
            } else {
                m_markers = new ArrayList<>();
            }
            m_markers = newMarkers;
            m_markers.add(m_map.addMarker(new MarkerOptions()
                    .position(latlng)
                    .title("Current location")));
        }
    }

    @Subscribe
    public void onLocationChanged(LatLng latlng) {
        m_lastLocation = latlng;
    }

    @Override
    public void failure(RetrofitError error) {
        Log.e(TAG, "Retrofit GET failed. Body: " + error.getBody() + ", message: " + error.getMessage() + ", kind: " + error.getKind());
    }

    @Override
    public void onDialogPositiveClick(String message) {
        LatLng latlng;
        if (m_isMapTouch) {
            latlng = m_lastMapTouch;
        } else {
            latlng = m_lastLocation;
        }
        if (latlng != null) {
            leaveMessage(latlng, message);
        }
        m_isMapTouch = false;
    }

    @Override
    public void onDialogNegativeClick() {

    }

    public void toMsgView(View v){
        Intent intent = new Intent(this, MessageViewActivity.class);
        startActivity(intent);
    }

    public void showNewMessageDialog() {
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new NewMessageDialogFragment();
        dialog.show(getFragmentManager(), "NewMessageDialogFragment");
    }

}
