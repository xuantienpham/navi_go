package navi_go.model;

import android.location.Location;

import androidx.annotation.NonNull;

import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;

import navi_go.Contract;
import navi_go.view.MainActivity;
import navi_go.view.SpeederOverlay;

public class Model implements Contract.Model {
    private float mOrientation = Float.NaN;
    private GeoPoint mFocusedPoint;
    private GeoPoint mActualPosition;
    private double mLevelZoom = 20d;
    private int mMaxSpeed = SpeederOverlay.NO_SPEED;
    private int mSpeed = SpeederOverlay.NO_SPEED;
    private boolean mNorthMode = false;


    // ===========================================================
    // Constructors
    // ===========================================================

    public Model(Location location) {
        if (location != null) {
            mFocusedPoint = new GeoPoint(location);

            if(location.hasBearing()) {
                mOrientation = location.getBearing();
            }
            if(location.hasSpeed()) {
                mSpeed = (int) (location.getSpeed() * 3.6f + 0.5f);
            }
        } else {
            mFocusedPoint = new GeoPoint(10.78456d, 106.65679d);
        }
        mActualPosition = new GeoPoint(mFocusedPoint);
    }


    // ===========================================================
    // Getter & Setter
    // ===========================================================

    public float getOrientation() {
        return mOrientation;
    }

    public void setOrientation(float mOrientation) {
        this.mOrientation = mOrientation;
    }

    public GeoPoint getFocusedPoint() {
        return mFocusedPoint;
    }

    public void setFocusedPoint(GeoPoint mFocusedPoint) {
        this.mFocusedPoint = mFocusedPoint;
    }

    public GeoPoint getActualPosition() {
        return mActualPosition;
    }

    public void setActualPosition(GeoPoint mActualPosition) {
        this.mActualPosition = mActualPosition;
    }

    public double getLevelZoom() {
        return mLevelZoom;
    }

    public void setLevelZoom(double mLevelZoom) {
        this.mLevelZoom = mLevelZoom;
    }

    public int getMaxSpeed() {
        return mMaxSpeed;
    }

    public void setMaxSpeed(int mMaxSpeed) {
        this.mMaxSpeed = mMaxSpeed;
    }

    public int getSpeed() {
        return mSpeed;
    }

    public void setSpeed(int mSpeed) {
        this.mSpeed = mSpeed;
    }

    public boolean isNorthMode() {
        return mNorthMode;
    }

    public void setNorthMode(boolean northMode) {
        mNorthMode = northMode;
    }


    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================

    public void onLocationChanged(@NonNull LocationInfo location) {
        mOrientation = location.mInternOrientation;
        mFocusedPoint = location.mFocusedPoint;
        mActualPosition = location.mActualPosition;
        mLevelZoom = location.mLevelZoom;
        mMaxSpeed = location.mMaxSpeed;
        mSpeed = location.mSpeed;
        mNorthMode = location.mNorthMode;
    }

    // ===========================================================
    // Methods
    // ===========================================================

    public void onOrientationChanged (float orientation) {
        if(isNorthMode())   return;
    }


    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    public class LocationInfo {
        private float mInternOrientation;
        private GeoPoint mFocusedPoint;
        private GeoPoint mActualPosition;
        private double mLevelZoom;
        private int mMaxSpeed;
        private int mSpeed;
        private boolean mNorthMode;

        // ===========================================================
        // Constructors
        // ===========================================================

        public LocationInfo (float orientation, GeoPoint focusedPoint, int speed) {
            mOrientation = orientation;
            mFocusedPoint = focusedPoint;
            mSpeed = speed;
        }


        // ===========================================================
        // Getter & Setter
        // ===========================================================

        public float getOrientation () {
            return mOrientation;
        }

        public GeoPoint getFocusedPoint() {
            return mFocusedPoint;
        }

        public int getSpeed() {
            return mSpeed;
        }
    }


}
