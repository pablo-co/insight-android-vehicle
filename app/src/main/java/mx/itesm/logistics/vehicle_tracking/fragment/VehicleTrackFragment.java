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
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionMenu;
import com.github.clans.fab.Label;
import com.rey.material.app.SimpleDialog;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import edu.mit.lastmite.insight_library.communication.TargetListener;
import edu.mit.lastmite.insight_library.event.ClearMapEvent;
import edu.mit.lastmite.insight_library.event.TimerEvent;
import edu.mit.lastmite.insight_library.fragment.TrackFragment;
import edu.mit.lastmite.insight_library.model.Location;
import edu.mit.lastmite.insight_library.model.Parking;
import edu.mit.lastmite.insight_library.model.Route;
import edu.mit.lastmite.insight_library.model.Stop;
import edu.mit.lastmite.insight_library.service.TimerService;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.ColorTransformation;
import edu.mit.lastmite.insight_library.util.Storage;
import edu.mit.lastmite.insight_library.util.TextSpeaker;
import icepick.Icepick;
import mx.itesm.logistics.vehicle_tracking.R;
import mx.itesm.logistics.vehicle_tracking.activity.SettingsActivity;
import mx.itesm.logistics.vehicle_tracking.model.Transshipment;
import mx.itesm.logistics.vehicle_tracking.queue.VehicleNetworkTaskQueue;
import mx.itesm.logistics.vehicle_tracking.service.LocationManagerService;
import mx.itesm.logistics.vehicle_tracking.task.CreateParkingTask;
import mx.itesm.logistics.vehicle_tracking.task.CreateRouteTask;
import mx.itesm.logistics.vehicle_tracking.task.CreateStopTask;
import mx.itesm.logistics.vehicle_tracking.task.CreateTransshipmentTask;
import mx.itesm.logistics.vehicle_tracking.task.StopRouteTask;
import mx.itesm.logistics.vehicle_tracking.task.StopStopTask;
import mx.itesm.logistics.vehicle_tracking.task.StopTransshipmentTask;
import mx.itesm.logistics.vehicle_tracking.util.Api;
import mx.itesm.logistics.vehicle_tracking.util.Lab;
import mx.itesm.logistics.vehicle_tracking.util.LocationUploader;
import mx.itesm.logistics.vehicle_tracking.util.Preferences;
import mx.itesm.logistics.vehicle_tracking.util.VehicleAppComponent;
import mx.itesm.logistics.vehicle_tracking.view.DCOverlayMenuView;
import mx.itesm.logistics.vehicle_tracking.view.TransOverlayMenuView;

public class VehicleTrackFragment extends TrackFragment implements TargetListener {
    public static final int REQUEST_DURATION = 0;

    public static final int PANEL_COLOR = Color.rgb(96, 125, 139);

    public enum TrackState implements State {
        IDLE,
        STARTING,
        LOADING,
        PAUSED,
        WAITING_LOCATION,
        TRACKING,
        PARKING,
        LOADING_TRANSSHIPMENT,
        UNLOADING_TRANSSHIPMENT,
        DELIVERING
    }

    @Inject
    protected transient Bus mBus;

    @Inject
    protected transient VehicleNetworkTaskQueue mNetworkTaskQueue;

    @Inject
    protected transient Api mApi;

    @Inject
    protected transient Lab mLab;

    @Inject
    protected transient TextSpeaker mTextSpeaker;

    @Inject
    protected transient LocationUploader mLocationUploader;

    @Inject
    protected transient Storage mStorage;

    @icepick.State
    protected Route mRoute;

    @icepick.State
    protected Parking mParking;

    @icepick.State
    protected Stop mStop;

    @icepick.State
    protected Transshipment mTransshipment;

    @icepick.State
    protected long mLoadingStartTime = -1;

    @icepick.State
    protected long mLoadingEndTime = -1;

    @icepick.State
    protected boolean mIsShowingButtons = false;

    @Bind(R.id.track_deliveringButton)
    protected com.github.clans.fab.FloatingActionButton mDeliveringButton;

    /**
     * Parking
     */

    @Bind(R.id.track_parkingButton)
    protected FloatingActionButton mParkingButton;

    @Bind(R.id.track_parkingLayout)
    protected FrameLayout mParkingLayout;

    /**
     * Delivering
     */

    @Bind(R.id.track_deliveringMenu)
    protected FloatingActionMenu mDeliveringMenu;

    @Bind(R.id.track_deliveringLayout)
    protected FrameLayout mDeliveringLayout;

    /**
     * Transshipment
     */

    @Bind(R.id.track_unloadingTransshipmentButton)
    protected com.github.clans.fab.FloatingActionButton mTransshipmentButton;


    /**
     * Stop
     */

    @Bind(R.id.track_stopLayout)
    protected View mStopLayout;

    @Bind(R.id.track_stopButton)
    protected FloatingActionButton mStopButton;


    /**
     * DC
     */

    @Bind(R.id.overlay_dc)
    protected DCOverlayMenuView mDCOverlayMenuView;

    @Bind(R.id.track_dcButton)
    protected FloatingActionButton mDCButton;

    @Bind(R.id.track_dcLayout)
    protected FrameLayout mDCLayout;

    /**
     * Trans
     */

    @Bind(R.id.overlay_trans)
    protected TransOverlayMenuView mTransOverlayMenuView;

    @Bind(R.id.track_transButton)
    protected FloatingActionButton mTransButton;

    @Bind(R.id.track_transLayout)
    protected FrameLayout mTransLayout;

    /**
     * Layouts
     */

    @Bind(R.id.track_panelLayout)
    protected LinearLayout mPanelLayout;

    @Nullable
    @Bind(R.id.track_contentLayout)
    protected LinearLayout mContentLayout;

    @Bind(R.id.track_actionsLayout)
    protected FrameLayout mActionsLayout;

    @Override
    public void injectFragment(ApplicationComponent component) {
        ((VehicleAppComponent) component).inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            mState = (TrackState) savedInstanceState.getSerializable(KEY_STATE);
            mLastState = (TrackState) savedInstanceState.getSerializable(KEY_LAST_STATE);
            mWaitForLocationCallback = (WaitForLocationCallback) savedInstanceState.getSerializable(KEY_WAITING_CALLBACK);
            Icepick.restoreInstanceState(this, savedInstanceState);
        }

        if (mState == null) {
            mState = TrackState.IDLE;
        }

        resetRoute();
        resetParking();
        resetStop();
        startBackgroundServices();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_track, container, false);
        ButterKnife.bind(this, view);

        findTrackViews(view);
        findPanelLayout(view, R.id.track_slidingUpPanel);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updatePanelShowingStatus();
        inflateMapsFragment(R.id.track_mapLayout);
        mSlidingUpPanel.setTouchEnabled(false);
        mDCOverlayMenuView.setToggle(mDCButton);
        mTransOverlayMenuView.setToggle(mTransButton);
        renderViewState((TrackState) mState);
        updateActionButtonColors();
        registerPaneListener();
        applyLabelsSettings();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mState == TrackState.IDLE) {
            stopBackgroundServices();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.menu_track, menu);
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem cancelItem = menu.findItem(R.id.track_menu_item_pause);
        MenuItem stopItem = menu.findItem(R.id.track_menu_item_stop);

        boolean actionsVisible = mState == TrackState.TRACKING;
        cancelItem.setVisible(actionsVisible);
        stopItem.setVisible(actionsVisible);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.track_menu_item_stop:
                getStopConfirmation();
                return true;
            case R.id.track_menu_item_pause:
                getPauseConfirmation();
                return true;
            case R.id.track_menu_item_settings:
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mState != null) {
            outState.putSerializable(KEY_STATE, (TrackState) mState);
        }

        if (mLastState != null) {
            outState.putSerializable(KEY_LAST_STATE, (TrackState) mLastState);
        }

        if (mWaitForLocationCallback != null) {
            //outState.putSerializable(KEY_WAITING_CALLBACK, mWaitForLocationCallback);
        }
        Icepick.saveInstanceState(this, outState);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onTimerEvent(TimerEvent event) {
        super.onTimerEvent(event);
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_startButton)
    protected void onStartClicked() {
        waitForLocation(new WaitForLocationCallback() {
            @Override
            public void onReceivedLocation(Location location) {
                sendStartRoute();
                startTracking();
            }
        });
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_deliveringButton)
    protected void onDeliveringClicked() {
        resetStop();
        sendStartDelivering();
        startDelivering();
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_unloadingTransshipmentButton)
    protected void onStartUnloadingTransshipmentClicked() {
        startUnloadingTransshipment();
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
        switch ((TrackState) mState) {
            case LOADING_TRANSSHIPMENT:
                saveEndTime();
                sendStartTransshipment();
                sendStopTransshipment();
                startNewTrip();
                break;
            case UNLOADING_TRANSSHIPMENT:
                saveEndTime();
                sendStopTransshipment();
                startTracking();
                break;
            case PAUSED:
                startTracking();
                break;
            case LOADING:
                saveEndTime();
                startNewTrip();
                break;
            case DELIVERING:
                sendStopDelivering();
                startTracking();
                mBus.post(new ClearMapEvent());
                break;
        }
    }


    /**
     * Delivering menu
     */

    /*@SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_startTransshipmentButton)
    protected void onStartMenuTransshippingClicked() {
        setTransshipmentLoading();
        saveStartTime();
        startUnloadingTransshipment();
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.track_startMenuDeliveringButton)
    protected void onStartMenuDeliveringClicked() {
        setTransshipmentLoading();
        saveStartTime();
        startUnloadingTransshipment();
    }*/
    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onLocationEvent(Location location) {
        updateStats(location);
        mLastLocation = location;
        checkIfWaitingForLocation(location);
    }

    /**
     * DC Overlay
     */

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.overlay_dc_load_button)
    public void onLoadClicked() {
        mDCOverlayMenuView.toggle();
        saveStartTime();
        startLoading();
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.overlay_dc_log_button)
    public void onLogClicked() {
        mDCOverlayMenuView.toggle();
        LogLoadDialogFragment fragment = LogLoadDialogFragment.newInstance(getContext());
        fragment.setTargetListener(this, REQUEST_DURATION);
        fragment.show(getActivity().getSupportFragmentManager(), null);
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.overlay_dc_track_button)
    public void onTrackClicked() {
        mDCOverlayMenuView.toggle();
        waitForLocation(new WaitForLocationCallback() {
            @Override
            public void onReceivedLocation(Location location) {
                startNewTrip();
            }
        });
    }

    /**
     * Transshipment Overlay
     */

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.overlay_trans_load_button)
    public void onTransLoadClicked() {
        mTransOverlayMenuView.toggle();
        if (mLastLocation == null) {
            waitForLocation(new WaitForLocationCallback() {
                @Override
                public void onReceivedLocation(Location location) {
                    startLoadingTransshipment();
                }
            });
        } else {
            startLoadingTransshipment();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.overlay_trans_track_button)
    public void onTransTrackClicked() {
        mTransOverlayMenuView.toggle();
        waitForLocation(new WaitForLocationCallback() {
            @Override
            public void onReceivedLocation(Location location) {
                startNewTrip();
            }
        });
    }

    /**
     * Dialog
     */

    protected void getStopConfirmation() {
        final SimpleDialog dialog = new SimpleDialog(getContext());
        dialog
                .message(getString(R.string.dialog_stop_message))
                .title(getString(R.string.dialog_stop_title))
                .positiveAction(getString(R.string.action_ok))
                .negativeAction(getString(R.string.action_cancel))
                .positiveActionClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startIdle();
                        sendStopRoute();
                        dialog.dismiss();
                    }
                })
                .negativeActionClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                })
                .cancelable(true)
                .show();
    }

    protected void getPauseConfirmation() {
        final SimpleDialog dialog = new SimpleDialog(getContext());
        dialog
                .message(getString(R.string.dialog_pause_message))
                .title(getString(R.string.dialog_pause_title))
                .positiveAction(getString(R.string.action_ok))
                .negativeAction(getString(R.string.action_cancel))
                .positiveActionClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startPause();
                        dialog.dismiss();
                    }
                })
                .negativeActionClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                })
                .cancelable(true)
                .show();
    }

    protected void getLogoutConfirmation() {
        final SimpleDialog dialog = new SimpleDialog(getContext());
        dialog
                .message(getString(R.string.dialog_logout_message))
                .title(getString(R.string.dialog_logout_title))
                .positiveAction(getString(R.string.action_ok))
                .negativeAction(getString(R.string.action_cancel))
                .positiveActionClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mApi.logout();
                        dialog.dismiss();
                    }
                })
                .negativeActionClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                })
                .cancelable(true)
                .show();
    }


    protected void waitForLocation(WaitForLocationCallback callback) {
        mWaitForLocationCallback = callback;
        startWaitingLocation();
    }

    protected void checkIfWaitingForLocation(Location location) {
        if (mState == TrackState.WAITING_LOCATION) {
            mWaitForLocationCallback.onReceivedLocation(location);
        }
    }

    protected void resetRoute() {
        mRoute = mLab.getRoute();

        if (mLastLocation != null) {
            mRoute.setLatitude(mLastLocation.getLatitude());
            mRoute.setLongitude(mLastLocation.getLongitude());
        }
        mRoute.setVehicleId(mLab.getVehicle().getId());
    }

    protected void resetParking() {
        mParking = new Parking();

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

    protected void resetTransshipment() {
        mTransshipment = new Transshipment();

        if (mLastLocation != null) {
            mTransshipment.setLatitude(mLastLocation.getLatitude());
            mTransshipment.setLongitude(mLastLocation.getLongitude());
        }
    }

    protected void startNewTrip() {
        sendStartRoute();
        startTracking();
        resetMap();
    }

    protected void resetMap() {
        mBus.post(new ClearMapEvent());
    }

    protected void saveStartTime() {
        mLoadingStartTime = System.currentTimeMillis();
    }

    protected void saveEndTime() {
        mLoadingEndTime = System.currentTimeMillis();
    }

    /**
     * Idle
     */

    protected void startIdle() {
        runIdleActions();
        showIdleViews();
    }

    protected void runIdleActions() {
        mLocationUploader.unregister();
        goToState(TrackState.IDLE);
        resetStats();
        stopTimer();
    }

    protected void showIdleViews() {
        updatePanelShowingButtons();
        hideAllViews();
        showIdleView();
    }

    /**
     * Tracking
     */

    protected void startStarting() {
        runStartingActions();
        showStartingViews();
    }

    protected void runStartingActions() {
        goToState(TrackState.STARTING);
    }

    protected void showStartingViews() {
        updatePanelShowingStatus();
        hideAllViews();
        showStartingView();
    }

    /**
     * Loading
     */

    protected void startLoading() {
        runLoadingActions();
        showLoadingViews();
    }

    protected void runLoadingActions() {
        goToState(TrackState.LOADING);
        resetStats();
        saveStartTime();
        startTimer();
    }

    protected void showLoadingViews() {
        updatePanelShowingButtons();
        hideAllViews();
        showLoadingView();
    }

    /**
     * Waiting
     */

    protected void startWaitingLocation() {
        runWaitingLocationActions();
        showWaitingLocationViews();
    }

    protected void runWaitingLocationActions() {
        goToState(TrackState.WAITING_LOCATION);
        startBackgroundServices();
    }

    protected void showWaitingLocationViews() {
        updatePanelShowingStatus();
        hideAllViews();
        showWaitingLocationView();
    }

    /**
     * Paused
     */

    protected void startPause() {
        runPausedActions();
        showPausedViews();
        stopTimer();
    }

    protected void runPausedActions() {
        goToState(TrackState.PAUSED);
    }

    protected void showPausedViews() {
        updatePanelShowingButtons();
        hideAllViews();
        showPausedView();
    }

    /**
     * Tracking
     */

    protected void startTracking() {
        runTrackingActions();
        showTrackingViews();
    }

    protected void runTrackingActions() {
        mLocationUploader.register();
        goToState(TrackState.TRACKING);
        resetSectionStats();
        saveEndTime();
        startTimer();
    }

    protected void showTrackingViews() {
        updatePanelShowingButtons();
        hideAllViews();
        showTrackingView();
    }

    /**
     * Delivery
     */

    protected void startDelivering() {
        runDeliveringActions();
        showDeliveringViews();
    }

    protected void runDeliveringActions() {
        goToState(TrackState.DELIVERING);
        resetSectionStats();
    }

    protected void showDeliveringViews() {
        updatePanelShowingButtons();
        hideAllViews();
        showDeliveringView();
        updateStatsView();
    }

    /**
     * Parking
     */

    protected void startParking() {
        runParkingActions();
        showParkingViews();
    }

    protected void runParkingActions() {
        goToState(TrackState.PARKING);
        resetSectionStats();
    }

    protected void showParkingViews() {
        updatePanelShowingButtons();
        hideAllViews();
        showParkingView();
        updateStatsView();
    }

    /**
     * Loading Transshipment
     */

    protected void startLoadingTransshipment() {
        runLoadingTransshipmentActions();
        showLoadingTransshipmentViews();
    }

    protected void runLoadingTransshipmentActions() {
        resetTransshipment();
        mTransshipment.setType(Transshipment.Type.LOADING);
        goToState(TrackState.LOADING_TRANSSHIPMENT);
    }

    protected void showLoadingTransshipmentViews() {
        updatePanelShowingButtons();
        hideAllViews();
        showTransshippingView();
    }

    /**
     * Unloading Transshipment
     */

    protected void startUnloadingTransshipment() {
        runUnloadingTransshipmentActions();
        showUnloadingTransshipmentViews();
    }

    protected void runUnloadingTransshipmentActions() {
        resetTransshipment();
        mTransshipment.setType(Transshipment.Type.UNLOADING);
        sendStartTransshipment();
        goToState(TrackState.UNLOADING_TRANSSHIPMENT);
        resetSectionStats();
    }

    protected void showUnloadingTransshipmentViews() {
        updatePanelShowingButtons();
        hideAllViews();
        showTransshippingView();
    }

    /**
     * Views
     */

    @Override
    protected void hideAllViews() {
        super.hideAllViews();
        mDeliveringLayout.setVisibility(View.GONE);
        mParkingLayout.setVisibility(View.GONE);
        mStopLayout.setVisibility(View.GONE);
        mPausedTextView.setVisibility(View.GONE);
        mDCLayout.setVisibility(View.GONE);
        mTransLayout.setVisibility(View.GONE);
    }

    protected void showIdleView() {
        mDCLayout.setVisibility(View.VISIBLE);
        mTransLayout.setVisibility(View.VISIBLE);
        showPanel();
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

    protected void showPausedView() {
        mPausedTextView.setVisibility(View.VISIBLE);
        mStopLayout.setVisibility(View.VISIBLE);
    }

    protected void showLoadingView() {
        mTimeTextView.setVisibility(View.VISIBLE);
        mStopLayout.setVisibility(View.VISIBLE);
        showPanel();
    }

    protected void showTrackingView() {
        mTimeTextView.setVisibility(View.VISIBLE);
        mStatsLayout.setVisibility(View.VISIBLE);
        mParkingLayout.setVisibility(View.VISIBLE);
        mDeliveringLayout.setVisibility(View.VISIBLE);
        showPanel();
    }

    protected void showParkingView() {
        mTimeTextView.setVisibility(View.VISIBLE);
        mDeliveringLayout.setVisibility(View.VISIBLE);
    }

    protected void showDeliveringView() {
        mTimeTextView.setVisibility(View.VISIBLE);
        mStopLayout.setVisibility(View.VISIBLE);
    }

    protected void showTransshippingView() {
        mTimeTextView.setVisibility(View.VISIBLE);
        mStopLayout.setVisibility(View.VISIBLE);
        startTimer();
        showPanel();
    }

    @Override
    public void goToState(State state) {
        super.goToState(state);
        if (mState == TrackState.TRACKING || mLastState == TrackState.TRACKING) {
            getActivity().supportInvalidateOptionsMenu();
        }

        if (mStorage.getSharedPreferences().getBoolean(Preferences.PREFERENCES_TEXT_TO_SPEECH, true)) {
            speakState((TrackState) state);
        }
    }

    @Override
    protected void updateStateView() {
        String label = "";
        switch ((TrackState) mState) {
            case IDLE:
                label = getString(R.string.state_idle);
                break;
            case STARTING:
                label = getString(R.string.state_starting);
                break;
            case PAUSED:
                label = getString(R.string.state_paused);
                break;
            case LOADING:
                label = getString(R.string.state_loading);
                break;
            case WAITING_LOCATION:
                label = getString(R.string.state_waiting);
                break;
            case TRACKING:
                label = getString(R.string.state_tracking);
                break;
            case LOADING_TRANSSHIPMENT:
            case UNLOADING_TRANSSHIPMENT:
                label = getString(R.string.state_transshipping);
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


    protected void renderViewState(TrackState state) {
        switch (state) {
            case IDLE:
                showIdleViews();
                break;
            case STARTING:
                showStartingViews();
                break;
            case LOADING:
                showLoadingViews();
                break;
            case PAUSED:
                showPausedViews();
                break;
            case WAITING_LOCATION:
                showWaitingLocationViews();
                break;
            case TRACKING:
                showTrackingViews();
                break;
            case PARKING:
                showParkingViews();
                break;
            case LOADING_TRANSSHIPMENT:
                showLoadingTransshipmentViews();
                break;
            case UNLOADING_TRANSSHIPMENT:
                showUnloadingTransshipmentViews();
                break;
            case DELIVERING:
                showDeliveringViews();
                break;
        }
        updateStateView();
    }

    protected void speakState(TrackState state) {
        String text = "";
        switch (state) {
            case IDLE:
                text = getString(R.string.state_idle_speak);
                break;
            case PAUSED:
                text = getString(R.string.state_paused_speak);
                break;
            case LOADING:
                text = getString(R.string.state_loading_speak);
                break;
            case PARKING:
                text = getString(R.string.state_parking_speak);
                break;
            case LOADING_TRANSSHIPMENT:
            case UNLOADING_TRANSSHIPMENT:
                text = getString(R.string.state_transshipping_speak);
                break;
            case WAITING_LOCATION:
                text = getString(R.string.state_waiting_speak);
                break;
            case TRACKING:
                text = getString(R.string.state_tracking_speak);
                break;
            case DELIVERING:
                text = getString(R.string.state_delivered_speak);
                break;
        }
        mTextSpeaker.say(text);
    }


    protected void updateActionButtonColors() {
        changeDrawableColor(R.mipmap.ic_truck_speed, PANEL_COLOR, mStopButton);
        changeDrawableColor(R.mipmap.ic_parking, PANEL_COLOR, mParkingButton);
        changeDrawableColor(R.mipmap.ic_truck_clock, PANEL_COLOR, mDeliveringButton);
        changeDrawableColor(R.mipmap.ic_load, PANEL_COLOR, mTransshipmentButton);
        changeDrawableColor(R.mipmap.ic_storage, PANEL_COLOR, mDCButton);
        changeDrawableColor(R.mipmap.ic_transship, PANEL_COLOR, mTransButton);
        changeDrawableColor(R.mipmap.ic_truck_speed, PANEL_COLOR, mDeliveringMenu.getMenuIconView());
    }

    protected void applyLabelsSettings() {
        if (mStorage.getSharedPreferences().getBoolean(Preferences.PREFERENCES_SHOW_LABELS, true)) {
            showLabels();
        } else {
            hideLabels();
        }
    }

    /**
     * Panel
     */

    protected void showPanel() {
        showPanel(PANEL_DELAY);
    }

    protected void hidePanel() {
        hidePanel(PANEL_DELAY);
    }

    protected void registerPaneListener() {
        mSlidingUpPanel.setPanelSlideListener(new SlidingUpPanelLayout.SimplePanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                if (mIsShowingButtons) {
                    slidingLayout(slideOffset);
                }
            }
        });
    }

    protected void slidingLayout(float amount) {
        fadeContentLayout(amount);
        slideActionsLayout(amount);
    }

    protected void fadeContentLayout(float amount) {
        mPanelLayout.setAlpha(amount);
    }

    protected void slideActionsLayout(float amount) {
        if (!isInPortrait()) {
            int totalPadding = mPanelLayout.getWidth() - mHelper.dpToPx(PANEL_ACTION_WIDTH);
            int padding = (int) (totalPadding * amount);
            setLeftPadding(mActionsLayout, padding);
        }
    }

    protected void updatePanelShowingStatus() {
        mIsShowingButtons = false;
        setBottomPadding(mPanelLayout, 0);
        setPanelHeight(mHelper.dpToPx(PANEL_STATUS_HEIGHT));
    }

    protected void updatePanelShowingButtons() {
        mIsShowingButtons = true;
        int actionHeight = mHelper.dpToPx(PANEL_ACTION_HEIGHT);
        setPanelHeight(actionHeight);
        if (isInPortrait()) {
            setBottomPadding(mPanelLayout, actionHeight);
        } else {
            mPanelLayout.setMinimumHeight(actionHeight);
            setRightPadding(mContentLayout, mHelper.dpToPx(PANEL_ACTION_WIDTH));
            setLeftPadding(mActionsLayout, Math.max(0, mPanelLayout.getWidth() - mHelper.dpToPx(PANEL_ACTION_WIDTH)));
        }
    }

    /**
     * Route
     */

    protected void sendStartRoute() {
        resetRoute();
        mRoute.measureTime();
        if (mLoadingStartTime != -1 && mLoadingEndTime != -1) {
            mRoute.setLoadingDuration(mLoadingEndTime - mLoadingStartTime);
        }
        CreateRouteTask task = new CreateRouteTask(mRoute);
        mNetworkTaskQueue.add(task);
        startBackgroundServices();
    }

    protected void sendStopRoute() {
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
     * Transshipping
     */

    protected void sendStartTransshipment() {
        mTransshipment.measureTime();
        CreateTransshipmentTask task = new CreateTransshipmentTask(mTransshipment);
        mNetworkTaskQueue.add(task);
    }

    protected void sendStopTransshipment() {
        mTransshipment.measureTime();
        StopTransshipmentTask task = new StopTransshipmentTask(mTransshipment);
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

    protected void startTimer() {
        Intent intent = new Intent(getActivity(), TimerService.class);
        getActivity().startService(intent);
    }

    protected void stopTimer() {
        Intent intent = new Intent(getActivity(), TimerService.class);
        getActivity().stopService(intent);
    }
}