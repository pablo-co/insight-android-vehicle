/*
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * Created by Pablo Cárdenas on 25/10/15.
 */

package mx.itesm.logistics.vehicle_tracking.service;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.squareup.otto.Bus;

import javax.inject.Inject;

import edu.mit.lastmite.insight_library.queue.NetworkTaskQueue;
import edu.mit.lastmite.insight_library.service.DaggerIntentService;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.Storage;
import mx.itesm.logistics.vehicle_tracking.queue.VehicleNetworkTaskQueue;
import mx.itesm.logistics.vehicle_tracking.task.CreateLocationTask;
import mx.itesm.logistics.vehicle_tracking.util.Lab;
import mx.itesm.logistics.vehicle_tracking.util.Preferences;
import mx.itesm.logistics.vehicle_tracking.util.VehicleAppComponent;

public class LocationManagerService extends DaggerIntentService implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private static final String TAG = "LocationManagerService";

    public static final String ACTION_LOCATION = "mx.itesm.cartokm2.ACTION_LOCATION";

    private static final int LOCATION_REQUEST_INTERVAL_ACTIVE = 5000;
    private static final int LOCATION_REQUEST_MAX_INTERVAL_ACTIVE = 5000;

    @Inject
    protected Bus mBus;

    @Inject
    protected Lab mLab;

    @Inject
    protected VehicleNetworkTaskQueue mNetworkTaskQueue;

    @Inject
    protected Storage mStorage;

    protected GoogleApiClient mGoogleApiClient;
    protected edu.mit.lastmite.insight_library.model.Location mLastLocation;

    public LocationManagerService() {
        super("LocationManagerService");
    }

    @Override
    public void injectService(ApplicationComponent component) {
        ((VehicleAppComponent) component).inject(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        buildGoogleApiClient();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        buildGoogleApiClient();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connection started");
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {
        if (mLab.getVehicle().isEmpty()) {
            stopSelf();
        }
        boolean isFirstLocation = mLastLocation == null;
        edu.mit.lastmite.insight_library.model.Location aggregateLocation = new edu.mit.lastmite.insight_library.model.Location(location);
        //aggregateLocation.setRouteId(route.getId());
        initLocation(aggregateLocation);
        if (!isFirstLocation) {
            saveLocation(aggregateLocation);
        }
        //cartoLocation.setVehicleId(Lab.get(this).getVehicle().getId());
    }

    protected void initLocation(edu.mit.lastmite.insight_library.model.Location location) {
        if (mLastLocation != null) {
            long seconds = (location.getTime() - mLastLocation.getTime()) / 1000;
            android.location.Location lastLocation = new android.location.Location("");
            lastLocation.setLatitude(mLastLocation.getLatitude());
            lastLocation.setLongitude(mLastLocation.getLongitude());

            android.location.Location newLocation = new android.location.Location("");
            newLocation.setLatitude(location.getLatitude());
            newLocation.setLongitude(location.getLongitude());

            float distanceInMeters = lastLocation.distanceTo(newLocation);
            float speed = 0.0f;
            if (seconds != 0) {
                speed = distanceInMeters / seconds * 3.6f;
            }
            location.setSpeed(speed);
        }
        mLastLocation = location;
    }

    protected void saveLocation(final edu.mit.lastmite.insight_library.model.Location location) {
        if (location != null) {
            mBus.post(location);
        }
    }


    /**
     *
     */
    protected void startLocationUpdates() {
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, createLocationRequest(), this);
        }
    }

    /**
     *
     */
    protected void stopLocationUpdates() {
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    /**
     *
     */
    protected LocationRequest createLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        int interval = getFrequency();
        int maxInterval = getMaxFrequency();
        int priority = LocationRequest.PRIORITY_HIGH_ACCURACY;
        locationRequest.setInterval(interval);
        locationRequest.setFastestInterval(maxInterval);
        locationRequest.setPriority(priority);
        return locationRequest;
    }

    protected int getFrequency() {
        String frequencyStr = mStorage.getGlobalString(Preferences.PREFERENCES_GPS_FREQUENCY);
        int frequency = LOCATION_REQUEST_INTERVAL_ACTIVE;;
        if (frequencyStr != null && !frequencyStr.equals("null")) {
            frequency = Integer.valueOf(frequencyStr);
        }
        return frequency;
    }

    protected int getMaxFrequency() {
        String frequencyStr = mStorage.getGlobalString(Preferences.PREFERENCES_GPS_FREQUENCY);
        int maxFrequency = LOCATION_REQUEST_MAX_INTERVAL_ACTIVE;;
        if (frequencyStr != null && !frequencyStr.equals("null")) {
            maxFrequency = Integer.valueOf(frequencyStr);
        }
        return maxFrequency;
    }

    /**
     *
     */
    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
}
