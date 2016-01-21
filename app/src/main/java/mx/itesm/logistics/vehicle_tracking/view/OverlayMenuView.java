package mx.itesm.logistics.vehicle_tracking.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.ButterKnife;
import edu.mit.lastmite.insight_library.listener.Refreshable;
import edu.mit.lastmite.insight_library.util.AnimatorUtils;
import edu.mit.lastmite.insight_library.util.ViewUtils;
import edu.mit.lastmite.insight_library.view.AppView;

public class OverlayMenuView extends AppView<Refreshable> {
    protected View mLayout;
    protected View mLayoutToggle;
    protected View mHomeButton;
    protected View mHomeLabel;

    protected float mToggleX;
    protected float mToggleY;

    public OverlayMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onViewCreated(View view) {
    }

    public void toggle() {
        if (canTurnOff()) {
            turnOff();
        } else {
            turnOn();
        }
    }

    public void setToggle(View view) {
        mLayoutToggle = view;
        mLayoutToggle.setSelected(ViewUtils.isVisible(mLayout));
        mLayoutToggle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });
    }

    public void turnOn() {
        ViewUtils.setVisible(mLayout);
        mToggleX = getViewX(mLayoutToggle); //ViewUtils.getCenterX(mLayoutToggle);
        mToggleY = getViewY(mLayoutToggle);//ViewUtils.getCenterY(mLayoutToggle);
    }


    public void turnOff() {
        mToggleX = getViewX(mLayoutToggle); //ViewUtils.getCenterX(mLayoutToggle);
        mToggleY = getViewY(mLayoutToggle);//ViewUtils.getCenterY(mLayoutToggle);
    }

    public AnimatorSet createOnAnimator(Animator... animators) {
        ArrayList<Animator> list = new ArrayList<>(Arrays.asList(animators));
        list.add(0, animateOutwards(mHomeButton, 200));

        Animator[] animatorArray = new Animator[list.size()];
        list.toArray(animatorArray);

        AnimatorSet animatorSet = new AnimatorSet();

        animatorSet.playSequentially(
                AnimatorUtils.fadeIn(mLayout).setDuration(100),
                AnimatorUtils.together(new OvershootInterpolator(), animatorArray),
                fadeIn(mHomeLabel, 200)
        );

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLayoutToggle.setSelected(true);
            }
        });

        return animatorSet;
    }

    public AnimatorSet createOffAnimator(Animator... animators) {
        AnimatorSet animatorSet = new AnimatorSet();

        ArrayList<Animator> list = new ArrayList<>(Arrays.asList(animators));
        list.add(animateInwards(mHomeButton, 200));

        Animator[] animatorArray = new Animator[list.size()];
        list.toArray(animatorArray);

        animatorSet.playSequentially(
                fadeOut(mHomeLabel, 200),
                AnimatorUtils.together(new AnticipateInterpolator(), animatorArray),
                AnimatorUtils.fadeOut(mLayout).setDuration(100)
        );

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLayoutToggle.setSelected(false);
                ViewUtils.setInvisible(mLayout);
                AnimatorUtils.reset(mHomeButton);
            }
        });

        return animatorSet;
    }


    protected Animator fadeIn(View view, int duration) {
        return AnimatorUtils.fadeIn(
                view,
                new DecelerateInterpolator()
        ).setDuration(duration);
    }

    protected Animator fadeOut(View view, int duration) {
        return AnimatorUtils.fadeOut(
                view,
                new DecelerateInterpolator()
        ).setDuration(duration);
    }

    protected Animator animateOutwards(View view, int duration) {
        float posX = mToggleX - getViewX(view);
        float posY = mToggleY - getViewY(view);

        view.setTranslationX(posX);
        view.setTranslationY(posY);
        view.setAlpha(0f);
        view.setScaleX(0f);
        view.setScaleY(0f);

        return AnimatorUtils.of(
                view,
                AnimatorUtils.ofTranslationX(posX, 0f),
                AnimatorUtils.ofTranslationY(posY, 0f),
                AnimatorUtils.ofAlpha(0f, 1f),
                AnimatorUtils.ofScaleX(0f, 1f),
                AnimatorUtils.ofScaleY(0f, 1f)
        ).setDuration(duration);
    }

    protected Animator animateInwards(View view, int duration) {
        float posX = mToggleX - getViewX(view);
        float posY = mToggleY - getViewY(view);

        return AnimatorUtils.of(
                view,
                AnimatorUtils.ofTranslationX(0f, posX),
                AnimatorUtils.ofTranslationY(0f, posY),
                AnimatorUtils.ofAlpha(1f, 0f),
                AnimatorUtils.ofScaleX(1f, 0f),
                AnimatorUtils.ofScaleY(1f, 0f)
        ).setDuration(duration);
    }

    protected int getViewX(View view) {
        int pos[] = new int[2];
        view.getLocationInWindow(pos);
        return pos[0];
    }

    protected int getViewY(View view) {
        int pos[] = new int[2];
        view.getLocationOnScreen(pos);
        return pos[1];
    }

    public boolean canTurnOff() {
        return mLayoutToggle.isSelected();
    }

}