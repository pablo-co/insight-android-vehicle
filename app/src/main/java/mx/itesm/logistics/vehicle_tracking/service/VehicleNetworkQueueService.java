package mx.itesm.logistics.vehicle_tracking.service;

import android.util.Log;

import javax.inject.Inject;

import edu.mit.lastmite.insight_library.BaseLibrary;
import edu.mit.lastmite.insight_library.queue.NetworkTaskQueue;
import edu.mit.lastmite.insight_library.service.NetworkQueueService;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import mx.itesm.logistics.vehicle_tracking.queue.VehicleNetworkTaskQueue;
import mx.itesm.logistics.vehicle_tracking.util.VehicleAppComponent;

public class VehicleNetworkQueueService extends NetworkQueueService {

    @Inject
    protected VehicleNetworkTaskQueue mNetworkTaskQueue;

    @Override
    public void injectService(ApplicationComponent component) {
        ((VehicleAppComponent) component).inject(this);
    }

    /** Warning use dependency injection and copy the reference to local variables,
     * otherwise you might end up with two instances of the queue and thus potential
     * corrupt file.
     */
    @Override
    protected NetworkTaskQueue createQueue(String queueName) {
        return mNetworkTaskQueue;
    }

}