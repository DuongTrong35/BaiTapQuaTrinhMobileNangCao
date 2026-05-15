package com.example.bai3.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Animation helper methods for button press effects, fade transitions,
 * pulse loading states, shake errors, and capability state transitions.
 */
public final class AnimationUtils {

    private AnimationUtils() {
        // Utility class
    }

    /**
     * Animate a button press: scale down on press, scale back on release.
     */
    public static void animateButtonPress(View view) {
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", Constants.BUTTON_SCALE_PRESSED);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", Constants.BUTTON_SCALE_PRESSED);
        scaleDownX.setDuration(Constants.ANIM_BUTTON_PRESS_MS / 2);
        scaleDownY.setDuration(Constants.ANIM_BUTTON_PRESS_MS / 2);

        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f);
        scaleUpX.setDuration(Constants.ANIM_BUTTON_PRESS_MS / 2);
        scaleUpY.setDuration(Constants.ANIM_BUTTON_PRESS_MS / 2);
        scaleUpX.setInterpolator(new OvershootInterpolator(2f));
        scaleUpY.setInterpolator(new OvershootInterpolator(2f));

        AnimatorSet press = new AnimatorSet();
        press.playTogether(scaleDownX, scaleDownY);

        AnimatorSet release = new AnimatorSet();
        release.playTogether(scaleUpX, scaleUpY);

        AnimatorSet full = new AnimatorSet();
        full.playSequentially(press, release);
        full.start();
    }

    /**
     * Flash a button briefly with cyan accent color to confirm command sent.
     */
    public static void animateCommandFlash(View view) {
        ObjectAnimator flash = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.5f, 1f);
        flash.setDuration(Constants.ANIM_FLASH_MS);
        flash.start();
    }

    /**
     * Shake animation for error / connection lost indicators.
     */
    public static void animateShake(View view) {
        ObjectAnimator shake = ObjectAnimator.ofFloat(view, "translationX",
                0, 10, -10, 10, -10, 5, -5, 0);
        shake.setDuration(400);
        shake.start();
    }

    /**
     * Repeating pulse animation for loading state buttons.
     * Alpha oscillates between 0.5 and 0.7.
     */
    public static ObjectAnimator startPulse(View view) {
        ObjectAnimator pulse = ObjectAnimator.ofFloat(view, "alpha",
                Constants.ALPHA_LOADING, 0.7f, Constants.ALPHA_LOADING);
        pulse.setDuration(Constants.ANIM_PULSE_DURATION_MS);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setRepeatMode(ValueAnimator.RESTART);
        pulse.start();
        return pulse;
    }

    /**
     * Stop a pulse animation and animate to final alpha.
     */
    public static void stopPulseAndSetAlpha(ObjectAnimator pulse, View view, float targetAlpha) {
        if (pulse != null && pulse.isRunning()) {
            pulse.cancel();
        }
        ObjectAnimator fade = ObjectAnimator.ofFloat(view, "alpha", targetAlpha);
        fade.setDuration(Constants.ANIM_CAPABILITY_FADE_MS);
        fade.start();
    }

    /**
     * Fade-in + slide-up animation for app launch.
     */
    public static void animateLaunchEntry(View view) {
        view.setAlpha(0f);
        view.setTranslationY(100f);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(view, "translationY", 100f, 0f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fadeIn, slideUp);
        set.setDuration(Constants.ANIM_LAUNCH_DURATION_MS);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.start();
    }

    /**
     * Fade a view to the specified alpha over the standard capability fade duration.
     */
    public static void fadeToAlpha(View view, float alpha) {
        ObjectAnimator fade = ObjectAnimator.ofFloat(view, "alpha", alpha);
        fade.setDuration(Constants.ANIM_CAPABILITY_FADE_MS);
        fade.start();
    }

    /**
     * Scale animation for touchpad tap feedback.
     */
    public static void animateTapFeedback(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(200);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.start();
    }

    /**
     * Fade in a view from invisible.
     */
    public static void fadeIn(View view, long duration) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        ObjectAnimator fade = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        fade.setDuration(duration);
        fade.start();
    }

    /**
     * Fade out a view and set it to GONE.
     */
    public static void fadeOut(View view, long duration) {
        ObjectAnimator fade = ObjectAnimator.ofFloat(view, "alpha", view.getAlpha(), 0f);
        fade.setDuration(duration);
        fade.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
            }
        });
        fade.start();
    }
}
