package mx.itesm.logistics.vehicle_tracking.util;


import android.app.Application;

import com.google.gson.Gson;
import com.squareup.otto.Bus;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import edu.mit.lastmite.insight_library.model.Vehicle;
import mx.itesm.logistics.vehicle_tracking.queue.VehicleNetworkTaskQueue;

@Module
public class VehicleAppModule {
    protected Application mApplication;

    public VehicleAppModule(Application application) {
        mApplication = application;
    }

    @Provides
    @Singleton
    Lab provideLab(Application application) {
        return new Lab(application);
    }

    @Provides
    @Singleton
    Api provideApi(Application application, Lab lab) {
        return new Api(application, lab);
    }

    @Provides
    @Singleton
    VehicleNetworkTaskQueue provideVehicleNetworkTaskQueue(Application application, Gson gson, Bus bus) {
        return VehicleNetworkTaskQueue.create(application, gson, bus);
    }
}