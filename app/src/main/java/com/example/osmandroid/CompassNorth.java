package com.example.osmandroid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.MotionEvent;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.IOrientationProvider;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;

public class CompassNorth extends CompassOverlay {
    private final static float COMPASS_POSITION_X = 350.0f;
    private final static float COMPASS_POSITION_Y = 400.0f;
    private final static float COMPASS_RADIUS     = 20.0f;
    private boolean mNorthMode = false;
    protected Bitmap mCompassNorthBitmap;   // picture of compass north mode
    protected Bitmap mCompassConventionalBitmap;    // picture of compass conventional mode

    // ===========================================================
    // Constructors
    // ===========================================================
    public CompassNorth(Context context, MapView mapView) {
        this(context, new InternalCompassOrientationProvider(context), mapView);
    }


    public CompassNorth(Context context, IOrientationProvider orientationProvider, MapView mapView) {
        super(context, orientationProvider, mapView);

        // test if pointer mode on class CompassOverlay
        if( super.isPointerMode() ) {  super.setPointerMode(false);    }

        // save pictures of conventional compass on bitmap
        mCompassConventionalBitmap = mCompassRoseBitmap;

        mCompassRoseBitmap = null;
        createCompassNorthPicture();    // create picture of north compass
        mCompassNorthBitmap = mCompassRoseBitmap;   //  save picture of north compass on bitmap
        mCompassRoseBitmap = mCompassConventionalBitmap;    //  take picture of conventional compass
    }


    @Override
    public void onDetach(MapView mapView) {
        mCompassRoseBitmap = null;
        mCompassNorthBitmap.recycle();
        mCompassConventionalBitmap.recycle();
        super.onDetach(mapView);
    }


    // ===========================================================
    // Getter & Setter
    // ===========================================================

    /**
     * Do nothing on CompassNorth
     * The compass can operate in two modes.
     * <ul>
     * <li>false - a conventional compass needle pointing north/south (false, default)</li>
     * <li>true - a pointer arrow that indicates the device's real world orientation on the map (true)</li>
     * </ul>
     * A different picture is used in each case.
     * @since 6.0.0
     * @param usePointArrow if true the pointer arrow is used, otherwise a compass rose is used
     */
    @Deprecated
    @Override
    public void setPointerMode(boolean usePointArrow) {}


    /**
     * The North Compass return always false
     * @since 6.0.0
     * @return true if we are in pointer mode, instead of compass mode
     */
    @Deprecated
    @Override
    public boolean isPointerMode() {    return false;   }


    /**
     * @since 6.0.0
     * @return true if we are in north mode, instead of compass mode
     */
    public boolean isNorthMode () { return mNorthMode;  }


    /**
     * The compass can operate in two modes.
     * <ul>
     * <li>true - a north pointer compass needle pointing north/south (false, default)</li>
     * <li>false - a conventional compass that indicates the device's real world orientation on the map (true)</li>
     * </ul>
     * A different picture is used in each case.
     * @since 6.0.0
     * @param northMode if true the north pointer is used, otherwise a conventional compass is used
     */
    public void setNorthMode(boolean northMode) {
        if(northMode == mNorthMode) return;
        mNorthMode = northMode;
        if(mNorthMode) {
            mCompassRoseBitmap = mCompassNorthBitmap;
            onOrientationChanged(0f, null);
        }
        else {
            mCompassRoseBitmap = mCompassConventionalBitmap;
            onOrientationChanged(getOrientation(),null);
        }
    }


    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView) {
        // toggle mode
        setNorthMode( !mNorthMode );
        return true;
    }


    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    private void createCompassNorthPicture() {
        // radius of Compass, taker this given value on class CompassOverlay from class MainActivity
        final float radius = MainActivity.COMPASS_RADIUS;
        // Paint design of north triangle (it's common to paint north in red color)
        final Paint northPaint = new Paint();
        northPaint.setColor(0xFFA00000);
        northPaint.setAntiAlias(true);
        northPaint.setStyle(Paint.Style.FILL);
        northPaint.setAlpha(220);

        // Paint design of character N (black)
        final Paint characterNPaint = new Paint();
        characterNPaint.setColor(Color.BLACK);
        characterNPaint.setAntiAlias(true);
        characterNPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        characterNPaint.setTextSize(radius / 1.5f * mScale);
        characterNPaint.setStrokeWidth(2f);
        characterNPaint.setAlpha(220);
        characterNPaint.setTextAlign(Paint.Align.CENTER);

        // Create a little white dot in the middle of the compass rose
        final Paint centerPaint = new Paint();
        centerPaint.setColor(Color.WHITE);
        centerPaint.setAntiAlias(true);
        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setAlpha(220);

        final int picBorderWidthAndHeight = (int) ((radius + 5) * 2 * mScale);
        final int center = picBorderWidthAndHeight / 2;

        if (mCompassRoseBitmap != null)
            mCompassRoseBitmap.recycle();
        mCompassRoseBitmap = Bitmap.createBitmap(picBorderWidthAndHeight, picBorderWidthAndHeight,
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(mCompassRoseBitmap);

        // Triangle pointing north
        final Path pathNorth = new Path();
        pathNorth.moveTo(center, center - (radius - 3) * mScale);
        pathNorth.lineTo(center + (4 * mScale), center);
        pathNorth.lineTo(center - (4 * mScale), center);
        pathNorth.lineTo(center, center - (radius - 3) * mScale);
        pathNorth.close();
        canvas.drawPath(pathNorth, northPaint);

        // Character N south
        Rect rectN = new Rect();
        final String textN = "N";
        characterNPaint.getTextBounds(textN, 0, 1, rectN);
        final int height = rectN.height();
        canvas.drawText(textN, 0, 1, center,
                center + (radius / 2) + height, characterNPaint);

        // Draw a little white dot in the middle
        canvas.drawCircle(center, center, 2, centerPaint);
    }
}
