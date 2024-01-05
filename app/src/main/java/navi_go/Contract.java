package navi_go;

import androidx.annotation.NonNull;

import org.osmdroid.util.GeoPoint;

import navi_go.model.Model.LocationInfo;

public interface Contract {
    interface View {
        void onOrientationChanged(float orientation);

        void onSpeedChanged(int speed);
        void onMaxSpeedChanged(int maxSpeed);
        void onAllSpeedChanged(int speed, int maxSpeed);

        boolean isCompassNorthMode();
        void setCompassNorthMode(boolean northMode);

        void onGeoPointFocusedChanged(GeoPoint point);
        void onLevelZoomChanged(double levelZoom);
    }


    interface Model {
        void onLocationChanged(@NonNull LocationInfo location);
    }


    interface Presenter {
        void onCompassTaped();
        void onRotate(float deltaAngle);
        void onModelChanged(@NonNull LocationInfo location);
    }
}
