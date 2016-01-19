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

package mx.itesm.logistics.vehicle_tracking.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.rey.material.widget.FloatingActionButton;
import com.rey.material.widget.ProgressView;

import org.apache.http.Header;
import org.json.JSONArray;

import java.util.ArrayList;

import javax.inject.Inject;

import edu.mit.lastmite.insight_library.annotation.ServiceConstant;
import edu.mit.lastmite.insight_library.communication.TargetListener;
import edu.mit.lastmite.insight_library.fragment.FragmentResponder;
import edu.mit.lastmite.insight_library.http.APIFetch;
import edu.mit.lastmite.insight_library.http.APIResponseHandler;
import edu.mit.lastmite.insight_library.model.Vehicle;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.ServiceUtils;
import mx.itesm.logistics.vehicle_tracking.R;
import mx.itesm.logistics.vehicle_tracking.util.VehicleAppComponent;

public class VehicleListFragment extends FragmentResponder implements ListView.OnItemClickListener {
    public static final String TAG = "VehicleListFragment";

    @ServiceConstant
    public static String EXTRA_TRUCK;

    public static final int REQUEST_NEW = 0;

    static {
        ServiceUtils.populateConstants(VehicleListFragment.class);
    }

    @Inject
    protected APIFetch mAPIFetch;

    private ListView mListView;
    private VehicleAdapter mVehicleAdapter;

    protected ArrayList<Vehicle> mVehicles;

    protected ProgressView mLoadingProgressView;
    protected View mEmptyView;
    protected FloatingActionButton mNewVehicleFloatingActionButton;

    @Override
    public void injectFragment(ApplicationComponent component) {
        ((VehicleAppComponent) component).inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVehicles = new ArrayList<>();
        mVehicleAdapter = new VehicleAdapter(mVehicles);
        loadVehicles();
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_vehicle_list, parent, false);

        mEmptyView = view.findViewById(android.R.id.empty);
        mLoadingProgressView = (ProgressView) view.findViewById(R.id.loadingProgressView);

        mListView = (ListView) view.findViewById(R.id.vehicle_list_listView);
        mListView.setEmptyView(mLoadingProgressView);
        mListView.setAdapter(mVehicleAdapter);
        mListView.setOnItemClickListener(this);

        mNewVehicleFloatingActionButton = (FloatingActionButton) view.findViewById(R.id.vehicle_list_newVehicleFloatingActionButton);
        mNewVehicleFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(getActivity(), VehicleNewActivity.class);
                //startActivityForResult(intent, REQUEST_NEW);
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mVehicles.size() == 0) {
            mLoadingProgressView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_NEW:
                mVehicleAdapter.notifyDataSetInvalidated();
                loadVehicles();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView adapterView, View view, int position, long id) {
        sendResult(TargetListener.RESULT_OK, mVehicles.get(position));
    }

    protected void loadVehicles() {
        mVehicles.clear();
        mAPIFetch.get("vehicles.json", null, new APIResponseHandler(getActivity(), getActivity().getSupportFragmentManager(), false) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                try {
                    for (int i = 0; i < response.length(); ++i) {
                        mVehicles.add(new Vehicle(response.getJSONObject(i)));
                    }
                    mVehicleAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onFinish(boolean success) {
                mLoadingProgressView.setVisibility(View.GONE);
                mListView.setEmptyView(mEmptyView);
            }
        });
    }

    private void sendResult(int resultCode, Vehicle vehicle) {
        if (getTargetListener() == null) {
            return;
        }

        Intent intent = new Intent();
        intent.putExtra(EXTRA_TRUCK, vehicle);

        getTargetListener().onResult(getRequestCode(), resultCode, intent);
    }

    private class VehicleAdapter extends ArrayAdapter<Vehicle> {

        public VehicleAdapter(ArrayList<Vehicle> vehicles) {
            super(getActivity(), 0, vehicles);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_vehicle, null);
            }

            Vehicle vehicle = getItem(position);

            TextView numberTextView = (TextView) convertView.findViewById(R.id.item_vehicle_identifierTextView);
            numberTextView.setText(vehicle.getIdentifier());

            return convertView;
        }
    }

}
