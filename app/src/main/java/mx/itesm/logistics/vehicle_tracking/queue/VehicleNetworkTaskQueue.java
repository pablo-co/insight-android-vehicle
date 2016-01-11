package mx.itesm.logistics.vehicle_tracking.queue;


import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.gson.Gson;
import com.squareup.otto.Bus;
import com.squareup.tape.FileObjectQueue;

import edu.mit.lastmite.insight_library.model.Vehicle;
import edu.mit.lastmite.insight_library.queue.NetworkTaskQueue;
import edu.mit.lastmite.insight_library.service.NetworkQueueService;
import edu.mit.lastmite.insight_library.task.NetworkTask;
import mx.itesm.logistics.vehicle_tracking.service.VehicleNetworkQueueService;


public class VehicleNetworkTaskQueue extends NetworkTaskQueue {

    public VehicleNetworkTaskQueue(FileObjectQueue<NetworkTask> delegate, Context context, Bus bus, String fileName) {
        super(delegate, context, bus, fileName);
    }

    @Override
    public void add(NetworkTask entry) {
        super.add(entry);
        startService();
    }

    @Override
    public void startService() {
        Intent intent = new Intent(mContext, VehicleNetworkQueueService.class);
        intent.putExtra(VehicleNetworkQueueService.EXTRA_QUEUE_NAME, mFileName);
        mContext.startService(intent);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static VehicleNetworkTaskQueue create(Context context, Gson gson, Bus bus) {
        return create(context, gson, bus, NetworkTaskQueue.FILENAME);
    }

    public static VehicleNetworkTaskQueue create(Context context, Gson gson, Bus bus, String fileName) {
        FileObjectQueue<NetworkTask> delegate = createFileObjectQueue(context, gson, fileName);
        VehicleNetworkTaskQueue taskQueue = new VehicleNetworkTaskQueue(delegate, context, bus, fileName);
        taskQueue.startServiceIfNotEmpty();
        return taskQueue;
    }
}