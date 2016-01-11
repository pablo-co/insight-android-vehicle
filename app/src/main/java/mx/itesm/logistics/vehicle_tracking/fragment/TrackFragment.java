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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.Choreographer;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.github.clans.fab.FloatingActionMenu;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import edu.mit.lastmite.insight_library.communication.TargetListener;
import edu.mit.lastmite.insight_library.event.ClearMapEvent;
import edu.mit.lastmite.insight_library.model.Location;
import edu.mit.lastmite.insight_library.model.Parking;
import edu.mit.lastmite.insight_library.model.Route;
import edu.mit.lastmite.insight_library.model.Stop;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import mx.itesm.logistics.vehicle_tracking.R;
import mx.itesm.logistics.vehicle_tracking.queue.VehicleNetworkTaskQueue;
import mx.itesm.logistics.vehicle_tracking.service.LocationManagerService;
import mx.itesm.logistics.vehicle_tracking.task.CreateParkingTask;
import mx.itesm.logistics.vehicle_tracking.task.CreateRouteTask;
import mx.itesm.logistics.vehicle_tracking.task.CreateStopTask;
import mx.itesm.logistics.vehicle_tracking.task.StopRouteTask;
import mx.itesm.logistics.vehicle_tracking.task.StopStopTask;
import mx.itesm.logistics.vehicle_tracking.util.Api;
import mx.itesm.logistics.vehicle_tracking.util.Lab;
import mx.itesm.logistics.vehicle_tracking.util.VehicleAppComponent;

public class TrackFragment extends edu.mit.lastmite.insight_library.fragment.TrackFragment implements TargetListener {

    public static final int REQUEST_DURATION = 0;

    public static final int PANEL_DELAY = 30;

    public enum TrackState implements State {
        IDLE,
        STARTING,
        LOADING,
        WAITING_LOCATION,
        TRACKING,
        PARKING,
        DELIVERING
    }

    @Inject
    protected Bus mBus;

    @Inject
    protected VehicleNetworkTaskQueue mNetworkTaskQueue;

    @Inject
    protected Api mApi;

    @Inject
    protected Lab mLab;

    protected Route mRoute;
    protected Parking mParking;
    protected Stop mStop;
    protected long mLoadingStartTime = -1;
    protected long mLoadingEndTime = -1;

    @Bind(R.id.track_deliveringButton)
    protected FloatingActionButton mDeliveringButton;

    @Bind(R.id.track_parkingButton)
    protected FloatingActionButton mParkingButton;

    @Bind(R.id.track_stopButton)
    protected FloatingActionButton mStopButton;

    @Bind(R.id.track_actionsMenu)
    protected FloatingActionMenu mFloatingActionMenu;

    @Bind(R.id.track_slidingUpPanel)
    protected SlidingUpPanelLayout mSlidingUpPanel;

    @Override
    public void injectFragment(ApplicationComponent component) {
        ((VehicleAppComponent) component).inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        mState = TrackState.IDLE;

        resetRoute();
        resetParking();
        resetStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_track, container, false);
        ButterKnife.bind(this, view);

        findTrackViews(view);
        inflateMapsFragment(R.id.track_mapLayout);
        startIdle();
        mSlidingUpPanel.setTouchEnabled(false);

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
                mApi.logout();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != TargetListener.RESULT_OK) return;

        switch (requestCode) {
            case REQUEST_DURATION:
                mLoadingStartTime = 0;
                mLoadingEndTime = data.getIntExtra(LogLoadDialogFragment.EXTRA_DURATION, -1);
                startStarting();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_startButton)
    protected void onStartClicked() {
        startWaitingLocation();
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_deliveringButton)
    protected void onDeliveringClicked() {
        resetStop();
        sendStartDelivering();
        startDelivery();
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_parkingButton)
    protected void onParkingClicked() {
        resetParking();
        sendStartParking();
        startParking();
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_stopButton)
    protected void onStopClicked() {
        sendStopDelivering();
        startTracking();
        mBus.post(new ClearMapEvent());
    }


    /**
     * Menu buttons
     */

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_startTrackButton)
    protected void onStartTrackClicked() {
        sendStartTracking();
        startWaitingLocation();
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_startLoadButton)
    protected void onStartLoadClicked() {
        startLoading();
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_logLoadButton)
    protected void onLogLoadClicked() {
        LogLoadDialogFragment fragment = LogLoadDialogFragment.newInstance(getContext());
        fragment.setTargetListener(this, REQUEST_DURATION);
        fragment.show(getActivity().getSupportFragmentManager(), null);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onLocationEvent(Location location) {
        updateStats(location);
        mLastLocation = location;
        checkIfWaitingForLocation();
    }

    protected void checkIfWaitingForLocation() {
        if (mState == TrackState.WAITING_LOCATION) {
            sendStartTracking();
            startTracking();
        }
    }

    protected void resetRoute() {
        mRoute = mLab.getRoute();
        mRoute.setVehicleId(mLab.getVehicle().getId());
    }

    protected void resetParking() {
        mParking = new Parking();
        mParking.measureTime();

        if (mLastLocation != null) {
            mParking.setLatitude(mLastLocation.getLatitude());
            mParking.setLongitude(mLastLocation.getLongitude());
        }
    }

    protected void resetStop() {
        mStop = new Stop();

        if (mLastLocation != null) {
            mStop.setLatitude(mLastLocation.getLatitude());
            mStop.setLongitude(mLastLocation.getLongitude());
        }
    }

    protected void saveStartTime() {
        mLoadingStartTime = System.currentTimeMillis();
    }

    protected void saveEndTime() {
        mLoadingEndTime = System.currentTimeMillis();
    }

    /**
     * States
     **/

    protected void startIdle() {
        goToState(TrackState.IDLE);
        resetStats();
        stopTimer();
        hideAllViews();
        showIdleView();
        hidePanel();
    }

    protected void startStarting() {
        goToState(TrackState.STARTING);
        hideAllViews();
        showStartingView();
    }

    protected void startLoading() {
        saveStartTime();
        goToState(TrackState.LOADING);
        resetStats();
        startTimer(TIMER_LENGTH);
        hideAllViews();
        showLoadingView();
    }

    protected void startWaitingLocation() {
        goToState(TrackState.WAITING_LOCATION);
        hideAllViews();
        showWaitingLocationView();
        startBackgroundServices();
    }

    protected void startTracking() {
        saveEndTime();
        goToState(TrackState.TRACKING);
        startTimer(TIMER_LENGTH);
        hideAllViews();
        showTrackingView();
        resetStats();
        saveEndTime();
    }

    protected void startDelivery() {
        goToState(TrackState.DELIVERING);
        hideAllViews();
        showDeliveringView();
    }

    protected void startParking() {
        goToState(TrackState.PARKING);
        hideAllViews();
        showParkingView();
    }

    /**
     * Views
     */

    @Override
    protected void hideAllViews() {
        super.hideAllViews();
        mDeliveringButton.setVisibility(View.GONE);
        mParkingButton.setVisibility(View.GONE);
        mStopButton.setVisibility(View.GONE);
        mFloatingActionMenu.setVisibility(View.GONE);
    }

    protected void showIdleView() {
        mFloatingActionMenu.setVisibility(View.VISIBLE);
    }


    protected void showStartingView() {
        mStartButton.setVisibility(View.VISIBLE);
        showPanel();
    }

    @Override
    protected void showWaitingLocationView() {
        super.showWaitingLocationView();
        showPanel();
    }

    protected void showLoadingView() {
        mTimeTextView.setVisibility(View.VISIBLE);
        mStartButton.setVisibility(View.VISIBLE);
        showPanel();
    }

    protected void showTrackingView() {
        mTimeTextView.setVisibility(View.VISIBLE);
        mStatsLayout.setVisibility(View.VISIBLE);
        mParkingButton.setVisibility(View.VISIBLE);
        mDeliveringButton.setVisibility(View.VISIBLE);
        showPanel();
    }

    protected void showParkingView() {
        mTimeTextView.setVisibility(View.VISIBLE);
        mDeliveringButton.setVisibility(View.VISIBLE);
    }

    protected void showDeliveringView() {
        mTimeTextView.setVisibility(View.VISIBLE);
        mStopButton.setVisibility(View.VISIBLE);
    }

    protected void updateStateView() {
        String label = "";
        switch ((TrackState) mState) {
            case IDLE:
                label = getString(R.string.state_idle);
                break;
            case STARTING:
                label = getString(R.string.state_starting);
            case LOADING:
                label = getString(R.string.state_loading);
                break;
            case WAITING_LOCATION:
                label = getString(R.string.state_waiting);
                break;
            case TRACKING:
                label = getString(R.string.state_tracking);
                break;
            case DELIVERING:
                label = getString(R.string.state_delivering);
                break;
            case PARKING:
                label = getString(R.string.state_parking);
                break;
        }
        mStateTextView.setText(label);
    }

    @SuppressLint("NewApi")
    protected void showPanel() {
        mSlidingUpPanel.setTouchEnabled(true);
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Choreographer.getInstance().postFrameCallbackDelayed(new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    mSlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                }
            }, PANEL_DELAY);
        }
    }

    @SuppressLint("NewApi")
    protected void hidePanel() {
        mSlidingUpPanel.setTouchEnabled(false);
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Choreographer.getInstance().postFrameCallbackDelayed(new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    mSlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                }
            }, PANEL_DELAY);
        }
    }

    /**
     * Route
     */

    protected void sendStartTracking() {
        mRoute.measureTime();
        if (mLoadingStartTime != -1 && mLoadingEndTime != -1) {
            mRoute.setLoadingDuration(mLoadingEndTime - mLoadingStartTime);
        }
        CreateRouteTask task = new CreateRouteTask(mRoute);
        mNetworkTaskQueue.add(task);
        startBackgroundServices();
    }

    protected void sendStopTracking() {
        mRoute.measureTime();
        StopRouteTask task = new StopRouteTask(mRoute);
        mNetworkTaskQueue.add(task);
        stopBackgroundServices();
        resetRoute();
    }

    /**
     * Parking
     */

    protected void sendStartParking() {
        resetParking();
        mParking.measureTime();
        CreateParkingTask task = new CreateParkingTask(mParking);
        mNetworkTaskQueue.add(task);
    }

    /**
     * Delivering
     */

    protected void sendStartDelivering() {
        resetStop();
        mStop.measureTime();
        CreateStopTask task = new CreateStopTask(mStop);
        mNetworkTaskQueue.add(task);
    }

    protected void sendStopDelivering() {
        mStop.measureTime();
        StopStopTask task = new StopStopTask(mStop);
        mNetworkTaskQueue.add(task);
    }

    /**
     * Services
     */

    protected void startBackgroundServices() {
        Intent intent = new Intent(getActivity(), LocationManagerService.class);
        getActivity().startService(intent);
    }

    protected void stopBackgroundServices() {
        Intent intent = new Intent(getActivity(), LocationManagerService.class);
        getActivity().stopService(intent);
    }
}