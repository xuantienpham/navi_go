package navi_go.view;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.widget.ImageView;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;

public class OriginPathOverlay extends Overlay {
    // ===========================================================
    // Constants
    // ===========================================================

    // ===========================================================
    // Fields
    // ===========================================================

    /**
     * Stores points, converted to the map projection.
     */
    private ArrayList<Point> mPoints;

    /**
     * Number of points that have precomputed values.
     */
    private int mPointsPrecomputed;

    /**
     * Paint settings.
     */
    protected Paint mPaint = new Paint();

    private final Path mPath = new Path();

    private final Point mTempPoint1 = new Point();
    private final Point mTempPoint2 = new Point();

    // bounding rectangle for the current line segment.
    private final Rect mLineBounds = new Rect();

    // ===========================================================
    // Constructors
    // ===========================================================

    public OriginPathOverlay() {
        this(Color.BLACK);
    }

    public OriginPathOverlay(final int color) {
        this(color, 2.0f);
    }

    public OriginPathOverlay(final int color, final float width) {
        super();
        this.mPaint.setColor(color);
        this.mPaint.setStrokeWidth(width);
        this.mPaint.setStyle(Paint.Style.STROKE);

        this.clearPath();
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    public void setColor(final int color) {
        this.mPaint.setColor(color);
    }

    public void setAlpha(final int a) {
        this.mPaint.setAlpha(a);
    }

    /**
     * Draw a great circle.
     * Calculate a point for every 100km along the path.
     * @param startPoint start point of the great circle
     * @param endPoint end point of the great circle
     */
    public void addGreatCircle(final GeoPoint startPoint, final GeoPoint endPoint) {
        //	get the great circle path length in meters
        final int greatCircleLength = (int) Math.round(startPoint.distanceToAsDouble(endPoint) + 0.5d);

        //	add one point for every 100kms of the great circle path
        final int numberOfPoints = greatCircleLength/100000;

        addGreatCircle(startPoint, endPoint, numberOfPoints);
    }

    /**
     * Draw a great circle.
     * @param startPoint start point of the great circle
     * @param endPoint end point of the great circle
     * @param numberOfPoints number of points to calculate along the path
     */
    public void addGreatCircle(final GeoPoint startPoint, final GeoPoint endPoint, final int numberOfPoints) {
        //	adapted from page http://compastic.blogspot.co.uk/2011/07/how-to-draw-great-circle-on-map-in.html
        //	which was adapted from page http://maps.forum.nu/gm_flight_path.html

        // convert to radians
        final double lat1 = startPoint.getLatitude() * Math.PI / 180;
        final double lon1 = startPoint.getLongitude() * Math.PI / 180;
        final double lat2 = endPoint.getLatitude() * Math.PI / 180;
        final double lon2 = endPoint.getLongitude() * Math.PI / 180;

        final double d = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin((lat1 - lat2) / 2), 2) + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin((lon1 - lon2) / 2), 2)));
        double bearing = Math.atan2(Math.sin(lon1 - lon2) * Math.cos(lat2),
                Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2))
                / -(Math.PI / 180);
        bearing = bearing < 0 ? 360 + bearing : bearing;

        for (int i = 0, j = numberOfPoints + 1; i < j; i++) {
            final double f = 1.0 / numberOfPoints * i;
            final double A = Math.sin((1 - f) * d) / Math.sin(d);
            final double B = Math.sin(f * d) / Math.sin(d);
            final double x = A * Math.cos(lat1) * Math.cos(lon1) + B * Math.cos(lat2) * Math.cos(lon2);
            final double y = A * Math.cos(lat1) * Math.sin(lon1) + B * Math.cos(lat2) * Math.sin(lon2);
            final double z = A * Math.sin(lat1) + B * Math.sin(lat2);

            final double latN = Math.atan2(z, Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)));
            final double lonN = Math.atan2(y, x);
            addPoint((int) (latN / (Math.PI / 180) * 1E6), (int) (lonN / (Math.PI / 180) * 1E6));
        }
    }

    public Paint getPaint() {
        return mPaint;
    }

    public void setPaint(final Paint pPaint) {
        if (pPaint == null) {
            throw new IllegalArgumentException("pPaint argument cannot be null");
        }
        mPaint = pPaint;
    }

    public void clearPath() {
        this.mPoints = new ArrayList<Point>();
        this.mPointsPrecomputed = 0;
    }

    public void addPoint(final IGeoPoint aPoint) {
        addPoint((int) aPoint.getLatitude(), (int) aPoint.getLongitude());
    }

    public void addPoint(final int aLatitude, final int aLongitude) {
        mPoints.add(new Point(aLatitude, aLongitude));
    }

    public void addPoints(final IGeoPoint... aPoints) {
        for(final IGeoPoint point : aPoints) {
            addPoint(point);
        }
    }

    public void addPoints(final List<IGeoPoint> aPoints) {
        for(final IGeoPoint point : aPoints) {
            addPoint(point);
        }
    }

    public int getNumberOfPoints() {
        return this.mPoints.size();
    }

    /**
     * This method draws the line. Note - highly optimized to handle long paths, proceed with care.
     * Should be fine up to 10K points.
     */
    @Override
    public void draw(final Canvas canvas, final MapView mapView, final boolean shadow) {

        if (shadow) {
            return;
        }

        final int size = this.mPoints.size();
        if (size < 2) {
            // nothing to paint
            return;
        }

        final Projection pj = mapView.getProjection();

        // precompute new points to the intermediate projection.
        while (this.mPointsPrecomputed < size) {
            final Point pt = this.mPoints.get(this.mPointsPrecomputed);
            //pj.toMapPixelsProjected(pt.x, pt.y, pt);

            this.mPointsPrecomputed++;
        }

        Point screenPoint0 = null; // points on screen
        Point screenPoint1;
        Point projectedPoint0; // points from the points list
        Point projectedPoint1;

        // clipping rectangle in the intermediate projection, to avoid performing projection.
        final Rect clipBounds = null;//pj.fromPixelsToProjected(pj.getScreenRect());

        mPath.rewind();
        projectedPoint0 = this.mPoints.get(size - 1);
        mLineBounds.set(projectedPoint0.x, projectedPoint0.y, projectedPoint0.x, projectedPoint0.y);

        for (int i = size - 2; i >= 0; i--) {
            // compute next points
            projectedPoint1 = this.mPoints.get(i);
            mLineBounds.union(projectedPoint1.x, projectedPoint1.y);

            if (!Rect.intersects(clipBounds, mLineBounds)) {
                // skip this line, move to next point
                projectedPoint0 = projectedPoint1;
                screenPoint0 = null;
                continue;
            }

            // the starting point may be not calculated, because previous segment was out of clip
            // bounds
            if (screenPoint0 == null) {
                screenPoint0 = null; //pj.toMapPixelsTranslated(projectedPoint0, this.mTempPoint1);
                mPath.moveTo(screenPoint0.x, screenPoint0.y);
            }

            screenPoint1 = null; //pj.toMapPixelsTranslated(projectedPoint1, this.mTempPoint2);

            // skip this point, too close to previous point
            if (Math.abs(screenPoint1.x - screenPoint0.x) + Math.abs(screenPoint1.y - screenPoint0.y) <= 1) {
                continue;
            }

            mPath.lineTo(screenPoint1.x, screenPoint1.y);

            // update starting point to next position
            projectedPoint0 = projectedPoint1;
            screenPoint0.x = screenPoint1.x;
            screenPoint0.y = screenPoint1.y;
            mLineBounds.set(projectedPoint0.x, projectedPoint0.y, projectedPoint0.x, projectedPoint0.y);
        }

        canvas.drawPath(mPath, this.mPaint);
    }
}
