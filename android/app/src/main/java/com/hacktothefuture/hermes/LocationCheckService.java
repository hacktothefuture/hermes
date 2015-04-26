package com.hacktothefuture.hermes;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.squareup.otto.Subscribe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class LocationCheckService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, Callback<List<Board>> {
    public static final String TAG = "LocationCheckService";
    private static final int GEOFENCE_RADIUS_IN_METERS = 15;
    private static final long LOCATION_POLLING_INTERVAL_IN_MILLIS = 3000;

    private final IBinder m_binder = new LocationCheckBinder();
    boolean m_bound;

    GoogleApiClient m_GoogleApiClient;
    RestAdapter m_restAdapter;
//    List<Board> m_boards = new ArrayList<>();
    static Map<String, List<String>> m_wallMessages = new HashMap<>();



    public LocationCheckService() {
    }

    @Override
    public void success(List<Board> boards, Response response) {
        if (boards != null && boards.size() > 0) {
            Log.i(TAG, "Retrofit GET successful.");
        } else {
            Log.e(TAG, "Retrofit GET failed. Reponse was " + response.getBody());
            return;
        }

        LatLng latlng = getLastLocation();


        for (Board board : boards) {
            double bLong = board.getLocation().getCoordinates().get(0);
            double bLat = board.getLocation().getCoordinates().get(1);
            board.set_latlng(new LatLng(bLat, bLong));
            float[] results = new float[1];
            Location.distanceBetween((double) latlng.latitude, (double) latlng.longitude,
                    (double) board.get_latlng().latitude, (double) board.get_latlng().longitude, results);

            List<String> messages;
            Log.i(TAG, "\nDistance from board: " + results[0]);
            Log.i(TAG, "\nOur pos: " + latlng.latitude + ", " + latlng.longitude);
            Log.i(TAG, "\nBoard pos: " + board.get_latlng().latitude + ", " + board.get_latlng().longitude);
            if (results[0] < GEOFENCE_RADIUS_IN_METERS) {
                messages = m_wallMessages.get(board.get_id());
                if (messages != null && messages.size() == board.getMessages().size()) {
                    continue;
                }
                else {
                    Log.d(TAG, "Sending notification for board id " + board.get_id() +
                            ", # msgs = " + (messages == null ? "null" : messages.size()));
                    sendNotification();
                    m_wallMessages.put(board.get_id(), board.getMessages());
                }
            }
        }


//        for (Board board : boards) {
//            List<String> message_list = board.getMessages();
//            for (String s : message_list) {
//            }
//            JsonLocation loc = board.getLocation();
//            List<Float> coords = loc.getCoordinates();
//        }
    }

    @Override
    public void failure(RetrofitError error) {
        Log.e(TAG, "Retrofit GET failed. Body: " + error.getBody() + ", message: " + error.getMessage() + ", kind: " + error.getKind());
    }

    public class LocationCheckBinder extends Binder {
        LocationCheckService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocationCheckService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        m_bound = true;
        return m_binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LocationBus.getInstance().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocationBus.getInstance().unregister(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand called");
        m_GoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        m_GoogleApiClient.connect();

        m_restAdapter = new RestAdapter.Builder()
                .setEndpoint(AppClient.API_URL)
                .build();

        return super.onStartCommand(intent, flags, startId);
    }

    private void getMessages(LatLng latlng) {
        AppClient.MyApp client = m_restAdapter.create(AppClient.MyApp.class);
        client.getBoards((float) latlng.latitude, (float) latlng.longitude, this);
    }



    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Lat: " + location.getLatitude() + ", Long: " + location.getLongitude() + ", Accuracy: " + location.getAccuracy());

        getMessages(new LatLng(location.getLatitude(), location.getLongitude()));
        LocationBus.getInstance().post(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    public LatLng getLastLocation() {
        if (m_GoogleApiClient == null) {
            Log.e(TAG, "GAPI is null!");
        }
        Location location = LocationServices.FusedLocationApi.getLastLocation(m_GoogleApiClient);
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    private void sendNotification() {
        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Define the notification settings.
        builder.setSmallIcon(R.drawable.abc_btn_check_material)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.abc_btn_check_material))
                .setColor(Color.RED)
                .setContentTitle(getString(R.string.wall_encountered))
//                .setContentText(getString(R.string.geofence_transition_notification_text))
                .setContentText("Click here to read message")
                .setContentIntent(notificationPendingIntent)
                .setVibrate(new long[]{2000, 2000});

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
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
        Log.e(TAG, "GoogleApiClient failed to connect.");
    }

    @Subscribe
    public void onBoardAdded(Board board) {
       if (m_wallMessages != null) {
           m_wallMessages.put(board.get_id(), board.getMessages());
       }
    }

}
