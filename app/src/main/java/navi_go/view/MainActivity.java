package navi_go.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.HandlerCompat;
import androidx.preference.PreferenceManager;

import com.example.osmandroid.R;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.MinimapOverlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import navi_go.model.HttpGetRequestRepository;
import navi_go.model.OSRMViewModel;

public class MainActivity extends AppCompatActivity {

    /** Les constants utils */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    public static float COMPASS_POSITION_X = 350.0f;
    public static float COMPASS_POSITION_Y = 400.0f;
    public static float COMPASS_RADIUS = 20.0f;


    /** Les composants de GPS */
    private MapView mMap = null;    // map
    private IMapController mMapController = null;    // map controller
    private CompassNorth mCompassNorth = null;      // compass
    private Polyline mPath = null;  // chemin à parcourir
    private Polyline mGonePath = null;  // chemin parcouru
    private SpeederOverlay mSpeeder = null; // compteur
    private Marker mActualPositionIcon = null;  // icon de la position actuelle
    private GeoPoint mActualPosition = null;    // geopoint actuelle
    private LocationManager mLocationManager = null;    // gestionnaire de GPS provider
    private LocationListener mLocationListener = null;  // listener de GPS


    private RotationGestureOverlay mRotationGestureOverlay = null;
    private float bearing = 0.f;

    /** UI de search destination GPS */
    private EditText mDestinationTextView;
    private Button mSearchButton;
    private TextView mResultTextView;

    /** gestion of current thread */
    ExecutorService mExecutorService = null;
    Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());



    // ===========================================================
    // Life Cycle
    // ===========================================================

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        setContentView(R.layout.activity_main);

        /* permissions */
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.INTERNET);
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
        requestPermissionsIfNecessary(permissions);
        /* end of permissions */

        /* Setting up map view */
        mMap = findViewById(R.id.mainMap);
        mMap.setTileSource(TileSourceFactory.MAPNIK);   // render
        mMap.setMaxZoomLevel(20.d);                     // max zoom level
        mMap.setMinZoomLevel(5.d);                      // min zoom level
        mMap.setVisibility(View.VISIBLE);               // visible
        mMap.setMultiTouchControls(true);               // control on multi touch
        mMap.setEnabled(true);
        mMapController = mMap.getController();
        mMapController.setZoom(20d);                    // zoom level 18
        /* end of setting up map view */

        /* Setting up LocationManager */
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        /* End of Setting up LocationManager */

        /* Search Path */
        mDestinationTextView = (EditText)findViewById(R.id.destinationEditText);
        mSearchButton = (Button)findViewById(R.id.searchButton);
        mResultTextView = findViewById(R.id.resultTextView);
        /* End of Search Path */

        /* Gestion current thread */
        mExecutorService = Executors.newFixedThreadPool(4);
        Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());
        /* End of Gestion current thread */

        /* request OSRMViewModel */
        GeoPoint start = new GeoPoint(2.3200410217200766,48.8588897);
        GeoPoint target = new GeoPoint(4.8059012,43.9492493);

        OSRMViewModel osrm = new OSRMViewModel(new HttpGetRequestRepository<>(mExecutorService), mainThreadHandler, mResultTextView);
        /* request OSRMViewModel */


        /* Setting up OnClickListener on SearchButton */
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        /* End of Setting up OnClickListener on SearchButton */



        /* request last know location on Location Service */
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        GeoPoint startPoint;
        if (location != null) {
            mActualPosition = new GeoPoint(location);
        } else {
            mActualPosition = new GeoPoint(10.7845174, 106.6570313);
        }
        //lat="10.7845174" lon="106.6570313 6 Tu Hai


        mMapController.setCenter(mActualPosition);  // map center in actual position


        mActualPositionIcon = new Marker(mMap);
        mActualPositionIcon.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mActualPositionIcon.setPosition(mActualPosition);
        mMap.getOverlays().add(mActualPositionIcon);


        /* LocationListener */
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                mActualPosition = new GeoPoint(location);
                mActualPositionIcon.setPosition(mActualPosition);
                if(location.hasSpeed()) {
                    mSpeeder.onSpeedChanged((int)(location.getSpeed()*3.6));
                }
                if(location.hasBearing()) {
                    float bearing = location.getBearing();
                    mMap.setMapOrientation(bearing);
                    mCompassNorth. onOrientationChanged(bearing, null);
                }
            }
        };
        /* End of LocationListener */


        /*
         * Initialize polyline to show the path
         */
        ArrayList<GeoPoint> geoPoints = new ArrayList<>();
        mPath = new Polyline();
        mPath.setPoints(geoPoints);
        mMap.getOverlays().add(mPath);  // add line to the overlays of map
        /*
         * End of Initialize polyline to show the path
         */


        /* North Compass */
        mCompassNorth = new CompassNorth(context, mMap);
        mCompassNorth.setCompassCenter(MainActivity.COMPASS_POSITION_X, MainActivity.COMPASS_POSITION_Y);
        mMap.getOverlays().add(mCompassNorth);
        mCompassNorth.enableCompass();
        mCompassNorth.onOrientationChanged(0f, null);
        /* End of North Compass */        /*

        /* Speeder */
        mSpeeder = new SpeederOverlay(context, mMap);
        mSpeeder.setSpeederCenter(50f, 600f);
        mMap.getOverlayManager().add(mSpeeder);
        mSpeeder.onMaxSpeedChanged(50);
        mSpeeder.enableSpeeder();
        /* End of Speeder */

        /* OSRM Request Callback */
        HttpGetRequestRepository.RequestCallback<String> callback = new HttpGetRequestRepository.RequestCallback<String>() {
            @Override
            public void onComplete(HttpGetRequestRepository.Result<String> result) {
                if(result instanceof HttpGetRequestRepository.Result.Success) {
                    // Happy path
                    String s = ((HttpGetRequestRepository.Result.Success<String>)result).data;
                    //String s =  ((Result<String>.Success<String>) result).data;
                    //mainActivity.mTxtViewLog.setText(s);
                    //Toast.makeText(mainActivity.getBaseContext(), s, Toast.LENGTH_SHORT).show();

                } else {
                    // Show error in UI
                    Exception exc;
                    exc = ((HttpGetRequestRepository.Result.Error<String>)result).exception;
                    String s = exc.toString();
                    //String s = ((Result.Error<String>) result).exception.toString();
                    //Toast.makeText(mainActivity.ctx, s, Toast.LENGTH_SHORT).show();
                }
            }
        };
        /* End of OSRM Request Callback */


        /* Rotation gesture */
        mRotationGestureOverlay = new RotationGestureOverlay(mMap) {
            @Override
            public void onRotate(float deltaAngle) {
                if(mCompassNorth.isNorthMode()) {
                    return;
                }
                super.onRotate(deltaAngle);
                mCompassNorth.onOrientationChanged(-mMap.getMapOrientation(), null);
            }
        };
        mMap.getOverlays().add(this.mRotationGestureOverlay);
        /* End of Rotation gesture */

        addLocationListener();
    }


    @Override
    public void onPause () {
        super.onPause();
        mMap.onPause();
        mLocationManager.removeUpdates(mLocationListener);
    }


    @Override
    public void onStop () {
        super.onStop();
        mLocationManager.removeUpdates(mLocationListener);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume () {
        super.onResume();
        mMap.onResume();
        if(mLocationListener != null) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 10, mLocationListener);
        }
    }

    @Override
    public void onDestroy() {
        mMap.onDetach();
        mLocationManager.removeUpdates(mLocationListener);
        super.onDestroy();
    }




    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            permissionsToRequest.add(permissions[i]);
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    // ===========================================================
    // Methods
    // ===========================================================



    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    /**
     * Add a LocationListener on Changed Location on LOCATION SERVICE
     */
    private void addLocationListener() {
        //mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //  if location has orientation
        //  if compass is not north mode, update orientation of compass and map
        //  if location has speed, update Speeder
        //  update the actual position with location
        //  add new position to polyline
        LocationListener mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                float speed_km_p_h = location.getSpeed() * 3.6f;

                long time_in_second = location.getTime() / 1000;
                long second = time_in_second % 60;
                long time_in_minute = time_in_second / 60;
                long minute = time_in_minute % 60;
                long time_in_hour = time_in_minute / 60;
                long hour = (time_in_hour + 7) % 24;

                //  if location has orientation
                if (location.hasBearing()) {
                    bearing = location.getBearing();
                    //  if compass is not north mode, update orientation of compass and map
                    if (!mCompassNorth.isNorthMode()) {
                        mMap.setMapOrientation(-bearing);
                        mCompassNorth.onOrientationChanged(bearing, null);
                    }
                }

                //  if location has speed, update Speeder
                if (location.hasSpeed()) {
                    mSpeeder.onSpeedChanged((int) (location.getSpeed() * 3.6f));
                }

                GeoPoint newPosition = new GeoPoint(location);

                //  update the actual position with location
                mActualPositionIcon.setPosition(newPosition);
                //  add new position to polyline
                mPath.addPoint(newPosition);

                //miniMap.getController().animateTo(newPosition);

            }
        };

        //  register location listener on GPS
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(getBaseContext(), "Pas de permission !", Toast.LENGTH_SHORT).show();
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 10, mLocationListener);
    }


    private void requestPermissionsIfNecessary(ArrayList<String> permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }
}