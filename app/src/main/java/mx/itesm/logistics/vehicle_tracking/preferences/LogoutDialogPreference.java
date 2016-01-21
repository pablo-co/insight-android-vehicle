package mx.itesm.logistics.vehicle_tracking.preferences;

import android.content.Context;
import android.util.AttributeSet;

import javax.inject.Inject;

import edu.mit.lastmite.insight_library.preferences.DaggerDialogPreference;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import mx.itesm.logistics.vehicle_tracking.util.Api;
import mx.itesm.logistics.vehicle_tracking.util.VehicleAppComponent;

/**
 * The LogoutDialogPreference will display a dialog, and will persist the
 * <code>true</code> when pressing the positive button and <code>false</code>
 * otherwise. It will persist to the android:key specified in xml-preference.
 */
public class LogoutDialogPreference extends DaggerDialogPreference {

    @Inject
    protected Api mApi;

    @Override
    public void injectPreference(ApplicationComponent component) {
        ((VehicleAppComponent) component).inject(this);
    }

    public LogoutDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        mApi.logout();
    }
}