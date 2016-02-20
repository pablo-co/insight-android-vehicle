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

package mx.itesm.logistics.vehicle_tracking.util;

import javax.inject.Singleton;

import dagger.Component;
import edu.mit.lastmite.insight_library.util.AppModule;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import mx.itesm.logistics.vehicle_tracking.activity.LoginActivity;
import mx.itesm.logistics.vehicle_tracking.activity.MainActivity;
import mx.itesm.logistics.vehicle_tracking.activity.SettingsActivity;
import mx.itesm.logistics.vehicle_tracking.activity.VehicleListActivity;
import mx.itesm.logistics.vehicle_tracking.fragment.DeliveryFormFragment;
import mx.itesm.logistics.vehicle_tracking.fragment.LoginFragment;
import mx.itesm.logistics.vehicle_tracking.fragment.NewVehicleFragment;
import mx.itesm.logistics.vehicle_tracking.fragment.VehicleTrackFragment;
import mx.itesm.logistics.vehicle_tracking.fragment.VehicleListFragment;
import mx.itesm.logistics.vehicle_tracking.preferences.LogoutDialogPreference;
import mx.itesm.logistics.vehicle_tracking.service.LocationManagerService;
import mx.itesm.logistics.vehicle_tracking.service.VehicleNetworkQueueService;
import mx.itesm.logistics.vehicle_tracking.task.CreateLocationTask;
import mx.itesm.logistics.vehicle_tracking.task.CreateParkingTask;
import mx.itesm.logistics.vehicle_tracking.task.CreateRouteTask;
import mx.itesm.logistics.vehicle_tracking.task.CreateStopTask;
import mx.itesm.logistics.vehicle_tracking.task.StopRouteTask;
import mx.itesm.logistics.vehicle_tracking.task.StopStopTask;

@Singleton
@Component(modules = {AppModule.class, VehicleAppModule.class})
public interface VehicleAppComponent extends ApplicationComponent {
    void inject(MainActivity activity);

    void inject(LoginActivity activity);

    void inject(SettingsActivity activity);

    void inject(VehicleListActivity activity);

    void inject(VehicleTrackFragment fragment);

    void inject(LocationManagerService service);

    void inject(VehicleNetworkQueueService service);

    void inject(LoginFragment fragment);

    void inject(DeliveryFormFragment fragment);

    void inject(VehicleListFragment fragment);

    void inject(NewVehicleFragment fragment);

    void inject(CreateRouteTask task);

    void inject(StopRouteTask task);

    void inject(CreateLocationTask task);

    void inject(CreateParkingTask task);

    void inject(CreateStopTask task);

    void inject(StopStopTask task);

    void inject(LogoutDialogPreference preference);
}