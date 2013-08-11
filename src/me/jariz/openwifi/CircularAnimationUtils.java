package me.jariz.openwifi;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.util.Log;
import de.passsy.holocircularprogressbar.HoloCircularProgressBar;

/**
 * JARIZ.PRO
 * Created @ 04/08/13
 * By: JariZ
 * Project: OpenWiFi
 * Package: me.jariz.openwifi
 */
public class CircularAnimationUtils {

    static String TAG = "OW_ANIMATION";
    static ObjectAnimator progressBarAnimator;

    public static void fillProgressbar(final long duration, final HoloCircularProgressBar holo) {
        if (progressBarAnimator != null) {
            if(progressBarAnimator.isRunning()) {
                progressBarAnimator.end();
                progressBarAnimator.cancel();
            }


        }
        progressBarAnimator = ObjectAnimator.ofFloat(holo, "progress", 1f);
        progressBarAnimator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationCancel(final Animator animation) {
                holo.setProgress(1f);
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
    static boolean pulseKeep = false;
    static boolean pulseRunning = false;

    public static void pulse(final HoloCircularProgressBar holo, final long duration) {
        if(pulseRunning) {
            Log.i(TAG, "Tried to start a pulse while there already is one running");
            return;
        }
        pulseRunning = true;
        pulseInternal(holo, pulseDirection, new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                newPulse(holo,duration);
            }

            @Override
            public void onAnimationEnd(Animator animator) {

            }

            @Override
            public void onAnimationCancel(Animator animator) {
                holo.setAlpha(pulseDirection ? 1f : 0f);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        }, duration);
    }

    static void newPulse(final HoloCircularProgressBar holo, final long duration) {
        if(CircularAnimationUtils.pulseKeep) {
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
                    holo.setAlpha(pulseDirection ? 1f : 0f);
                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            }, duration);
            pulseDirection = !pulseDirection;
        } else {
            
            pulseRunning = false;
        }
    }

    static ObjectAnimator pulseAnimator;
    private static void pulseInternal(final HoloCircularProgressBar holo, final boolean direction, final Animator.AnimatorListener listener, final long duration) {
        if(pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        pulseAnimator = ObjectAnimator.ofFloat(holo, "alpha", direction ? 1f : 0f);
        pulseAnimator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationCancel(final Animator animation) {
                holo.setAlpha(direction ? 1f : 0f);
            }

            @Override
            public void onAnimationEnd(final Animator animation) {
                if(!pulseKeep) {
                    holo.setAlpha(1f);
                    pulseDirection = true;
                } else
                    holo.setAlpha(direction ? 1f : 0f);
            }

            @Override
            public void onAnimationRepeat(final Animator animation) {
            }

            @Override
            public void onAnimationStart(final Animator animation) {
            }
        });

        pulseAnimator.addListener(listener);
        pulseAnimator.setDuration(duration);
        pulseAnimator.reverse();

        pulseAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(final ValueAnimator animation) {
                if(pulseKeep)
                    holo.setAlpha((Float) animation.getAnimatedValue());
                else //'reverse' the animation
                    holo.setAlpha(1f - (Float) animation.getAnimatedValue() );
            }
        });
        pulseAnimator.start();
    }
}
