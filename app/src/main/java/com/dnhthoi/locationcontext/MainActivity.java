package com.dnhthoi.locationcontext;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks
        , GoogleApiClient.OnConnectionFailedListener
        ,LocationListener, ResultCallback< Status>,OnMapReadyCallback {


    private static final String TAG = MainActivity.class.getName();
    private TextView mTxtAcitivity;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Button mbtnRequest;
    private Button mbtnRemove;
    private GoogleMap mMapl;
    private Marker mCurrentMarker;
    protected ActivityDetectioBroadcastReciever mActivityBroadcastReciever;
    private SupportMapFragment mapFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        BuildGooogleApiClient();

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mActivityBroadcastReciever = new ActivityDetectioBroadcastReciever();

        mLocationRequest = LocationRequest.create();

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000);

        mTxtAcitivity = (TextView) findViewById(R.id.txtActivity);
        mbtnRequest = (Button) findViewById(R.id.btnRequestActivity);
        mbtnRemove = (Button) findViewById(R.id.btnRemoveActivity);

        mbtnRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RequestAction();
            }
        });
        mbtnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RemoveAction();
            }
        });

    }
    private synchronized void BuildGooogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }
    private void RequestAction(){
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                mGoogleApiClient,
                Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                getActivityDetectionPendingIntent()
        ).setResultCallback(this);
        mbtnRequest.setEnabled(false);
        mbtnRemove.setEnabled(true);
    }
    private void RemoveAction(){
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        // Remove all activity updates for the PendingIntent that was used to request activity
        // updates.
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
                mGoogleApiClient,
                getActivityDetectionPendingIntent()
        ).setResultCallback(this);
        mbtnRequest.setEnabled(true);
        mbtnRemove.setEnabled(false);
    }
    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, DetectedActivitiesIntentService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void onResult(Status status) {
        if (status.isSuccess()) {
            Log.e(TAG, "Successfully added activity detection.");

        } else {
            Log.e(TAG, "Error adding or removing activity detection: " + status.getStatusMessage());
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onStart(){
        super.onStart();
        if(!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
    }
    @Override
    public void onPause(){
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mActivityBroadcastReciever);
     super.onPause();
        if( mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
    }
    @Override
    public void onResume(){
        super.onResume();
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mActivityBroadcastReciever,
                        new IntentFilter(Constants.BROADCAST_ACTION));
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            if(mGoogleApiClient.isConnected()){
                Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                LatLng current;
                current = new LatLng(location.getLatitude(),location.getLongitude());

                mCurrentMarker = mMapl.addMarker(new MarkerOptions().position(current).title("i'm here"));
                // Showing the current location in Google Map
                mMapl.moveCamera(CameraUpdateFactory.newLatLng(current));

                // Zoom in the Google Map
                mMapl.animateCamera(CameraUpdateFactory.zoomTo(18));
            }
            return true;
        }
        if (id == R.id.normalMap){
            mMapl.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            return true;
        }
        if(id == R.id.hybird){
            mMapl.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            return true;
        }
        if(id == R.id.statellite){
            mMapl.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "connection failed");
    }

    @Override
    public void onLocationChanged(Location location) {

        mMapl.clear();
        Log.e(TAG, "onLocationChange()");
        if( mCurrentMarker != null)
            mCurrentMarker.remove();
        LatLng current = new LatLng(location.getLatitude(),location.getLongitude());
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(lastLocation != null){
            LatLng last = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());

            mMapl.addPolyline(new PolylineOptions().add(current,last).color(Color.RED).width(5));
        }


       mCurrentMarker = mMapl.addMarker(new MarkerOptions().position(current).title("i'm here"));
        // Showing the current location in Google Map
        //mMapl.moveCamera(CameraUpdateFactory.newLatLng(current));

        // Zoom in the Google Map
        //mMapl.animateCamera(CameraUpdateFactory.zoomTo(18));

    }

    public String getActivityString(int detectedActivityType) {
        Resources resources = this.getResources();
        switch(detectedActivityType) {
            case DetectedActivity.IN_VEHICLE:
                return resources.getString(R.string.in_vehicle);
            case DetectedActivity.ON_BICYCLE:
                return resources.getString(R.string.on_bicycle);
            case DetectedActivity.ON_FOOT:
                return resources.getString(R.string.on_foot);
            case DetectedActivity.RUNNING:
                return resources.getString(R.string.running);
            case DetectedActivity.STILL:
                return resources.getString(R.string.still);
            case DetectedActivity.TILTING:
                return resources.getString(R.string.tilting);
            case DetectedActivity.UNKNOWN:
                return resources.getString(R.string.unknown);
            case DetectedActivity.WALKING:
                return resources.getString(R.string.walking);
            default:
                return resources.getString(R.string.unidentifiable_activity, detectedActivityType);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMapl = googleMap;
        if(mCurrentMarker != null)
            mCurrentMarker.remove();
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        LatLng current;
        if(location != null) {
            current = new LatLng(location.getLatitude(), location.getLongitude());

        }
        else {
            current = new LatLng(10.795868943365294, 106.6611018973327);
        }

        mCurrentMarker = mMapl.addMarker(new MarkerOptions().position(current).title("i'm here"));
        // Showing the current location in Google Map
        mMapl.moveCamera(CameraUpdateFactory.newLatLng(current));

        // Zoom in the Google Map
        mMapl.animateCamera(CameraUpdateFactory.zoomTo(18));
    }

    public class ActivityDetectioBroadcastReciever extends BroadcastReceiver {
        public final String TAG = ActivityDetectioBroadcastReciever.class.getName();

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<DetectedActivity> updateActivities =
                    intent.getParcelableArrayListExtra(Constants.ACTIVITY_EXTRA);
            String strStatus = "";
            for (DetectedActivity thisActivity : updateActivities) {
                strStatus += getActivityString(thisActivity.getType()) + thisActivity.getConfidence() + "%\n";
            }
            mTxtAcitivity.setText(strStatus);
        }
    }
}
