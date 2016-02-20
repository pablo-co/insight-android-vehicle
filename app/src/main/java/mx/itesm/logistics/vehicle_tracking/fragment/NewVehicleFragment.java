package mx.itesm.logistics.vehicle_tracking.fragment;


import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.dd.CircularProgressButton;

import org.apache.http.Header;
import org.json.JSONObject;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import edu.mit.lastmite.insight_library.annotation.ServiceConstant;
import edu.mit.lastmite.insight_library.communication.TargetListener;
import edu.mit.lastmite.insight_library.fragment.DialogFragmentResponder;
import edu.mit.lastmite.insight_library.http.APIFetch;
import edu.mit.lastmite.insight_library.http.APIResponseHandler;
import edu.mit.lastmite.insight_library.model.Vehicle;
import edu.mit.lastmite.insight_library.util.ApplicationComponent;
import edu.mit.lastmite.insight_library.util.ServiceUtils;
import mx.itesm.logistics.vehicle_tracking.R;
import mx.itesm.logistics.vehicle_tracking.util.Lab;
import mx.itesm.logistics.vehicle_tracking.util.VehicleAppComponent;

public class NewVehicleFragment extends DialogFragmentResponder {
    @ServiceConstant
    public static String EXTRA_VEHICLE;

    static {
        ServiceUtils.populateConstants(NewVehicleFragment.class);
    }

    @Inject
    protected APIFetch mAPIFetch;

    @Inject
    protected Lab mLab;

    protected Vehicle mVehicle;

    @Bind(R.id.identifierEditText)
    protected EditText mIdentifierEditText;

    @Bind(R.id.acceptButton)
    protected CircularProgressButton mAcceptButton;

    @Override
    public void injectFragment(ApplicationComponent component) {
        ((VehicleAppComponent) component).inject(this);
    }

    @Override
    public Dialog onCreateDialog(@NonNull Bundle savedInstanceState) {
        RelativeLayout root = new RelativeLayout(getActivity());
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(root);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVehicle = new Vehicle();
        mVehicle.setCompanyId(mLab.getDriver().getCompanyId());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vehicle_new, parent, false);
        ButterKnife.bind(this, view);
        mAcceptButton.setIndeterminateProgressMode(true);
        return view;
    }

    protected void lockView() {
        mAcceptButton.setProgress(0);
        mAcceptButton.setProgress(1);
        switchControlState(false);
    }

    protected void unlockView() {
        switchControlState(true);
    }

    protected void switchControlState(boolean state) {
        mIdentifierEditText.setEnabled(state);
    }


    protected void updateVehicle() {
        mVehicle.setIdentifier(mIdentifierEditText.getText().toString());
    }

    protected void requestFinished(boolean success) {
        int progress = -1;
        if (success) progress = 0;
        mAcceptButton.setProgress(progress);
        unlockView();
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.acceptButton)
    public void onAcceptClicked() {
        updateVehicle();
        lockView();
        mAPIFetch.post("vehicles/postNewvehicle", mVehicle.buildParams(), new APIResponseHandler(getActivity(), getFragmentManager(), false) {
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    mVehicle = new Vehicle(response);
                    sendResult(TargetListener.RESULT_OK, mVehicle);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFinish(boolean success) {
                requestFinished(success);
            }
        });
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.cancelButton)
    public void onRejectClicked() {
        sendResult(TargetListener.RESULT_CANCELED, null);
    }

    protected void sendResult(int resultCode, Vehicle vehicle) {
        if (getTargetListener() == null) {
            return;
        }

        Intent data = new Intent();
        data.putExtra(EXTRA_VEHICLE, vehicle);

        getTargetListener().onResult(getRequestCode(), resultCode, data);
        dismiss();
    }

}
