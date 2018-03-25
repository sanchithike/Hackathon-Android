package com.roposo.core.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.SimpleArrayMap;

import com.roposo.core.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by rahul on 7/11/14.
 */

public class ContextHelper {

    // Refactor:: no need to make them static, basically remove them
    public static volatile Context applicationContext;
    public static volatile Handler applicationHandler;
    private static volatile Context context;

    private static volatile SimpleArrayMap<Integer, ActionListener> onResultCallbacks;


    private ContextHelper() {
    }

    public static Context getContext() {
        return context;
    }

    public static void setContext(final Context context1) {
        if (context1 == null) return;
        context = context1;
    }

    public static void setApplicationContext(final Context context) {
        applicationContext = context;
        applicationHandler = new Handler(Looper.getMainLooper());
    }

    public static ContentResolver getContentResolver() {
        return applicationContext.getContentResolver();
    }

    public static SimpleArrayMap<Integer, ActionListener> getOnResultCallback() {
        return onResultCallbacks;
    }

    public static void setOnResultCallback(SimpleArrayMap<Integer, ActionListener> map) {
        onResultCallbacks = map;
    }

    @IntDef({
            TRANSITION_ANIMATION_NONE,
            TRANSITION_ANIMATION_FROM_RIGHT,
            TRANSITION_ANIMATION_FROM_BOTTOM,
            TRANSITION_ANIMATION_FADE_OUT,
            TRANSITION_ANIMATION_FROM_BOTTOM_WITH_FADE_IN,
            TRANSITION_ANIMATION_FROM_RIGHT_EXIT_FROM_RIGHT,
            TRANSITION_ANIMATION_FROM_LEFT,
            TRANSITION_ANIMATION_FROM_TOP
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FragmentTransitionAnimationGroup {
    }

    public static final int TRANSITION_ANIMATION_NONE = -1;
    public static final int TRANSITION_ANIMATION_FROM_RIGHT = 0;
    public static final int TRANSITION_ANIMATION_FROM_BOTTOM = 1;
    public static final int TRANSITION_ANIMATION_FADE_OUT = 2;
    public static final int TRANSITION_ANIMATION_FROM_BOTTOM_WITH_FADE_IN = 3;
    public static final int TRANSITION_ANIMATION_FROM_RIGHT_EXIT_FROM_RIGHT = 4;
    public static final int TRANSITION_ANIMATION_FROM_LEFT = 5;
    public static final int TRANSITION_ANIMATION_FROM_TOP = 6;

    private static void setCustomAnimation(FragmentTransaction transaction,
                                           @FragmentTransitionAnimationGroup int animationGroup) {
        switch (animationGroup) {
            case TRANSITION_ANIMATION_FROM_BOTTOM:
                transaction.setCustomAnimations(R.anim.enter_from_bottom, R.anim.exit_from_top,
                        R.anim.enter_from_top, R.anim.exit_from_bottom);
                break;
            case TRANSITION_ANIMATION_FROM_RIGHT:
                transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left,
                        R.anim.enter_from_left, R.anim.exit_to_right);
                break;
            case TRANSITION_ANIMATION_FROM_BOTTOM_WITH_FADE_IN:
                transaction.setCustomAnimations(R.anim.enter_from_bottom, R.anim.fade_out_fragment,
                        R.anim.fade_in_fragment, R.anim.exit_from_bottom);
                break;
            case TRANSITION_ANIMATION_FROM_RIGHT_EXIT_FROM_RIGHT:
                transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left,
                        R.anim.enter_from_right, R.anim.exit_to_left);
            case TRANSITION_ANIMATION_NONE:
                break;
            case TRANSITION_ANIMATION_FADE_OUT:
                transaction.setCustomAnimations(R.anim.fade_in_fragment, R.anim.fade_out_fragment,
                        0, R.anim.fade_out_fragment);
                break;
            case TRANSITION_ANIMATION_FROM_LEFT:
                transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right,
                        R.anim.enter_from_right, R.anim.exit_to_left);
                break;
            case TRANSITION_ANIMATION_FROM_TOP:
                transaction.setCustomAnimations(R.anim.enter_from_top, R.anim.exit_from_top,
                        R.anim.enter_from_top, R.anim.exit_from_top);
                break;
        }
    }

    public static void startActivity(Context thiz, Class targetActivity, Bundle data) {
        if (null != thiz) {
            Intent intent = new Intent(thiz, targetActivity);
            intent.putExtras(data);
            thiz.startActivity(intent);
        }
    }

    public static void killFragmentAtBottom() {
        if (getContext() instanceof FragmentActivity) {
            FragmentManager fragmentManager = ((FragmentActivity) getContext()).getSupportFragmentManager();
            int count = fragmentManager.getBackStackEntryCount();

            if (count > 2) { // condition can be changed to count >0
                for (int i = 0; i < count - 3; i++) {
                    String bottomFragmentName = fragmentManager.getBackStackEntryAt(i + 1).getName();
                    Fragment bottomFragment = fragmentManager.findFragmentByTag(bottomFragmentName);
                    if (bottomFragment == null) {
                        return;
                    }
                }
            }
        }
    }

    public static void openFragmentWithTag(Fragment fragment, String tag, boolean animate) {
        FragmentManager mFragmentManager = ((FragmentActivity) ContextHelper.getContext()).getSupportFragmentManager();
        if (mFragmentManager != null) {
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            if (animate) {
                transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right
                        , R.anim.enter_from_right, R.anim.exit_to_left);
            }
            transaction.add(android.R.id.content, fragment, tag)
                    .addToBackStack(tag).commitAllowingStateLoss();
        }
    }
}
