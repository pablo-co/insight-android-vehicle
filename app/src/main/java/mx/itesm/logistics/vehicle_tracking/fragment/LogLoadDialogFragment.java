package mx.itesm.logistics.vehicle_tracking.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import com.rey.material.app.Dialog;
import com.rey.material.app.SimpleDialog;
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

    @Bind(R.id.log_load_durationTextView)
    protected EditText mDurationTextView;

    public static LogLoadDialogFragment newInstance(Context context) {

        LogLoadDialogFragment fragment = new LogLoadDialogFragment();

        SimpleDialog.Builder builder = new SimpleDialog.Builder();
        builder.title(context.getString(R.string.dialog_log_load_title))
                .positiveAction(context.getString(R.string.action_save))
                .negativeAction(context.getString(R.string.action_cancel))
                .contentView(R.layout.fragment_dialog_log_load);

        fragment.mBuilder = builder;
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = this.mBuilder.build(this.getActivity());
        ButterKnife.bind(this, dialog);

        dialog.positiveActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendResult(TargetListener.RESULT_OK, Float.valueOf(mDurationTextView.getText().toString()));
                dismiss();
            }
        });

        dialog.negativeActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return dialog;
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
