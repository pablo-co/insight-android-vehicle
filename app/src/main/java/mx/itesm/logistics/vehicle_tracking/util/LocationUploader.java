package mx.itesm.logistics.vehicle_tracking.util;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import edu.mit.lastmite.insight_library.model.Location;
import edu.mit.lastmite.insight_library.queue.NetworkTaskQueue;
import mx.itesm.logistics.vehicle_tracking.task.CreateLocationTask;

@SuppressWarnings("UnusedDeclaration")
public class LocationUploader {
    protected Bus mBus;
    protected NetworkTaskQueue mQueue;
    protected boolean mRegistered;
    protected Api mApi;

    public LocationUploader(Bus bus, NetworkTaskQueue queue, Api api) {
        mBus = bus;
        mQueue = queue;
        mApi = api;
        mRegistered = false;
    }

    public void register() {
        if (!mRegistered) {
            mRegistered = true;
            mBus.register(this);
        }
    }

    public void unregister() {
        if (mRegistered) {
            mRegistered = false;
            mBus.unregister(this);
        }
    }

    @Subscribe
    public void onLocationEvent(Location location) {
        location.setStatus(mApi.getLocationState());
        CreateLocationTask task = new CreateLocationTask(location);
        mQueue.add(task);
    }
}
