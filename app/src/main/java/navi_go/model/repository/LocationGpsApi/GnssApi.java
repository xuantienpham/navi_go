package navi_go.model.repository.LocationGpsApi;

import android.location.LocationListener;

public interface GnssApi {
    void addLocationListener(LocationListener listener);

    void removeLocationListener(LocationListener listener);

    void start();

    void pause();

    void resume();

    void stop();
}
