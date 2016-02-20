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

import android.content.Context;
import android.util.Log;

import edu.mit.lastmite.insight_library.model.JSONSerializer;
import edu.mit.lastmite.insight_library.model.Parking;
import edu.mit.lastmite.insight_library.model.Route;
import edu.mit.lastmite.insight_library.model.Vehicle;
import mx.itesm.logistics.vehicle_tracking.model.Driver;

// TODO change serializers and objets to ArrayList and key extraction
public class Lab {

    private static final String TAG = "Lab";
    private static final String DRIVER_FILENAME = "driver.json";
    private static final String TRUCK_FILENAME = "vehicle.json";
    private static final String ROUTE_FILENAME = "route.json";
    private static final String PARKING_FILENAME = "parking.json";
    
    protected static Driver mDriver;
    protected static Vehicle mVehicle;
    protected static Route mRoute;
    protected static Parking mParking;

    protected JSONSerializer mDriverSerializer;
    protected JSONSerializer mVehicleSerializer;
    protected JSONSerializer mRouteSerializer;
    protected JSONSerializer mParkingSerializer;

    protected Context mAppContext;

    protected Lab(Context appContext) {
        mAppContext = appContext;

        mDriverSerializer = new JSONSerializer(appContext, DRIVER_FILENAME);
        try {
            mDriver = (Driver) mDriverSerializer.loadObject("mx.itesm.logistics.vehicle_tracking.model.Driver");
        } catch (Exception e) {
            Log.e(TAG, "Error loading user: ", e);
        } finally {
            if (mDriver == null) {
                mDriver = new Driver();
            }
        }

        mVehicleSerializer = new JSONSerializer(appContext, TRUCK_FILENAME);
        try {
            mVehicle = (Vehicle) mVehicleSerializer.loadObject("edu.mit.lastmite.insight_library.model.Vehicle");
        } catch (Exception e) {
            Log.e(TAG, "Error loading vehicle: ", e);
        } finally {
            if (mVehicle == null) {
                mVehicle = new Vehicle();
            }
        }

        mRouteSerializer = new JSONSerializer(appContext, ROUTE_FILENAME);
        try {
            mRoute = (Route) mRouteSerializer.loadObject("edu.mit.lastmite.insight_library.model.Route");
        } catch (Exception e) {
            Log.e(TAG, "Error loading Route: ", e);
        } finally {
            if (mRoute == null) {
                mRoute = new Route();
            }
        }

        mParkingSerializer = new JSONSerializer(appContext, PARKING_FILENAME);
        try {
            mParking = (Parking) mParkingSerializer.loadObject("edu.mit.lastmite.insight_library.model.Parking");
        } catch (Exception e) {
            Log.e(TAG, "Error loading Parking: ", e);
        } finally {
            if (mParking == null) {
                mParking = new Parking();
            }
        }
    }

    public Driver getDriver() {
        return mDriver;
    }

    public Lab setDriver(Driver user) {
        mDriver = user;
        return this;
    }

    public boolean saveDriver() {
        try {
            mDriverSerializer.saveObject(mDriver);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteDriver() {
        try {
            mDriverSerializer.deleteObject();
            mDriver = new Driver();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Vehicle getVehicle() {
        return mVehicle;
    }

    public Lab setVehicle(Vehicle vehicle) {
        mVehicle = vehicle;
        return this;
    }

    public boolean saveVehicle() {
        try {
            mVehicleSerializer.saveObject(mVehicle);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteVehicle() {
        try {
            mVehicleSerializer.deleteObject();
            mVehicle = new Vehicle();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Route getRoute() {
        return mRoute;
    }

    public Lab setRoute(Route Route) {
        mRoute = Route;
        return this;
    }

    public boolean saveRoute() {
        try {
            mRouteSerializer.saveObject(mRoute);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteRoute() {
        try {
            mRouteSerializer.deleteObject();
            mRoute = new Route();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Parking getParking() {
        return mParking;
    }

    public Lab setParking(Parking Parking) {
        mParking = Parking;
        return this;
    }

    public boolean saveParking() {
        try {
            mParkingSerializer.saveObject(mParking);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteParking() {
        try {
            mParkingSerializer.deleteObject();
            mParking = new Parking();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
