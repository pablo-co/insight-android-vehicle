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


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.widget.ListViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.airbnb.android.airmapview.AirMapInterface;
import com.airbnb.android.airmapview.AirMapMarker;
import com.airbnb.android.airmapview.AirMapView;
import com.airbnb.android.airmapview.listeners.OnMapInitializedListener;
import com.google.android.gms.maps.model.LatLng;
import com.rey.material.widget.ProgressView;
import com.rey.material.widget.TextView;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import edu.mit.lastmite.insight_library.communication.TargetListener;
import edu.mit.lastmite.insight_library.fragment.FragmentResponder;
import edu.mit.lastmite.insight_library.util.Helper;
import mx.itesm.logistics.vehicle_tracking.R;
import edu.mit.lastmite.insight_library.model.Shop;

public class ShopListFragment extends FragmentResponder implements ListView.OnItemClickListener, OnMapInitializedListener {

    public static final String TAG = "ShopListFragment";

    public static final String EXTRA_CARD = "com.gruporaido.tasker.extra_card";

    public static final int REQUEST_NEW = 0;
    public static final double MAP_PERCENTAGE = 0.5;
    public static final int MAP_OFFSET = 32;

    @Inject
    protected Bus mBus;

    protected AirMapMarker mMarker;
    protected AirMapInterface mAirMapInterface;

    protected Shop mShop;

    /**
     * Layouts
     **/

    @Bind(R.id.list_shop_rootLayout)
    FrameLayout mRootLayout;

    @Bind(R.id.shopListsLayout)
    FrameLayout mShopListsLayout;


    /**
     * Shops
     **/

    protected ArrayList<Shop> mShops;
    protected ShopAdapter mShopAdapter;

    @Bind(R.id.shopsListView)
    protected ListView mShopsListView;

    @Bind(R.id.shopsLoadingProgressView)
    protected ProgressView mShopsLoadingProgressView;

    @Bind(R.id.shopsEmptyView)
    protected View mShopsEmptyView;


    /**
     * Nearby
     **/

    protected ArrayList<Shop> mNearby;
    protected ShopAdapter mNearbyAdapter;

    @Bind(R.id.nearbyListView)
    protected ListView mNearbyListView;

    @Bind(R.id.nearbyLoadingProgressView)
    protected ProgressView mNearbyLoadingProgressView;

    @Bind(R.id.nearbyEmptyView)
    protected View mNearbyEmptyView;

    /**
     * Map
     **/

    @Bind(R.id.list_shop_mapView)
    protected AirMapView mMapView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mShops = new ArrayList<>();
        mShopAdapter = new ShopAdapter(mShops);

        mNearby = new ArrayList<>();
        mNearbyAdapter = new ShopAdapter(mNearby);

        mBus.register(this);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_list_shop, parent, false);

        ButterKnife.bind(this, view);

        mMapView.setOnMapInitializedListener(this);
        mMapView.initialize(getChildFragmentManager());

        mShopsListView.setEmptyView(mShopsEmptyView);
        mShopsListView.setAdapter(mShopAdapter);
        Helper.get(getActivity()).setListViewHeightBasedOnChildren((ListViewCompat) mShopsListView);

        //mNearbyListView.setEmptyView(mNearbyLoadingProgressView);
        mNearbyListView.setEmptyView(mNearbyLoadingProgressView);
        mNearbyListView.setAdapter(mNearbyAdapter);
        mNearbyListView.setOnItemClickListener(this);
        Helper.get(getActivity()).setListViewHeightBasedOnChildren((ListViewCompat) mNearbyListView);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mRootLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                int height = (int) (mRootLayout.getHeight() * MAP_PERCENTAGE);
                Log.e(TAG, "height; " + height);

                AirMapView.LayoutParams layoutParams = new AirMapView.LayoutParams(AirMapView.LayoutParams.MATCH_PARENT, AirMapView.LayoutParams.MATCH_PARENT);
                layoutParams.setMargins(0, 0, 0, height);
                mMapView.setLayoutParams(layoutParams);

                FrameLayout.LayoutParams shopLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                shopLayoutParams.setMargins(0, height - Helper.get(getActivity()).dpToPx(MAP_OFFSET), 0, 0);
                mShopListsLayout.setLayoutParams(shopLayoutParams);
                loadShops();
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        if (mShops.isEmpty()) {
            //mShopsLoadingProgressView.setVisibility(View.VISIBLE);
            mShopsLoadingProgressView.setVisibility(View.GONE);
            mNearbyLoadingProgressView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onMapInitialized() {
        mAirMapInterface = mMapView.getMapInterface();
        mAirMapInterface.setMyLocationEnabled(true);
    }

    @Subscribe
    public void locationUpdate(edu.mit.lastmite.insight_library.model.Location location) {
        if (mMarker != null) {
            mAirMapInterface.removeMarker(mMarker);
        }

        mMarker = new AirMapMarker.Builder()
                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                .build();
        mAirMapInterface.addMarker(mMarker);

        mAirMapInterface.setCenterZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return;

        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_NEW:
                if (mShop != null) {
                    mShops.add(mShop);
                    mNearby.remove(mShop);
                    mShopAdapter.notifyDataSetChanged();
                    mNearbyAdapter.notifyDataSetChanged();
                    Helper.get(getActivity()).setListViewHeightBasedOnChildren((ListViewCompat) mNearbyListView);
                    Helper.get(getActivity()).setListViewHeightBasedOnChildren((ListViewCompat) mShopsListView);
                    mShop = null;
                }
                //loadShops();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView adapterView, View view, int position, long id) {
        mShop = (Shop) adapterView.getAdapter().getItem(position);
        //Intent intent = new Intent(getActivity(), DeliveryNewActivity.class);
        //startActivityForResult(intent, REQUEST_NEW);
        //sendResult(TargetListener.RESULT_OK, mShops.get(position));
    }

    @OnClick(R.id.nextFloatingActionButton)
    protected void onActionClick() {
        sendResult(TargetListener.RESULT_OK, null);
    }

    protected void loadShops() {
        mShops.clear();
        /*APIFetch.get(getActivity()).get("shops.json", null, new APIResponseHandler(getActivity(), getActivity().getSupportFragmentManager(), false) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                try {
                    for (int i = 0; i < response.length(); ++i) {
                        mShops.add(new Shop(response.getJSONObject(i)));
                    }
                    mShopAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onFinish(boolean success) {
                mShopsLoadingProgressView.setVisibility(View.GONE);
                mShopsListView.setEmptyView(mShopsEmptyView);
            }
        });*/

        for (int i = 0; i < 20; ++i) {
            Shop shop = new Shop();
            shop.setName(Long.toHexString(Double.doubleToLongBits(Math.random())));
            shop.setLatitude(Math.random() % 120 - 120);
            shop.setLongitude(Math.random() % 120 - 120);
            shop.setDistance(Math.random() * i * 1000);
            mNearby.add(shop);
        }
        mNearbyAdapter.notifyDataSetChanged();
        mNearbyLoadingProgressView.setVisibility(View.GONE);
        mNearbyListView.setEmptyView(mNearbyEmptyView);
        Helper.get(getActivity()).setListViewHeightBasedOnChildren((ListViewCompat) mNearbyListView);
    }

    private void sendResult(int resultCode, Shop shop) {
        if (getTargetListener() == null) {
            return;
        }

        Intent intent = new Intent();
        intent.putExtra(EXTRA_CARD, (Parcelable) shop);

        getTargetListener().onResult(getRequestCode(), resultCode, intent);
    }

    private class ShopAdapter extends ArrayAdapter<Shop> {

        public ShopAdapter(ArrayList<Shop> cards) {
            super(getActivity(), 0, cards);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_shop, null);
            }

            Shop shop = getItem(position);

            TextView nameTextView = (TextView) convertView.findViewById(R.id.item_shop_nameTextView);
            nameTextView.setText(shop.getName());

            TextView addressTextView = (TextView) convertView.findViewById(R.id.item_shop_addressTextView);
            addressTextView.setText("Avenida de los Bosques #45, Alvaro Obregon, Edo. de Mex.");

            TextView distanceTextView = (TextView) convertView.findViewById(R.id.item_shop_distanceTextView);
            distanceTextView.setText(Helper.get(getActivity()).formatDouble(shop.getDistance() / 1000.0));

            return convertView;
        }
    }

}

