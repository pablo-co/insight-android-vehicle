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

package mx.itesm.logistics.vehicle_tracking.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import edu.mit.lastmite.insight_library.communication.TargetListener;
import edu.mit.lastmite.insight_library.http.APIFetch;
import edu.mit.lastmite.insight_library.http.APIResponseHandler;
import edu.mit.lastmite.insight_library.http.ErrorHandler;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.Helper;
import mx.itesm.logistics.vehicle_tracking.R;
import mx.itesm.logistics.vehicle_tracking.fragment.ShopListFragment;
import mx.itesm.logistics.vehicle_tracking.fragment.StopPeriodFragment;
import mx.itesm.logistics.vehicle_tracking.fragment.TrackFragment;
import mx.itesm.logistics.vehicle_tracking.fragment.VehicleListFragment;
import edu.mit.lastmite.insight_library.model.Vehicle;
import edu.mit.lastmite.insight_library.model.User;
import mx.itesm.logistics.vehicle_tracking.receiver.LocationReceiver;
import mx.itesm.logistics.vehicle_tracking.service.LocationManagerService;
import mx.itesm.logistics.vehicle_tracking.util.Lab;
import mx.itesm.logistics.vehicle_tracking.util.VehicleAppComponent;

public class MainActivity extends SingleFragmentActivity implements TargetListener {

    public static final int REQUEST_TRACKS = 0;
    public static final int REQUEST_PARKING = 1;
    public static final int REQUEST_DELIVERING = 2;
    public static final int REQUEST_SHOPS = 3;
    public static final int REQUEST_TRUCKS = 4;

    @Override
    public void injectActivity(ApplicationComponent component) {
        ((VehicleAppComponent) component).inject(this);
    }

    @Override
    protected Fragment createFragment() {
        TrackFragment fragment = new TrackFragment();
        fragment.setTargetListener(this, REQUEST_TRACKS);
        return fragment;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_main;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overrideTransitions = false;
        super.onCreate(savedInstanceState);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onResult(int requestCode, int resultCode, final Intent data) {
        if (resultCode != TargetListener.RESULT_OK) return;

        switch (requestCode) {
            case REQUEST_TRACKS:
                Helper.get(this).inflateFragment(getSupportFragmentManager(), R.id.fragmentContainer, new Helper.FragmentCreator() {
                    @Override
                    public Fragment createFragment() {
                        StopPeriodFragment fragment = StopPeriodFragment.newInstance(
                                getString(R.string.parking_title),
                                getString(R.string.parking_content),
                                getString(R.string.parking_action),
                                R.mipmap.bg_parking
                        );
                        fragment.setTargetListener(MainActivity.this, REQUEST_PARKING);
                        return fragment;
                    }
                }, R.animator.no_animation, R.animator.no_animation);
                break;
            case REQUEST_PARKING:
                Helper.get(this).inflateFragment(getSupportFragmentManager(), R.id.fragmentContainer, new Helper.FragmentCreator() {
                    @Override
                    public Fragment createFragment() {
                        StopPeriodFragment fragment = StopPeriodFragment.newInstance(
                                getString(R.string.delivering_title),
                                getString(R.string.delivering_content),
                                getString(R.string.delivering_action),
                                R.mipmap.bg_delivering
                        );
                        fragment.setTargetListener(MainActivity.this, REQUEST_DELIVERING);
                        return fragment;
                    }
                }, R.animator.no_animation, R.animator.no_animation);
                break;
            case REQUEST_DELIVERING:
                Helper.get(this).inflateFragment(getSupportFragmentManager(), R.id.fragmentContainer, new Helper.FragmentCreator() {
                    @Override
                    public Fragment createFragment() {
                        ShopListFragment fragment = new ShopListFragment();
                        fragment.setTargetListener(MainActivity.this, REQUEST_SHOPS);
                        return fragment;
                    }
                }, R.animator.no_animation, R.animator.no_animation);
                break;
            case REQUEST_SHOPS:
                Helper.get(this).inflateFragment(getSupportFragmentManager(), R.id.fragmentContainer, new Helper.FragmentCreator() {
                    @Override
                    public Fragment createFragment() {
                        TrackFragment fragment = new TrackFragment();
                        fragment.setTargetListener(MainActivity.this, REQUEST_TRACKS);
                        return fragment;
                    }
                }, R.animator.no_animation, R.animator.no_animation);
                break;
            case REQUEST_TRUCKS:
                Vehicle vehicle = (Vehicle) data.getSerializableExtra(VehicleListFragment.EXTRA_TRUCK);
                Lab.get(this).setVehicle(vehicle).saveVehicle();
                startBackgroundServices();
                break;
        }
    }

    protected void startBackgroundServices() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        sharedPreferences.edit().putBoolean(getString(R.string.location_enabled), true).apply();
        Intent intent = new Intent(this, LocationManagerService.class);
        startService(intent);
    }

    protected void stopBackgroundServices() {
        Intent destroyIntent = new Intent(this, LocationManagerService.class);
        stopService(destroyIntent);
    }

    private BroadcastReceiver mLocationReceiver = new LocationReceiver() {

        @Override
        protected void onProviderEnabledChanged(boolean enabled) {
            int toastText = enabled ? R.string.gps_enabled : R.string.gps_disabled;
            Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_LONG).show();
        }
    };

}
