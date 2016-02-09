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

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;

import javax.inject.Inject;

import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.Helper;
import mx.itesm.logistics.vehicle_tracking.R;
import mx.itesm.logistics.vehicle_tracking.fragment.VehicleTrackFragment;
import mx.itesm.logistics.vehicle_tracking.util.VehicleAppComponent;

public class MainActivity extends SingleFragmentActivity {
    protected static final int REQUEST_LOCATION_ACCESS = 0;

    @Inject
    protected Helper mHelper;

    @Override
    public void injectActivity(ApplicationComponent component) {
        ((VehicleAppComponent) component).inject(this);
    }

    @Override
    protected Fragment createFragment() {
        VehicleTrackFragment fragment = new VehicleTrackFragment();
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
        askLocationPermissions();
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
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_ACCESS: {
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    askLocationPermissions();
                }
                return;
            }
        }
    }

    protected boolean askLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_ACCESS);
            return true;
        }
        return false;
    }
}
