package com.example.osmandroid;

import android.location.LocationListener;
import android.location.LocationManager;

import org.osmdroid.views.overlay.compass.IOrientationProvider;


public abstract class ISpeedConsumer implements LocationListener, IOrientationProvider {

        /**
         *
         * @param speed this is speed in km/h
         * */
        public abstract void onSpeedChanged(int speed);

}
