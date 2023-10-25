package navi_go.view;

import android.location.LocationListener;

import org.osmdroid.views.overlay.compass.IOrientationProvider;


public abstract class ISpeedConsumer implements LocationListener, IOrientationProvider {

        /**
         *
         * @param speed this is speed in km/h
         * */
        public abstract void onSpeedChanged(int speed);

}
