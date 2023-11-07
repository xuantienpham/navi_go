package navi_go.view;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
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

import navi_go.model.RequestRepository;
import navi_go.model.OSRMViewModel;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    public static float COMPASS_POSITION_X = 350.0f;
    public static float COMPASS_POSITION_Y = 400.0f;
    public static float COMPASS_RADIUS = 20.0f;
    private MapView mMapView = null;
    private LocationManager mLocationManager;
    private static double levelZoom;
    private Polyline polyline = null;
    private CompassNorth mCompassNorth = null;
    private RotationGestureOverlay mRotationGestureOverlay = null;
    private float bearing = 0.f;
    private float mScale;
    private SpeederOverlay mSpeederOverlay = null;
    private Marker actualPosition;
    private EditText mTxtViewFrom;
    private EditText mTxtViewTo;
    private Button mBtnSearch;
    public TextView mTxtViewLog;

    private static final String GET_URL = "https://nominatim.openstreetmap.org/search?q=Paris,France&format=jsonv2";
    private static final String USER_AGENT = "Mozilla/5.0";

    ExecutorService mExecutorService = null;
    Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());

    public Context ctx;// = getBaseContext();

    // ===========================================================
    // Life Cycle
    // ===========================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        setContentView(R.layout.activity_main);

        /*
         *  permissions
         */
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.INTERNET);
        permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
        requestPermissionsIfNecessary(permissions);
        /*
         *  end of permissions
         */

        // density of resolution
        mScale = context.getResources().getDisplayMetrics().density;

        /*
         * Setting up map view
         */
        mMapView = findViewById(R.id.mainMap);
        mMapView.setTileSource(TileSourceFactory.MAPNIK);    // render
        mMapView.setBuiltInZoomControls(true);               // zoomable
        mMapView.setMultiTouchControls(true);                // control on multi touch
        levelZoom = 20.0d;
        /*
         * end of setting up map view
         */


        // Search Path
        //mTxtViewFrom = (EditText)findViewById(R.id.editTexteFrom);
        mTxtViewTo = (EditText)findViewById(R.id.editTexteDestination);
        mBtnSearch = (Button)findViewById(R.id.btnSearch);
        mTxtViewLog = (TextView)findViewById(R.id.lblLog);

        SearchRoutesView routes = (SearchRoutesView)findViewById(R.id.layoutSearch);

        mBtnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTxtViewLog.setText(mTxtViewFrom.getText() + " " + mTxtViewTo.getText());
            }
        });


        /*
         * Connection to OSRM
         */

        mExecutorService = Executors.newFixedThreadPool(4);
        Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());

        RequestRepository osrmConnection = new RequestRepository(mExecutorService);
        OSRMViewModel osrmViewModel = new OSRMViewModel(osrmConnection, mainThreadHandler);
        osrmViewModel.makeOSRMRequest();
        /*
         * End of Connection to OSRM
         */

        // initialize the Location Manger
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // request last know location on Location Service
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

        /*
         * Setting up map view to last know location or default location (lat: 10.78456d, lon: 106.65679d)
         */
        GeoPoint startPoint;
        if (location != null) {
            startPoint = new GeoPoint(location);
        } else {
            startPoint = new GeoPoint(10.78456d, 106.65679d);
        }
        IMapController mapController = mMapView.getController();
        mapController.setCenter(startPoint);
        mapController.setZoom(MainActivity.levelZoom);
        /*
         * End of setting up map view to last know location or default location (lat: 10.78456d, lon: 106.65679d)
         */

        /*
         * Initialize marker of actual position
         */
        actualPosition = new Marker(mMapView);
        actualPosition.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mMapView.getOverlays().add(actualPosition);
        /*
         * End of initialize marker of actual position
         */

        /*
         * Initialize polyline to show the path
         */
        ArrayList<GeoPoint> geoPoints = new ArrayList<>();
        polyline = new Polyline();
        polyline.setPoints(geoPoints);
        polyline.setColor(Color.BLUE);
        polyline.setWidth(10f * mScale);
        mMapView.getOverlays().add(polyline);  // add line to the overlays of map
        /*
         * End of Initialize polyline to show the path
         */

        ctx = getBaseContext();

        /*
         *  initialize the North Compass
         */
        mCompassNorth = new CompassNorth(context, mMapView)
        {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView) {
                float x = e.getX();
                float y = e.getY();
                float centerX = MainActivity.COMPASS_POSITION_X * mScale;
                float centerY = MainActivity.COMPASS_POSITION_Y * mScale;
                float radius = MainActivity.COMPASS_RADIUS * mScale;

                String txt = //"Radius=" + Float.toString(radius) +
                        "Center X=" + Float.toString(centerX) +
                        "Center Y=" + Float.toString(centerY) +
                        "X=" + Float.toString(x) +
                        "Y=" + Float.toString(y);

                Toast.makeText(getBaseContext(), txt, Toast.LENGTH_SHORT).show();
                //  if( taped point is out of circle) do nothing
                if(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2) > Math.pow(radius, 2))
                    return false;

                boolean northMode = isNorthMode();
                setNorthMode( !northMode ); // toggle the mode
                mMapView.setMapOrientation(-getOrientation());
                mRotationGestureOverlay.setEnabled( northMode );    // toggle enable/disable of gesture rotation
                return true;
            }
        };
        mCompassNorth.setCompassCenter(MainActivity.COMPASS_POSITION_X, MainActivity.COMPASS_POSITION_Y);
        mMapView.getOverlays().add(mCompassNorth);
        mCompassNorth.enableCompass();
        mCompassNorth.onOrientationChanged(0f, null);
        /*
         *  End of Initialize the North Compass
         */

        /*
         *  Initialize Speeder
         */
        mSpeederOverlay = new SpeederOverlay(context, mMapView);
        mSpeederOverlay.setSpeederCenter(50f, 600f);
        mMapView.getOverlayManager().add(mSpeederOverlay);
        mSpeederOverlay.onMaxSpeedChanged(50);
        mSpeederOverlay.enableSpeeder();
        /*
         *  End of Initialize Speeder
         */

        /*
         *  Initialize rotation gesture
         */
        mRotationGestureOverlay = new RotationGestureOverlay(mMapView) {
            @Override
            public void onRotate(float deltaAngle) {
                super.onRotate(deltaAngle);
                mCompassNorth.onOrientationChanged(-mMapView.getMapOrientation(), null);
                //miniMap.setMapOrientation(mMapView.getMapOrientation());
                //miniMap.getController().zoomTo(mMapView.getZoomLevelDouble() - 3);
            }
        };
        mMapView.getOverlays().add(this.mRotationGestureOverlay);
        mMapView.setMultiTouchControls(true);
        /*
         *  End of Initialize rotation gesture
         */

        /*
         * Initialize mini-map
         */
        MinimapOverlay minimapOverlay = new MinimapOverlay(context, mMapView.getTileRequestCompleteHandler());
        minimapOverlay.setZoomDifference(4);
        minimapOverlay.setWidth(200);
        minimapOverlay.setHeight(200);
        //mMapView.getOverlays().add(minimapOverlay);
        /*
         * End of Initialize mini-map
         */

        addLocationListener();
    }


    @Override
    public void onPause () {
        super.onPause();
        mMapView.onPause();
    }


    @Override
    public void onResume () {
        super.onResume();
        mMapView.onResume();
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
                        mMapView.setMapOrientation(-bearing);
                        mCompassNorth.onOrientationChanged(bearing, null);
                    }
                }

                //  if location has speed, update Speeder
                if (location.hasSpeed()) {
                    mSpeederOverlay.onSpeedChanged((int) (location.getSpeed() * 3.6f));
                }

                GeoPoint newPosition = new GeoPoint(location);

                //  update the actual position with location
                actualPosition.setPosition(newPosition);
                //  add new position to polyline
                polyline.addPoint(newPosition);

                //miniMap.getController().animateTo(newPosition);

                RelativeLayout relativeLayout = (RelativeLayout)findViewById(R.id.main);
                int number = relativeLayout.getChildCount();
                //relativeLayout.getChildAt(00).



                String txt = //relativeLayout.getChildAt(0).getClass().getName() + " " +
                        //relativeLayout.getChildAt(1).getClass().getName() +
                //Integer.toString(number) +
                        "Lat : " + location.getLatitude() +
                        " - Lon : " + location.getLongitude() +
                        " - Alt : " + location.getAltitude() +
                        " - Time : " + hour + ":" + minute + ":" + second +
                        " - Bear : " + bearing + "°" +
                        " - Speed : " + speed_km_p_h + " km/h" +
                        " - Accuracy : " + 0 + " m";
                Toast.makeText(getBaseContext(), txt, Toast.LENGTH_SHORT).show();
                mMapView.getController().animateTo(newPosition, MainActivity.levelZoom, 200L);
                //miniMap.getController().animateTo(newPosition, MainActivity.levelZoom-3, 200L);
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
        //mLocationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 200, 10, mLocationListener);
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