package com.example.osmandroid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;

import androidx.annotation.NonNull;

import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

public class SpeederOverlay extends Overlay implements LocationListener {

    public final static int NO_SPEED = -1;
    private int mSpeed;
    private int mMaxSpeed;
    private final MapView mMapView;
    private final Paint sSmoothPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    protected Bitmap mSpeederFramePicture;
    private boolean mIsSpeederEnabled;
    private boolean mInCenter = false;
    private final float mSpeederFrameCenterX;
    private final float mSpeederFrameCenterY;
    private final Matrix mSpeederMatrix = new Matrix();
    private float mSpeederCenterX;
    private float mSpeederCenterY;
    private final float mSpeederRadius = 30.0f;
    private final float mSpeedCenterX;
    private final float mSpeedCenterY;
    private final float mLimitedSpeedCenterX;
    private final float mLimitedSpeedCenterY;
    protected long mLastRender = 0L;
    protected final float mScale;


    // ===========================================================
    // Constructors
    // ===========================================================

    SpeederOverlay(Context context, MapView map) {
        super();
        mMapView = map;
        mScale = context.getResources().getDisplayMetrics().density;

        // create picture of Speeder Frame
        createSpeederFramePicture();
        float mSpeedRadius = mSpeederRadius * mScale;
        float mLimitedSpeedRadius = mSpeedRadius / 1.3f;
        mSpeedCenterX = mSpeedRadius;
        mSpeedCenterY = mSpeedRadius + mLimitedSpeedRadius;
        mLimitedSpeedCenterX = mSpeedCenterX + mLimitedSpeedRadius;
        mLimitedSpeedCenterY = mSpeedCenterY - mLimitedSpeedRadius;
        mSpeederFrameCenterX = mSpeederFramePicture.getWidth() / 2f;
        mSpeederFrameCenterY = mSpeederFramePicture.getHeight() /2f;

        mSpeed = SpeederOverlay.NO_SPEED;
        mMaxSpeed = SpeederOverlay.NO_SPEED;
    }


    // ===========================================================
    // Life Cycle
    // ===========================================================

    @Override
    public void onPause() {
        super.onPause();
    }


    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onDetach(MapView mapView) {
        if (this.mSpeederFramePicture !=null) {
            this.mSpeederFramePicture.recycle();
        }
        super.onDetach(mapView);
    }


    // ===========================================================
    // Getter & Setter
    // ===========================================================

    public float getSpeederCenterX() {
        return mSpeederCenterX;
    }


    public float getSpeederCenterY() {
        return mSpeederCenterY;
    }


    public boolean isSpeederEnabled () {
        return mIsSpeederEnabled;
    }


    public void setSpeederCenter (float centerX, float centerY) {
        mSpeederCenterX = centerX;
        mSpeederCenterY = centerY;
    }


    public boolean enableSpeeder() {
        if (mIsSpeederEnabled) return true;

        mIsSpeederEnabled = true;

        // Update the screen to see changes take effect
        if (mMapView != null) {
            this.invalidateSpeeder();
        }
        return true;
    }


    public boolean disableSpeeder() {
        if ( !mIsSpeederEnabled ) return true;
        mIsSpeederEnabled = false;

        // Reset values
        mSpeed = SpeederOverlay.NO_SPEED;
        mMaxSpeed = SpeederOverlay.NO_SPEED;

        // Update the screen to see changes take effect
        if (mMapView != null) {
            this.invalidateSpeeder();
        }
        return true;
    }


    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================

    @Override
    public void draw(Canvas c, MapView mapView, boolean shadow) {
        if (shadow) {
            return;
        }
        if (isSpeederEnabled()) {
            drawSpeeder(c, mSpeed, mMaxSpeed, mapView.getProjection().getScreenRect());
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        int speed = (int)(location.getSpeed() * 3.6f + 0.5f);
        if(location.hasSpeed()) {
            if(mSpeed == speed) return;
            mSpeed = speed;
        }
    }


    // ===========================================================
    // Methods
    // ===========================================================

    public void onSpeedChanged (int speed) {
        onAllSpeedChanged(speed, mMaxSpeed);
    }

    public void onMaxSpeedChanged (int maxSpeed) {
        onAllSpeedChanged(mSpeed, maxSpeed);
    }

    public void onAllSpeedChanged (int speed, int maxSpeed) {
        if(mSpeed == speed && mMaxSpeed == maxSpeed)    return;
        mSpeed = speed;
        mMaxSpeed = maxSpeed;
        this.invalidateSpeeder();
    }


    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================


    private void createSpeederFramePicture() {
        // The inside of the speed circle is black and transparent
        final Paint innerSpeedPaint = new Paint();
        innerSpeedPaint.setColor(Color.BLACK);
        innerSpeedPaint.setAntiAlias(true);
        innerSpeedPaint.setStyle(Paint.Style.FILL);
        innerSpeedPaint.setAlpha(200);

        // The outer of the speed circle is gray and transparent
        final Paint outerSpeedPaint = new Paint();
        outerSpeedPaint.setColor(Color.GRAY);
        outerSpeedPaint.setAntiAlias(true);
        outerSpeedPaint.setStyle(Paint.Style.STROKE);
        outerSpeedPaint.setStrokeWidth(3.0f * mScale);
        outerSpeedPaint.setAlpha(200);

        //  Calculation radius of Speed, limitedSpeed
        final float radiusSpeed  = mSpeederRadius * mScale;
        final float radiusLimitedSpeed = radiusSpeed / 1.3f;

        //  Calculation center of Speed, limitedSpeed
        final float speedCenterX = radiusSpeed;//((mSpeederRadius + 3f) * mScale);
        final float speedCenterY = radiusSpeed + radiusLimitedSpeed;
        final float limitedSpeedCenterX = speedCenterX + radiusSpeed;
        final float limitedSpeedCenterY = speedCenterY - radiusSpeed;

        //  Calculation Width and Height of Picture
        final int picBorderWidthAndHeight = (int)((radiusSpeed * 2f) + radiusLimitedSpeed);

        //  Create SpeederFrameBitmap
        if (mSpeederFramePicture != null)
            mSpeederFramePicture.recycle();
        mSpeederFramePicture = Bitmap.createBitmap(picBorderWidthAndHeight, picBorderWidthAndHeight, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(mSpeederFramePicture);

        // draw speed inner and border
        canvas.drawCircle(speedCenterX, speedCenterY, radiusSpeed, innerSpeedPaint);
        canvas.drawCircle(speedCenterX, speedCenterY, radiusSpeed - (5f * mScale), outerSpeedPaint);

        // The inside of the limited speed is white and transparent
        final Paint innerLimitedSpeedPaint = new Paint();
        innerLimitedSpeedPaint.setColor(Color.WHITE);
        innerLimitedSpeedPaint.setAntiAlias(true);
        innerLimitedSpeedPaint.setStyle(Paint.Style.FILL);
        innerLimitedSpeedPaint.setAlpha(200);

        // The outer part circle is red and transparent
        final Paint outerLimitedSpeedPaint = new Paint();
        outerLimitedSpeedPaint.setColor(Color.RED);
        outerLimitedSpeedPaint.setAntiAlias(true);
        outerLimitedSpeedPaint.setStyle(Paint.Style.STROKE);
        outerLimitedSpeedPaint.setStrokeWidth(5.0f * mScale);
        outerLimitedSpeedPaint.setAlpha(200);

        // draw limited speed inner and border
        canvas.drawCircle(limitedSpeedCenterX, limitedSpeedCenterY, radiusLimitedSpeed, innerLimitedSpeedPaint);
        canvas.drawCircle(limitedSpeedCenterX, limitedSpeedCenterY, radiusLimitedSpeed - (5f * mScale), outerLimitedSpeedPaint);

        String textUnitSpeed = "km/h";

        // unit speed is white and transparent
        final Paint unitSpeedPaint = new Paint();
        unitSpeedPaint.setColor(Color.WHITE);
        unitSpeedPaint.setAntiAlias(true);
        unitSpeedPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        unitSpeedPaint.setStrokeWidth(2.0f);
        unitSpeedPaint.setTextSize(radiusSpeed / 4f);
        unitSpeedPaint.setTextAlign(Paint.Align.CENTER);

        // draw unit speed
        canvas.drawText(textUnitSpeed, 0, textUnitSpeed.length(), speedCenterX, speedCenterY + radiusSpeed /2f, unitSpeedPaint);
    }


    private void drawSpeeder(final Canvas canvas, final int speed, final int limitedSpeed, final Rect screenRect) {
        final Projection proj = mMapView.getProjection();

        float centerX;
        float centerY;

        if (false) {
            centerX = screenRect.exactCenterX();
            centerY = screenRect.exactCenterY();
        } else {
            centerX = mSpeederCenterX * mScale;
            centerY = mSpeederCenterY * mScale;
        }

        // draw SpeederFramePicture
        mSpeederMatrix.setTranslate(-mSpeederFrameCenterX, - mSpeederFrameCenterY);
        mSpeederMatrix.postTranslate(centerX, centerY);

        proj.save(canvas, false, true);
        canvas.concat(mSpeederMatrix);
        canvas.drawBitmap(mSpeederFramePicture, 0, 0, sSmoothPaint);
        proj.restore(canvas, true);

        //  SpeedPaint
        final Paint speedPaint = new Paint();
        speedPaint.setColor(Color.WHITE);
        speedPaint.setAntiAlias(true);
        speedPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        speedPaint.setStrokeWidth(2.0f);
        speedPaint.setTextSize(mSpeederRadius * mScale * 0.7f);
        speedPaint.setTextAlign(Paint.Align.CENTER);

        //  SpeedPaint
        final Paint limitedSpeedPaint = new Paint();
        limitedSpeedPaint.setColor(Color.BLACK);
        limitedSpeedPaint.setAntiAlias(true);
        limitedSpeedPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        limitedSpeedPaint.setStrokeWidth(3.0f);
        limitedSpeedPaint.setTextSize(speedPaint.getTextSize() / 1.3f);
        limitedSpeedPaint.setTextAlign(Paint.Align.CENTER);

        //  textSpeed
        String textSpeed = (speed == SpeederOverlay.NO_SPEED) ? "--" : (Integer.toString(speed));
        //textSpeed = Integer.toString(99);
        // draw Speed
        mSpeederMatrix.setTranslate(centerX, centerY);
        mSpeederMatrix.postTranslate(-mSpeederFrameCenterX, -mSpeederFrameCenterY);
        mSpeederMatrix.postTranslate(mSpeedCenterX, mSpeedCenterY);
        proj.save(canvas, false, true);
        canvas.concat(mSpeederMatrix);
        canvas.drawText(textSpeed, 0, speedPaint.getTextSize() / 4, speedPaint);
        proj.restore(canvas, true);

        //  textLimitedSpeed
        String textLimitedSpeed = (limitedSpeed == SpeederOverlay.NO_SPEED) ? "--" : (Integer.toString(limitedSpeed));
        //textLimitedSpeed = Integer.toString(99);
        // draw Speed
        mSpeederMatrix.setTranslate(centerX, centerY);
        mSpeederMatrix.postTranslate(-mSpeederFrameCenterX, -mSpeederFrameCenterY);
        mSpeederMatrix.postTranslate(mLimitedSpeedCenterX, mLimitedSpeedCenterY);
        proj.save(canvas, false, true);
        canvas.concat(mSpeederMatrix);
        canvas.drawText(textLimitedSpeed, 15, 0, limitedSpeedPaint);
        proj.restore(canvas, true);
    }


    public void invalidateSpeeder () {
        if (mLastRender + 500 > System.currentTimeMillis())
            return;
        mLastRender = System.currentTimeMillis();
        Rect screenRect = mMapView.getProjection().getScreenRect();
        int frameLeft;
        int frameRight;
        int frameTop;
        int frameBottom;
        float speederFrameCenter = mSpeederRadius - 0.5f;
        frameLeft = screenRect.left
                + (int) Math.ceil((mSpeederCenterX - speederFrameCenter) * mScale);
        frameTop = screenRect.top
                + (int) Math.ceil((mSpeederCenterY - speederFrameCenter) * mScale);
        frameRight = screenRect.left
                + (int) Math.ceil((mSpeederCenterX + speederFrameCenter) * mScale);
        frameBottom = screenRect.top
                + (int) Math.ceil((mSpeederCenterY + speederFrameCenter) * mScale);

        // Expand by 2 to cover stroke width
        mMapView.postInvalidateMapCoordinates(frameLeft - 2, frameTop - 2, frameRight + 2,
                frameBottom + 2);
    }
}
