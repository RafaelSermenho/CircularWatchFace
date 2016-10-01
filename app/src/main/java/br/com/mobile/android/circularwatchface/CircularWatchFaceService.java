package br.com.mobile.android.circularwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class CircularWatchFaceService extends CanvasWatchFaceService {
    private static final int MSG_UPDATE_TIME = 0;
    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);

    private Calendar mCalendar;

    // device features
    private boolean mLowBitAmbient;

    // graphic objects
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    private Paint mMinutesPaint;
    private float mCenterX;
    private float mCenterY;
    private int mChinSize;
    private Paint mTextPaint;
    private boolean mAmbient;
    private final Rect textBounds = new Rect();
    private boolean mIsRound;
    private boolean is24hFormat;

    @Override
    public Engine onCreateEngine() {
        /* provide your watch face implementation */
        return new Engine();
    }

    /* implement service callback methods */
    private class Engine extends CanvasWatchFaceService.Engine {
        private boolean mRegisteredTimeZoneReceiver;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            Resources resources = CircularWatchFaceService.this.getResources();

            mMinutesPaint = new Paint();
            mMinutesPaint.setAntiAlias(true);
            mMinutesPaint.setStyle(Paint.Style.STROKE);
            mMinutesPaint.setStrokeWidth(5);
            mMinutesPaint.setColor(resources.getColor(R.color.dawn));

            mTextPaint = new Paint();
            mTextPaint.setColor(resources.getColor(R.color.dawn));
            mTextPaint.setTypeface(NORMAL_TYPEFACE);
            mTextPaint.setAntiAlias(true);

            is24hFormat = DateFormat.is24HourFormat(CircularWatchFaceService.this);

            // allocate a Calendar to calculate local time using the UTC time and time zone
            mCalendar = Calendar.getInstance();

            setWatchFaceStyle(new WatchFaceStyle.Builder(CircularWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle
                            .BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setHideHotwordIndicator(false)
                    .setHideStatusBar(false)
                    .build());
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            /* the time changed */
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {

            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                    mMinutesPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Update the time
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            int minutes = mCalendar.get(Calendar.MINUTE);
            int hour = mCalendar.get(Calendar.HOUR_OF_DAY);
            if (hour >= 6 && hour < 13 ){
                mTextPaint.setColor(getResources().getColor(R.color.dawn));
                mMinutesPaint.setColor(getResources().getColor(R.color.dawn));
            } else if (hour >= 13 && hour < 18){
                mTextPaint.setColor(getResources().getColor(R.color.afternoon));
                mMinutesPaint.setColor(getResources().getColor(R.color.afternoon));
            } else {
                mTextPaint.setColor(getResources().getColor(R.color.night));
                mMinutesPaint.setColor(getResources().getColor(R.color.night));
            }
            int minRot = (minutes * 360) / 60;
            // Draw the background.
            if (minutes == 0) {
                minRot = 360;
            }

            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            if (mIsRound) {
                canvas.drawArc(bounds.left + mChinSize, bounds.top + mChinSize, bounds.right - mChinSize, bounds.bottom - mChinSize, -90, minRot, false, mMinutesPaint);
            } else {
                canvas.drawArc(bounds.left + 40, bounds.top  + 40, bounds.right - 40 , bounds.bottom - 40, -90, minRot, false, mMinutesPaint);
            }
//            if (minutes == 0) {
//                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
//            }
//            if (shouldClear){
//                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
//                canvas.drawArc(bounds.left + mChinSize, bounds.top + mChinSize, bounds.right - mChinSize, bounds.bottom - mChinSize, -90, minRot, false, mMinutesPaint);
//            }
            String text;
            if (is24hFormat) {
                text = String.format(Locale.getDefault(), "%d", mCalendar.get(Calendar.HOUR_OF_DAY));
            } else {
                text = String.format(Locale.getDefault(), "%d", mCalendar.get(Calendar.HOUR));
            }
            mTextPaint.getTextBounds(text, 0, text.length(), textBounds);
            canvas.drawText(text, mCenterX - textBounds.exactCenterX(), mCenterY - textBounds.exactCenterY(), mTextPaint);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
            } else {
                unregisterReceiver();

            }

            // Whether the timer should be running depends on whether we're visible and
            // whether we're in ambient mode, so we may need to start or stop the timer
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED );
            CircularWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            CircularWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        // handler to update the time once a second in interactive mode
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler
                                    .sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };


        // receiver to update the time zone
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        @Override
        public void onSurfaceChanged(
                SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            int mWidth = width;
            int mHeight = height;
            /*
             * Find the coordinates of the center point on the screen.
             * Ignore the window insets so that, on round watches
             * with a "chin", the watch face is centered on the entire screen,
             * not just the usable portion.
             */
            mCenterX = mWidth / 2f;
            mCenterY = mHeight / 2f;

        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            mIsRound = insets.isRound();
            mChinSize = insets.getSystemWindowInsetBottom();

            // Load resources that have alternate values for round watches.
            Resources resources = CircularWatchFaceService.this.getResources();
            float textSize = resources.getDimension(mIsRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mTextPaint.setTextSize(textSize);
        }
    }
}