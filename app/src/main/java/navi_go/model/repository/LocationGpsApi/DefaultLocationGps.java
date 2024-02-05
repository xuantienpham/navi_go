package navi_go.model.repository.LocationGpsApi;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

public class DefaultLocationGps implements GnssApi {

    private boolean isEnable = false;
    private Context context;
    private LocationManager mLocationManager;
    private MyLocationListener myLocationListener = new MyLocationListener();
    private ArrayList<LocationListener> mListeners = new ArrayList<>();


    private class MyLocationListener implements LocationListener {

        public MyLocationListener() {
        }


        @Override
        public void onLocationChanged(@NonNull Location location) {
            for (LocationListener listener : mListeners) {
                listener.onLocationChanged(location);
            }
        }
    }


    public DefaultLocationGps(AppCompatActivity context) {
        this.context = context;
        myLocationListener = new MyLocationListener();
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }


    @Override
    public void addLocationListener(LocationListener listener) {
        if (listener != null) {
            mListeners.add(listener);
        }
    }

    @Override
    public void removeLocationListener(LocationListener listener) {
        mListeners.remove(listener);
        if(mListeners.size() == 0) {
            stop();
        }
    }

    @Override
    public void start() {
        if(isEnable)
            return;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }
        isEnable = true;
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLocationListener);
    }

    @Override
    public void pause() {
        mLocationManager.removeUpdates(myLocationListener);
        isEnable = false;
    }

    @Override
    public void resume() {
        start();
    }


    @Override
    public void stop() {
        pause();
    }
}
