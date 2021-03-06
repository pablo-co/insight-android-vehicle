package mx.itesm.logistics.vehicle_tracking.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import edu.mit.lastmite.insight_library.fragment.TrackFragment;
import edu.mit.lastmite.insight_library.model.Location;
import edu.mit.lastmite.insight_library.util.AnimatorUtils;
import edu.mit.lastmite.insight_library.util.ViewUtils;
import mx.itesm.logistics.vehicle_tracking.R;

public class TransOverlayMenuView extends OverlayMenuView {

    @Bind(R.id.overlay_trans_load_layout)
    protected View mLogLayout;

    @Bind(R.id.overlay_trans_track_layout)
    protected View mTrackLayout;

    public TransOverlayMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onViewCreated(View view) {
        ButterKnife.bind(this, view);
        mLayout = ButterKnife.findById(view, R.id.overlay_trans_menu);
        mHomeButton = ButterKnife.findById(view, R.id.overlay_trans_home_button);
        mHomeLabel = ButterKnife.findById(view, R.id.overlay_trans_home_text);
        super.onViewCreated(view);
    }

    @Override
    public void turnOn() {
        super.turnOn();

        AnimatorSet animatorSet = createOnAnimator(
                animateOutwards(mHomeButton, 200),
                animateOutwards(mTrackLayout, 300),
                animateOutwards(mLogLayout, 400)
        );

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLayoutToggle.setSelected(true);
            }
        });

        animatorSet.start();
    }

    @Override
    public void turnOff() {
        super.turnOff();

        AnimatorSet animatorSet = createOffAnimator(
                animateInwards(mLogLayout, 200),
                animateInwards(mTrackLayout, 300)
        );

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLayoutToggle.setSelected(false);
                ViewUtils.setInvisible(mLayout);
                AnimatorUtils.reset(mHomeButton);
                AnimatorUtils.reset(mTrackLayout);
                AnimatorUtils.reset(mLogLayout);
            }
        });

        animatorSet.start();
    }
}