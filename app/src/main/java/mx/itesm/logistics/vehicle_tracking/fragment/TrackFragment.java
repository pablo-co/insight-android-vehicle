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

package mx.itesm.logistics.vehicle_tracking.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TableLayout;

import com.airbnb.android.airmapview.AirMapInterface;
import com.airbnb.android.airmapview.AirMapMarker;
import com.airbnb.android.airmapview.AirMapPolyline;
import com.airbnb.android.airmapview.AirMapView;
import com.airbnb.android.airmapview.listeners.OnMapInitializedListener;
import com.google.android.gms.maps.model.LatLng;
import com.rey.material.widget.Button;
import com.rey.material.widget.TextView;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.apache.http.Header;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import edu.mit.lastmite.insight_library.fragment.FragmentResponder;
import edu.mit.lastmite.insight_library.http.APIFetch;
import edu.mit.lastmite.insight_library.http.APIResponseHandler;
import edu.mit.lastmite.insight_library.model.Parking;
import edu.mit.lastmite.insight_library.model.Route;
import edu.mit.lastmite.insight_library.model.Stop;
import edu.mit.lastmite.insight_library.model.Vehicle;
import edu.mit.lastmite.insight_library.queue.NetworkTaskQueue;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.Helper;
import mx.itesm.logistics.vehicle_tracking.R;
import edu.mit.lastmite.insight_library.model.Location;
import mx.itesm.logistics.vehicle_tracking.VehicleTrackingApplication;
import mx.itesm.logistics.vehicle_tracking.service.LocationManagerService;
import mx.itesm.logistics.vehicle_tracking.task.CreateRouteTask;
import mx.itesm.logistics.vehicle_tracking.util.Api;
import mx.itesm.logistics.vehicle_tracking.util.Lab;
import mx.itesm.logistics.vehicle_tracking.util.VehicleAppComponent;

public class TrackFragment extends FragmentResponder implements OnMapInitializedListener {

    protected static final int TIMER_LENGTH = 1000;
    protected static final float OVERLAY_OPACITY = 0.35f;

    public enum State {
        IDLE,
        TRACKING,
        PARKING,
        DELIVERING
    }

    @Inject
    protected Bus mBus;

    @Inject
    protected APIFetch mAPIFetch;

    @Inject
    protected NetworkTaskQueue mNetworkTaskQueue;

    protected CountDownTimer mCountDownTimer;
    protected AirMapInterface mAirMapInterface;
    protected List<LatLng> mTrackingPoints;
    protected AirMapPolyline mAirMapPolyline;
    protected AirMapMarker mMarker;

    protected State mState = State.IDLE;
    protected int times = 0;
    protected float mAcumDistance = 0.0f;
    protected Location mLastLocation;
    protected BigDecimal mAcumSpeed = new BigDecimal(0);
    protected BigDecimal mSpeedCount = new BigDecimal(0);

    protected Route mRoute;
    protected Parking mParking;
    protected Stop mStop;

    @Bind(R.id.track_startButton)
    protected Button mStartButton;

    @Bind(R.id.track_deliveringButton)
    protected Button mDeliveringButton;

    @Bind(R.id.track_parkingButton)
    protected Button mParkingButton;

    @Bind(R.id.track_trackingLayout)
    protected LinearLayout mTrackingLayout;

    @Bind(R.id.track_timeTextView)
    protected TextView mTimeTextView;

    @Bind(R.id.track_mapView)
    protected AirMapView mMapView;

    @Bind(R.id.track_overlayLayout)
    protected FrameLayout mOverlayLayout;

    @Bind(R.id.track_stateTextView)
    protected TextView mStateTextView;

    @Bind(R.id.track_actionButton)
    protected Button mActionButton;

    @Bind(R.id.track_distanceTextView)
    protected TextView mDistanceTextView;

    @Bind(R.id.track_speedTextView)
    protected TextView mSpeedTextView;

    @Bind(R.id.track_statsLayout)
    protected TableLayout mStatsLayout;

    @Bind(R.id.track_averageSpeedTextView)
    protected TextView mAverageSpeedTextView;

    @Override
    public void injectFragment(ApplicationComponent component) {
        ((VehicleAppComponent) component).inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        ((VehicleTrackingApplication) getActivity().getApplicationContext()).getVehicleComponent().inject(this);
        mBus.register(this);

        mTrackingPoints = new ArrayList<>();
        mAirMapPolyline = new AirMapPolyline(null, mTrackingPoints, 0, 5, getResources().getColor(R.color.colorAccent));

        resetRoute();
        resetParking();
        resetStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_track, container, false);
        ButterKnife.bind(this, view);

        mMapView.setOnMapInitializedListener(this);
        mMapView.initialize(getChildFragmentManager());

        mOverlayLayout.setAlpha(OVERLAY_OPACITY);
        startIdle();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.menu_track, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.track_menu_item_stop:
                startIdle();
                sendStopTracking();
                return true;
            case R.id.track_menu_item_logout:
                Api.get(getContext()).logout();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }


    @Override
    public void onMapInitialized() {
        mAirMapInterface = mMapView.getMapInterface();
        mAirMapInterface.setMyLocationEnabled(true);
    }

    @OnClick(R.id.track_startButton)
    protected void onStartClicked() {
        sendStartTracking();
        startTracking();
    }

    @OnClick(R.id.track_deliveringButton)
    protected void onDeliveringClicked() {
        resetStop();
        sendStartDelivering();
        startDelivery();
    }

    @OnClick(R.id.track_parkingButton)
    protected void onParkingClicked() {
        resetParking();
        sendStartParking();
        startParking();
    }

    @OnClick(R.id.track_actionButton)
    protected void onActionClicked() {
        switch (mState) {
            case PARKING:
                resetStop();
                sendStartDelivering();
                startDelivery();
                break;
            case DELIVERING:
                sendStopDelivering();
                startTracking();
                break;
        }
    }

    protected void resetRoute() {
        mRoute = Lab.get(getContext()).getRoute();
        mRoute.setVehicleId(Lab.get(getContext()).getVehicle().getId());
    }

    protected void resetParking() {
        mParking = Lab.get(getContext()).getParking();

        if (mLastLocation != null) {
            mParking.setLatitude(mLastLocation.getLatitude());
            mParking.setLongitude(mLastLocation.getLongitude());
        }

        if (mRoute != null) {
            mParking.setRouteId(mRoute.getId());
        }
    }

    protected void resetStop() {
        mStop = new Stop();

        if (mLastLocation != null) {
            mStop.setLatitude(mLastLocation.getLatitude());
            mStop.setLongitude(mLastLocation.getLongitude());
        }

        if (mRoute != null) {
            mStop.setRouteId(mRoute.getId());
        }
    }

    /**
     * States
     **/

    protected void startIdle() {
        resetStats();
        goToState(State.IDLE);
        stopTracking();
        hideAllViews();
        showIdleView();
    }

    protected void startTracking() {
        goToState(State.TRACKING);
        hideAllViews();
        showTrackingView();
        startTimer(TIMER_LENGTH);
    }

    protected void startDelivery() {
        goToState(State.DELIVERING);
        hideAllViews();
        showDeliveringView();
    }

    protected void startParking() {
        goToState(State.PARKING);
        hideAllViews();
        showParkingView();
    }

    protected void hideAllViews() {
        mStatsLayout.setVisibility(View.GONE);
        mStartButton.setVisibility(View.GONE);
        mTimeTextView.setVisibility(View.GONE);
        mTrackingLayout.setVisibility(View.GONE);
        mActionButton.setVisibility(View.GONE);
    }

    protected void showIdleView() {
        Animation fadeInAnimation = createFadeInAnimation(200);

        mStartButton.setVisibility(View.VISIBLE);
        mOverlayLayout.setVisibility(View.VISIBLE);

        fadeInAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

    }

    protected void showTrackingView() {
        Animation fadeOutAnimation = createFadeOutAnimation(200);

        mTimeTextView.setVisibility(View.VISIBLE);
        mTrackingLayout.setVisibility(View.VISIBLE);
        mStatsLayout.setVisibility(View.VISIBLE);

        mOverlayLayout.startAnimation(fadeOutAnimation);

        fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mOverlayLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    protected void showParkingView() {
        mTimeTextView.setVisibility(View.VISIBLE);
        mActionButton.setVisibility(View.VISIBLE);
        mActionButton.setText("delivering");
    }

    protected void showDeliveringView() {
        mTimeTextView.setVisibility(View.VISIBLE);
        mActionButton.setVisibility(View.VISIBLE);
        mActionButton.setText("delivered");
    }

    protected void stopTracking() {
        mStartButton.setVisibility(View.VISIBLE);
        mTrackingLayout.setVisibility(View.GONE);
        stopTimer();
        //sendResult(TargetListener.RESULT_OK);

        Animation fadeInAnimation = createFadeInAnimation(200);

        mOverlayLayout.setVisibility(View.VISIBLE);
        mOverlayLayout.startAnimation(fadeInAnimation);
    }

    protected Animation createFadeInAnimation(int duration) {
        Animation fadeIn = new AlphaAnimation(OVERLAY_OPACITY, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        fadeIn.setDuration(duration);

        return fadeIn;
    }

    protected Animation createFadeOutAnimation(int duration) {
        Animation fadeOut = new AlphaAnimation(1, OVERLAY_OPACITY);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(duration);

        return fadeOut;
    }

    protected void goToState(State state) {
        mState = state;
        updateStateView();
    }

    protected void updateStateView() {
        String label = "";
        switch (mState) {
            case IDLE:
                label = "idle";
                break;
            case TRACKING:
                label = "tracking";
                break;
            case DELIVERING:
                label = "delivering";
                break;
            case PARKING:
                label = "parking";
                break;
        }
        mStateTextView.setText(label);
    }

    @Subscribe
    public void locationUpdate(Location location) {
        updateStats(location);
        addMarkerToMap(location.getLatitude(), location.getLongitude());
        drawPath();
    }

    protected void addMarkerToMap(double latitude, double longitude) {
        mTrackingPoints.add(new LatLng(latitude, longitude));

        if (mMarker != null) {
            mAirMapInterface.removeMarker(mMarker);
        }

        mMarker = new AirMapMarker.Builder()
                .position(new LatLng(latitude, longitude))
                .build();
        mAirMapInterface.addMarker(mMarker);
        mAirMapInterface.setCenterZoom(new LatLng(latitude, longitude), 18);
    }

    protected void drawPath() {
        mAirMapPolyline.setPoints(mTrackingPoints);
        mAirMapInterface.addPolyline(mAirMapPolyline);
    }


    protected void updateStats(Location location) {
        if (mLastLocation != null) {
            try {
                android.location.Location lastLocation = new android.location.Location("");
                lastLocation.setLatitude(mLastLocation.getLatitude());
                lastLocation.setLongitude(mLastLocation.getLongitude());

                android.location.Location newLocation = new android.location.Location("");
                newLocation.setLatitude(location.getLatitude());
                newLocation.setLongitude(location.getLongitude());

            /* Calculate distance */
                float distanceInMeters = lastLocation.distanceTo(newLocation);
                mAcumDistance += distanceInMeters / 1000.0f;
                String distance = Helper.get(getActivity()).formatDouble(mAcumDistance);
                mDistanceTextView.setText(distance + " km");

            /* Calculate speed */
                String speed = Helper.get(getActivity()).formatDouble(location.getSpeed());
                mSpeedTextView.setText(speed + " kph");

            /* Averge speed */
                mAcumSpeed = mAcumSpeed.add(new BigDecimal(location.getSpeed()));
                mSpeedCount = mSpeedCount.add(new BigDecimal(1));
                String averageSpeed = Helper.get(getActivity()).formatDouble(mAcumSpeed.divide(mSpeedCount, 2, RoundingMode.HALF_UP).doubleValue());
                mAverageSpeedTextView.setText(averageSpeed + " kph");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mLastLocation = location;

    }

    protected void updateTime() {
        mTimeTextView.setText(secondsToString(times));
    }

    protected void resetStats() {
        mAcumSpeed = new BigDecimal(0);
        mSpeedCount = new BigDecimal(0);
        mAcumDistance = 0;
        times = 0;
    }

    private String secondsToString(int time) {
        int mins = time / 60;
        int secs = time % 60;

        String strMin = String.format("%02d", mins);
        String strSec = String.format("%02d", secs);
        return String.format("%s:%s", strMin, strSec);
    }

    protected void startTimer(final int time) {
        stopTimer();
        mCountDownTimer = new CountDownTimer(time, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                times++;
                updateTime();
                startTimer(time);
            }
        };
        mCountDownTimer.start();
    }

    protected void stopTimer() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }

    /**
     * Route
     */

    protected void sendStartTracking() {
        mRoute.measureTime();
        /*CreateRouteTask task = new CreateRouteTask(mRoute);
        ((VehicleAppComponent) getComponent()).inject(task);
        mNetworkTaskQueue.add(task);*/
        Log.d("TRACKING", mRoute.buildParams().toString());
        mAPIFetch.post("routes/postRoute", mRoute.buildParams(), new APIResponseHandler(getActivity(), getActivity().getSupportFragmentManager(), false) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    routeCreated(new Route(response));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onFinish(boolean success) {
            }
        });
    }

    protected void sendStopTracking() {
        mRoute.measureTime();
        mAPIFetch.post("routes/postEnd", mRoute.buildParams(), new APIResponseHandler(getActivity(), getActivity().getSupportFragmentManager(), false) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    routeEnded(new Route(response));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onFinish(boolean success) {
            }
        });
    }

    protected void routeCreated(Route route) {
        mRoute = route;
        Lab.get(getContext()).setRoute(mRoute).saveRoute();
        startBackgroundServices();
    }

    protected void routeEnded(Route route) {
        Lab.get(getContext()).deleteRoute();
        resetRoute();
        stopBackgroundServices();
    }

    /**
     * Parking
     */

    protected void sendStartParking() {
        mParking.measureTime();
        Log.d("PARKING", mParking.buildParams().toString());
        mAPIFetch.post("parkings/postInitialparking", mParking.buildParams(), new APIResponseHandler(getActivity(), getActivity().getSupportFragmentManager(), false) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    parkingCreated(new Parking(response));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onFinish(boolean success) {
            }
        });
    }

    protected void parkingCreated(Parking parking) {
        mParking = parking;
        Lab.get(getContext()).setParking(mParking).saveParking();
    }

    protected void parkingEnded(Parking parking) {
        Lab.get(getContext()).deleteParking();
        resetParking();
    }

    /**
     * Delivering
     */

    protected void sendStartDelivering() {
        mStop.measureTime();
        Log.d("DELIVERING", mStop.buildParams().toString());
        mAPIFetch.post("stops/postInitialstop", mStop.buildParams(), new APIResponseHandler(getActivity(), getActivity().getSupportFragmentManager(), false) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    deliveringCreated(new Stop(response));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onFinish(boolean success) {
            }
        });
    }

    protected void sendStopDelivering() {
        mStop.measureTime();
        mAPIFetch.post("stops/postEndstop", mStop.buildParams(), new APIResponseHandler(getActivity(), getActivity().getSupportFragmentManager(), false) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    deliveringEnded(new Stop(response));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onFinish(boolean success) {
            }
        });
    }

    protected void deliveringCreated(Stop stop) {
        mStop = stop;
    }

    protected void deliveringEnded(Stop stop) {
        resetStop();
    }

    private void sendResult(int resultCode) {
        if (getTargetListener() == null) return;

        getTargetListener().onResult(getRequestCode(), resultCode, null);
    }

    protected void startBackgroundServices() {
        Intent intent = new Intent(getActivity(), LocationManagerService.class);
        getActivity().startService(intent);
    }

    protected void stopBackgroundServices() {
        Intent intent = new Intent(getActivity(), LocationManagerService.class);
        getActivity().stopService(intent);
    }

}