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

package mx.itesm.logistics.vehicle_tracking;

import edu.mit.lastmite.insight_library.BaseLibrary;
import edu.mit.lastmite.insight_library.util.AppModule;
import mx.itesm.logistics.vehicle_tracking.util.DaggerVehicleAppComponent;
import mx.itesm.logistics.vehicle_tracking.util.VehicleAppComponent;
import mx.itesm.logistics.vehicle_tracking.util.VehicleAppModule;


public class VehicleTrackingApplication extends BaseLibrary {

    @Override
    protected void createComponent() {
        mComponent = DaggerVehicleAppComponent.builder()
                .appModule(new AppModule(this))
                .vehicleAppModule(new VehicleAppModule(this))
                .build();
        mComponent.inject(this);
    }

    @SuppressWarnings("UnusedDeclaration")
    public VehicleAppComponent getVehicleComponent() {
        return (VehicleAppComponent) getComponent();
    }
}