package mx.itesm.logistics.vehicle_tracking.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;

import com.rey.material.widget.EditText;

import butterknife.Bind;
import butterknife.ButterKnife;
import edu.mit.lastmite.insight_library.annotation.ServiceConstant;
import edu.mit.lastmite.insight_library.communication.TargetListener;
import edu.mit.lastmite.insight_library.fragment.DialogFragmentResponder;
import edu.mit.lastmite.insight_library.util.ServiceUtils;
import mx.itesm.logistics.vehicle_tracking.R;

public class LogLoadDialogFragment extends DialogFragmentResponder {

    @ServiceConstant
    public static String EXTRA_DURATION;

    static {
        ServiceUtils.populateConstants(LogLoadDialogFragment.class);
    }

    protected AlertDialog.Builder mBuilder;

    @Bind(R.id.log_load_durationTextView)
    protected EditText mDurationTextView;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater()
                .inflate(R.layout.fragment_dialog_log_load, null, false);
        mBuilder = new AlertDialog.Builder(getContext());
        mBuilder
                .setTitle(R.string.dialog_log_load_title)
                .setView(view)
                .setPositiveButton(R.string.action_send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendResult(TargetListener.RESULT_OK, Float.valueOf(mDurationTextView.getText().toString()));
                        dismiss();
                    }
                })
                .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                });

        Dialog dialog = mBuilder.create();
        //mDurationTextView = (EditText) dialog.findViewById(R.id.log_load_durationTextView);
        ButterKnife.bind(this, view);

        return dialog;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d("as", "ass");
    }

    protected void sendResult(int resultCode, float duration) {
        if (getTargetListener() == null) {
            return;
        }

        Intent intent = new Intent();
        intent.putExtra(EXTRA_DURATION, (int) (duration * 60 * 1000));

        getTargetListener().onResult(getRequestCode(), resultCode, intent);
    }
}
