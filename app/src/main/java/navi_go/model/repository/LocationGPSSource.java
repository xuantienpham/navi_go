package navi_go.model.repository;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class LocationGPSSource implements LocationListener {
    private LocationManager mLocationManager;
    private Location location;

    public LocationGPSSource(Activity activity) {
        mLocationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //return TODO;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100L, 10f,this);
        }
    }


    @Override
    public void onLocationChanged(@NonNull Location location) {
        this.location = location;
    }
}
