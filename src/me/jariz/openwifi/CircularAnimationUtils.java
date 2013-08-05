package me.jariz.openwifi;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import de.passsy.holocircularprogressbar.HoloCircularProgressBar;

/**
 * JARIZ.PRO
 * Created @ 04/08/13
 * By: JariZ
 * Project: OpenWiFi
 * Package: me.jariz.openwifi
 */
public class CircularAnimationUtils {

    String TAG = "OpenWiFi_CircularAnimationUtils";

    public static void fillProgressbar(final long duration, final HoloCircularProgressBar holo) {
        final ObjectAnimator progressBarAnimator = ObjectAnimator.ofFloat(holo, "progress", 1f);
        progressBarAnimator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationCancel(final Animator animation) {
            }

            @Override
            public void onAnimationEnd(final Animator animation) {
                holo.setProgress(1f);
            }

            @Override
            public void onAnimationRepeat(final Animator animation) {
            }

            @Override
            public void onAnimationStart(final Animator animation) {
            }
        });

        progressBarAnimator.setDuration(duration);
        progressBarAnimator.reverse();
        progressBarAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(final ValueAnimator animation) {
                holo.setProgress((Float) animation.getAnimatedValue());
            }
        });
        progressBarAnimator.start();
    }

    static boolean pulseDirection = true;
    static int pulseTimes;
    //overrides pulseTimes
    static boolean pulseKeep = false;

    public static void pulse(final int times, final HoloCircularProgressBar holo, final long duration) {
        CircularAnimationUtils.pulseTimes = times;

        pulseInternal(holo, true, new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                newPulse(holo,duration);
            }

            @Override
            public void onAnimationEnd(Animator animator) {

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        }, duration);
    }

    static void newPulse(final HoloCircularProgressBar holo, final long duration) {
        if(CircularAnimationUtils.pulseKeep && CircularAnimationUtils.pulseDirection || CircularAnimationUtils.pulseTimes != 0) {
            pulseInternal(holo, CircularAnimationUtils.pulseDirection, new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    newPulse(holo,duration);
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            }, duration);
            CircularAnimationUtils.pulseDirection = !CircularAnimationUtils.pulseDirection;
            if(!CircularAnimationUtils.pulseKeep) CircularAnimationUtils.pulseTimes--;
        }
    }

    private static void pulseInternal(final HoloCircularProgressBar holo, final boolean direction, final Animator.AnimatorListener listener, final long duration) {
        final ObjectAnimator progressBarAnimator = ObjectAnimator.ofFloat(holo, "alpha", direction ? 1f : 0f);
        progressBarAnimator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationCancel(final Animator animation) {
            }

            @Override
            public void onAnimationEnd(final Animator animation) {
                holo.setAlpha(direction ? 1f : 0f);
            }

            @Override
            public void onAnimationRepeat(final Animator animation) {
            }

            @Override
            public void onAnimationStart(final Animator animation) {
            }
        });

        progressBarAnimator.addListener(listener);
        progressBarAnimator.setDuration(duration);
        progressBarAnimator.reverse();
        progressBarAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(final ValueAnimator animation) {
                holo.setAlpha((Float) animation.getAnimatedValue());
            }
        });
        progressBarAnimator.start();
    }
}
