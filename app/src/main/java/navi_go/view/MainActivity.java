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
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.HandlerCompat;
import androidx.preference.PreferenceManager;

import com.example.osmandroid.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.arnaudguyon.xmltojsonlib.XmlToJson;
import navi_go.model.HttpGetRequestRepository;
import navi_go.model.OSRMRepository;


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
    private Marker mFromIcon = null;  // icon du départ
    private Marker mToIcon = null;  // icon de la destination
    private GeoPoint mActualPosition = null;    // geopoint actuelle
    private LocationManager mLocationManager = null;    // gestionnaire de GPS provider
    private LocationListener mLocationListener = null;  // listener de GPS
    private RotationGestureOverlay mRotationGestureOverlay = null;


    /** UI de search destination GPS */
    private EditText mDestinationTextView;
    private Button mSearchButton;
    private TextView mResultTextView;

    /** manager of current thread */
    private ExecutorService mExecutorService = null;
    private OSRMRepository mOSRMRepository;
    private HttpGetRequestRepository mHttpGetRequestRepository;


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
        mMap.setMaxZoomLevel(22.);                      // max zoom level 20.
        mMap.setMinZoomLevel(5.);                       // min zoom level 50.
        mMap.setVisibility(View.VISIBLE);               // visible
        mMap.setMultiTouchControls(true);               // control on multi touch
        mMapController = mMap.getController();
        mMapController.setZoom(21.);                    // zoom level 21.
        mMap.setEnabled(true);
        /* end of setting up map view */

        /* Setting up LocationManager */
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        /* End of Setting up LocationManager */

        /* Search Path */
        mDestinationTextView = (EditText) findViewById(R.id.destinationEditText);
        mSearchButton = (Button) findViewById(R.id.searchButton);
        mResultTextView = findViewById(R.id.resultTextView);
        mResultTextView.setBackgroundColor(Color.BLUE);
        mResultTextView.setAlpha(0.7f);
        mResultTextView.setTextColor(Color.CYAN);
        mResultTextView.setMovementMethod(new ScrollingMovementMethod());
        /* End of Search Path */

        /* Setting up for OSRM Request */
        mExecutorService = Executors.newFixedThreadPool(4);
        Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());

        // OSRM Request Callback
        HttpGetRequestRepository.Callback callback = new HttpGetRequestRepository.Callback() {
            @Override
            public void onComplete(HttpGetRequestRepository.Result<String> result) {
                if (result instanceof HttpGetRequestRepository.Result.Success) {
                    // Happy path
                    String s = ((HttpGetRequestRepository.Result.Success<String>) result).data;
                    try {
                        JSONObject osrm = new JSONObject(s);
                        ArrayList<GeoPoint> waypoints = new ArrayList<>();
                        String code = osrm.getString("code");
                        if(!code.equals("Ok")) {
                            Toast.makeText(getBaseContext(), "Error of response of OSRM's request", Toast.LENGTH_SHORT).show();
                            return;
                        } else {
                            osrm.remove("code");
                            String resultText = "";

                            // traiter le waypoints
                            {
                                JSONArray jsonwaypoints = osrm.getJSONArray("waypoints");
                                //mResultTextView.setText(jsonwaypoints.toString());
                                ArrayList<GeoPoint> pws = new ArrayList<>();
                                for(int i =0; i < jsonwaypoints.length(); i++) {
                                    JSONObject waypoint = jsonwaypoints.getJSONObject(i);

                                    JSONArray location = waypoint.getJSONArray("location");
                                    GeoPoint point = new GeoPoint(location.getDouble(1), location.getDouble(0));
                                    waypoints.add(point);

                                    Marker marker = new Marker(mMap);
                                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                    marker.setPosition(point);
                                    mMap.getOverlays().add(marker);

                                    pws.add(point);
                                }
                                GeoPoint start = pws.get(0);
                                GeoPoint end = pws.get(pws.size()-1);
                                int d = (int)start.distanceToAsDouble(end);
                                resultText += "Path from " + start.getLatitude() + "," + start.getLongitude() + " to " +
                                        end.getLatitude() + "," + end.getLongitude() + "\nDistance : " + d + "\n";
                            } // end of waypoints

                            // routes
                            {
                                JSONArray routes = osrm.getJSONArray("routes");
                                for(int i = 0; i < routes.length(); i++) {
                                    JSONObject route = routes.getJSONObject(0);
                                    int weight_route = route.getInt("weight");
                                    int duration_route = route.getInt("duration");
                                    int distance_route = route.getInt("distance");

                                    resultText += "route " + (i+1) + " : \n";
                                    //resultText += "    weight : " + weight_route + "\n";
                                    resultText += "    duration : " + duration_route + "\n";
                                    resultText += "    distance : " + distance_route + "\n";

                                    // legs
                                    ArrayList<GeoPoint> points = new ArrayList<>();
                                    JSONArray legs = route.getJSONArray("legs");
                                    for(int j=0; j < legs.length(); j++) {
                                        JSONObject leg = legs.getJSONObject(j);

                                        //leg.remove("steps");

                                        String summary = leg.getString("summary");
                                        int weight_leg = leg.getInt("weight");
                                        int duration_leg = leg.getInt("duration");
                                        int distance_leg = leg.getInt("distance");

                                        resultText += "\nleg " + (j+1) + ". Summary : " + summary + "\n";
                                        //resultText += "    weight : " + weight_leg + "\n";
                                        resultText += "    duration : " + duration_leg + ". ";
                                        resultText += "    distance : " + distance_leg + "\n";

                                        JSONObject annotations = leg.getJSONObject("annotation");
                                        JSONArray nodes = annotations.getJSONArray("nodes");




                                        // steps
                                        JSONArray steps = leg.getJSONArray("steps");
                                        for(int k = 0; k < steps.length(); k++) {
                                            JSONObject step = steps.getJSONObject(k);

                                            String name_step = step.getString("name");
                                            int duration_step = step.getInt("duration");
                                            int distance_step = step.getInt("distance");

                                            step.remove("geometry");
                                            step.remove("maneuver");
                                            step.remove("mode");
                                            step.remove("driving_side");
                                            //step.remove("intersections");
                                            step.remove("weight");

                                            resultText += "\nstep " + (k+1) + "\n";

                                            resultText += "    name : " + name_step + " ";
                                            resultText += "    duration : " + duration_step + " ";
                                            resultText += "    distance : " + distance_step + "\n";

                                            // intersections
                                            JSONArray intersections = step.getJSONArray("intersections");
                                            resultText += "\nintersections\n";
                                            for(int l = 0; l < intersections.length(); l++) {
                                                JSONObject intersection = intersections.getJSONObject(l);
                                                JSONArray location_intersection = intersection.getJSONArray("location");
                                                GeoPoint point = new GeoPoint(location_intersection.getDouble(1), location_intersection.getDouble(0));
                                                points.add(point);
                                                //resultText += "    { " + point.getLatitude() + " , " + point.getLongitude() + " }" + "\n";
                                            } // end for intersections
                                        } // end for steps
                                    } // end for legs


                                    mGonePath.setPoints(points);

                                } // end for routes

                                mResultTextView.setText(resultText);
                            } // end of routes

                            osrm.remove("routes");

                        }
                    } catch (JSONException e) {
                        mResultTextView.setText(e.toString());
                        Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Show error in UI
                    Exception exc;
                    exc = ((HttpGetRequestRepository.Result.Error<String>) result).exception;
                    String s = exc.toString();
                    Toast.makeText(getBaseContext(), exc.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };
        // End of OSRM Request Callback

        mHttpGetRequestRepository = new HttpGetRequestRepository(mExecutorService);
        mOSRMRepository = new OSRMRepository(mHttpGetRequestRepository, callback, mainThreadHandler);
        /* End of Setting up for OSRM Request */


        /* Setting up OnClickListener on SearchButton */
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String xmlString0 = "<osm version=\"0.6\" generator=\"CGImap 0.8.10 (658869 spike-06.openstreetmap.org)\" copyright=\"OpenStreetMap and contributors\" attribution=\"http://www.openstreetmap.org/copyright\" license=\"http://opendatacommons.org/licenses/odbl/1-0/\">\n" +
                        "<way id=\"353926767\" visible=\"true\" version=\"6\" changeset=\"50011543\" timestamp=\"2017-07-03T14:57:50Z\" user=\"Manoj Thapa\" uid=\"781054\">\n" +
                        "<nd ref=\"3597182934\"/>\n" +
                        "<nd ref=\"4878061368\"/>\n" +
                        "<nd ref=\"3597182931\"/>\n" +
                        "<nd ref=\"3597182930\"/>\n" +
                        "<nd ref=\"3597182925\"/>\n" +
                        "<nd ref=\"3597182923\"/>\n" +
                        "<nd ref=\"4938798168\"/>\n" +
                        "<nd ref=\"3597182921\"/>\n" +
                        "<nd ref=\"3597182919\"/>\n" +
                        "<nd ref=\"4878048010\"/>\n" +
                        "<nd ref=\"4878048542\"/>\n" +
                        "<nd ref=\"4878048576\"/>\n" +
                        "<nd ref=\"4878048592\"/>\n" +
                        "<nd ref=\"4878048605\"/>\n" +
                        "<nd ref=\"4878048622\"/>\n" +
                        "<nd ref=\"4878048617\"/>\n" +
                        "<nd ref=\"4878048598\"/>\n" +
                        "<nd ref=\"4878048593\"/>\n" +
                        "<nd ref=\"4878048589\"/>\n" +
                        "<nd ref=\"4878048586\"/>\n" +
                        "<nd ref=\"4878048578\"/>\n" +
                        "<nd ref=\"4878048564\"/>\n" +
                        "<nd ref=\"4878048551\"/>\n" +
                        "<nd ref=\"4878048546\"/>\n" +
                        "<nd ref=\"4878048550\"/>\n" +
                        "<nd ref=\"4878048554\"/>\n" +
                        "<nd ref=\"4878048567\"/>\n" +
                        "<nd ref=\"4878048573\"/>\n" +
                        "<nd ref=\"4878048575\"/>\n" +
                        "<nd ref=\"4878048580\"/>\n" +
                        "<nd ref=\"4878048581\"/>\n" +
                        "<nd ref=\"4878048582\"/>\n" +
                        "<nd ref=\"4878048590\"/>\n" +
                        "<nd ref=\"4878048596\"/>\n" +
                        "<nd ref=\"4878048616\"/>\n" +
                        "<nd ref=\"4878048625\"/>\n" +
                        "<nd ref=\"4878048637\"/>\n" +
                        "<nd ref=\"4878048645\"/>\n" +
                        "<nd ref=\"4878048648\"/>\n" +
                        "<nd ref=\"4878048657\"/>\n" +
                        "<nd ref=\"4878048659\"/>\n" +
                        "<nd ref=\"4878048661\"/>\n" +
                        "<nd ref=\"4878048665\"/>\n" +
                        "<nd ref=\"4935124797\"/>\n" +
                        "<tag k=\"highway\" v=\"track\"/>\n" +
                        "<tag k=\"source:geometry\" v=\"Bing\"/>\n" +
                        "</way>\n" +
                        "</osm>";

                String xmlStringnodes = "<osm version=\"0.6\" generator=\"CGImap 0.8.10 (2884143 spike-08.openstreetmap.org)\" copyright=\"OpenStreetMap and contributors\" attribution=\"http://www.openstreetmap.org/copyright\" license=\"http://opendatacommons.org/licenses/odbl/1-0/\">\n" +
                        "<node id=\"366372109\" visible=\"true\" version=\"6\" changeset=\"65952752\" timestamp=\"2019-01-02T09:27:11Z\" user=\"tonytran\" uid=\"2513747\" lat=\"10.7833862\" lon=\"106.6613755\"/>\n" +
                        "<node id=\"366384637\" visible=\"true\" version=\"3\" changeset=\"72510039\" timestamp=\"2019-07-22T11:08:42Z\" user=\"grabtaxi18\" uid=\"10141211\" lat=\"10.7837818\" lon=\"106.6617657\"/>\n" +
                        "<node id=\"366414715\" visible=\"true\" version=\"5\" changeset=\"48948475\" timestamp=\"2017-05-24T13:54:15Z\" user=\"TuanIfan\" uid=\"625303\" lat=\"10.7846817\" lon=\"106.6568180\"/>\n" +
                        "<node id=\"366416518\" visible=\"true\" version=\"2\" changeset=\"45172507\" timestamp=\"2017-01-14T21:30:25Z\" user=\"Gã Trùm\" uid=\"5083656\" lat=\"10.7828795\" lon=\"106.6602062\"/>\n" +
                        "<node id=\"366419588\" visible=\"true\" version=\"4\" changeset=\"54783808\" timestamp=\"2017-12-20T11:23:40Z\" user=\"ff5722\" uid=\"3450290\" lat=\"10.7833851\" lon=\"106.6579962\"/>\n" +
                        "<node id=\"366422630\" visible=\"true\" version=\"3\" changeset=\"66934693\" timestamp=\"2019-02-05T15:04:50Z\" user=\"KimChinhTri\" uid=\"4892796\" lat=\"10.7826008\" lon=\"106.6599135\"/>\n" +
                        "<node id=\"366440484\" visible=\"true\" version=\"3\" changeset=\"66934693\" timestamp=\"2019-02-05T15:04:50Z\" user=\"KimChinhTri\" uid=\"4892796\" lat=\"10.7824417\" lon=\"106.6591612\"/>\n" +
                        "<node id=\"366449169\" visible=\"true\" version=\"2\" changeset=\"23114700\" timestamp=\"2014-06-24T04:52:31Z\" user=\"QuangDBui@TMA\" uid=\"1276367\" lat=\"10.7824966\" lon=\"106.6590825\"/>\n" +
                        "<node id=\"366449867\" visible=\"true\" version=\"2\" changeset=\"45172507\" timestamp=\"2017-01-14T21:30:25Z\" user=\"Gã Trùm\" uid=\"5083656\" lat=\"10.7831076\" lon=\"106.6604158\"/>\n" +
                        "<node id=\"366461271\" visible=\"true\" version=\"2\" changeset=\"45172507\" timestamp=\"2017-01-14T21:30:26Z\" user=\"Gã Trùm\" uid=\"5083656\" lat=\"10.7827199\" lon=\"106.6600419\"/>\n" +
                        "<node id=\"366469192\" visible=\"true\" version=\"3\" changeset=\"66934693\" timestamp=\"2019-02-05T15:04:50Z\" user=\"KimChinhTri\" uid=\"4892796\" lat=\"10.7825391\" lon=\"106.6597448\"/>\n" +
                        "<node id=\"366473378\" visible=\"true\" version=\"3\" changeset=\"66934693\" timestamp=\"2019-02-05T15:04:50Z\" user=\"KimChinhTri\" uid=\"4892796\" lat=\"10.7824773\" lon=\"106.6594000\"/>\n" +
                        "<node id=\"2930594545\" visible=\"true\" version=\"2\" changeset=\"88476708\" timestamp=\"2020-07-24T20:41:57Z\" user=\"jlbpxt\" uid=\"11536604\" lat=\"10.7835416\" lon=\"106.6563968\"/>\n" +
                        "<node id=\"2930594546\" visible=\"true\" version=\"2\" changeset=\"88476708\" timestamp=\"2020-07-24T20:41:57Z\" user=\"jlbpxt\" uid=\"11536604\" lat=\"10.7844852\" lon=\"106.6567794\"/>\n" +
                        "<node id=\"2930594547\" visible=\"true\" version=\"2\" changeset=\"88476708\" timestamp=\"2020-07-24T20:41:57Z\" user=\"jlbpxt\" uid=\"11536604\" lat=\"10.7841938\" lon=\"106.6566810\"/>\n" +
                        "<node id=\"2930594548\" visible=\"true\" version=\"1\" changeset=\"23115395\" timestamp=\"2014-06-24T06:27:58Z\" user=\"QuangDBui@TMA\" uid=\"1276367\" lat=\"10.7837800\" lon=\"106.6564912\"/>\n" +
                        "<node id=\"2930594550\" visible=\"true\" version=\"2\" changeset=\"88476708\" timestamp=\"2020-07-24T20:41:57Z\" user=\"jlbpxt\" uid=\"11536604\" lat=\"10.7839267\" lon=\"106.6565539\"/>\n" +
                        "<node id=\"4120156883\" visible=\"true\" version=\"1\" changeset=\"38544918\" timestamp=\"2016-04-14T04:23:48Z\" user=\"Nguyen Thanh\" uid=\"3588291\" lat=\"10.7840709\" lon=\"106.6633410\"/>\n" +
                        "<node id=\"4120156884\" visible=\"true\" version=\"1\" changeset=\"38544918\" timestamp=\"2016-04-14T04:23:48Z\" user=\"Nguyen Thanh\" uid=\"3588291\" lat=\"10.7836995\" lon=\"106.6639671\"/>\n" +
                        "<node id=\"4120156888\" visible=\"true\" version=\"2\" changeset=\"72510039\" timestamp=\"2019-07-22T11:08:42Z\" user=\"grabtaxi18\" uid=\"10141211\" lat=\"10.7845413\" lon=\"106.6625985\"/>\n" +
                        "<node id=\"4608562274\" visible=\"true\" version=\"2\" changeset=\"48948475\" timestamp=\"2017-05-24T13:54:16Z\" user=\"TuanIfan\" uid=\"625303\" lat=\"10.7826856\" lon=\"106.6573621\"/>\n" +
                        "<node id=\"4608562277\" visible=\"true\" version=\"4\" changeset=\"66934693\" timestamp=\"2019-02-05T15:04:50Z\" user=\"KimChinhTri\" uid=\"4892796\" lat=\"10.7826417\" lon=\"106.6589198\"/>\n" +
                        "<node id=\"4608562278\" visible=\"true\" version=\"3\" changeset=\"54761035\" timestamp=\"2017-12-19T14:19:43Z\" user=\"ff5722\" uid=\"3450290\" lat=\"10.7825639\" lon=\"106.6590066\"/>\n" +
                        "<node id=\"4618443198\" visible=\"true\" version=\"3\" changeset=\"65952752\" timestamp=\"2019-01-02T09:27:11Z\" user=\"tonytran\" uid=\"2513747\" lat=\"10.7833332\" lon=\"106.6613290\"/>\n" +
                        "<node id=\"4878046785\" visible=\"true\" version=\"2\" changeset=\"72510039\" timestamp=\"2019-07-22T11:08:42Z\" user=\"grabtaxi18\" uid=\"10141211\" lat=\"10.7845790\" lon=\"106.6625460\"/>\n" +
                        "<node id=\"4903697613\" visible=\"true\" version=\"1\" changeset=\"49363923\" timestamp=\"2017-06-08T11:13:08Z\" user=\"tonytran\" uid=\"2513747\" lat=\"10.7830381\" lon=\"106.6605593\"/>\n" +
                        "<node id=\"4903697614\" visible=\"true\" version=\"1\" changeset=\"49363923\" timestamp=\"2017-06-08T11:13:08Z\" user=\"tonytran\" uid=\"2513747\" lat=\"10.7829775\" lon=\"106.6607256\"/>\n" +
                        "<node id=\"4903697615\" visible=\"true\" version=\"3\" changeset=\"65952768\" timestamp=\"2019-01-02T09:28:26Z\" user=\"tonytran\" uid=\"2513747\" lat=\"10.7828728\" lon=\"106.6608999\"/>\n" +
                        "<node id=\"5092125140\" visible=\"true\" version=\"2\" changeset=\"88476708\" timestamp=\"2020-07-24T20:41:57Z\" user=\"jlbpxt\" uid=\"11536604\" lat=\"10.7843384\" lon=\"106.6567404\"/>\n" +
                        "<node id=\"5092125149\" visible=\"true\" version=\"1\" changeset=\"51884146\" timestamp=\"2017-09-09T16:05:17Z\" user=\"KimChinhTri\" uid=\"4892796\" lat=\"10.7833282\" lon=\"106.6566296\"/>\n" +
                        "<node id=\"5092125157\" visible=\"true\" version=\"2\" changeset=\"66934693\" timestamp=\"2019-02-05T15:04:51Z\" user=\"KimChinhTri\" uid=\"4892796\" lat=\"10.7831845\" lon=\"106.6567965\"/>\n" +
                        "<node id=\"5092125158\" visible=\"true\" version=\"1\" changeset=\"51884146\" timestamp=\"2017-09-09T16:05:17Z\" user=\"KimChinhTri\" uid=\"4892796\" lat=\"10.7829882\" lon=\"106.6570171\"/>\n" +
                        "<node id=\"5092125160\" visible=\"true\" version=\"1\" changeset=\"51884146\" timestamp=\"2017-09-09T16:05:17Z\" user=\"KimChinhTri\" uid=\"4892796\" lat=\"10.7830122\" lon=\"106.6569898\"/>\n" +
                        "<node id=\"5295492410\" visible=\"true\" version=\"2\" changeset=\"66934693\" timestamp=\"2019-02-05T15:04:51Z\" user=\"KimChinhTri\" uid=\"4892796\" lat=\"10.7828713\" lon=\"106.6585932\"/>\n" +
                        "<node id=\"5295492420\" visible=\"true\" version=\"1\" changeset=\"54783808\" timestamp=\"2017-12-20T11:23:33Z\" user=\"ff5722\" uid=\"3450290\" lat=\"10.7828670\" lon=\"106.6575266\"/>\n" +
                        "<node id=\"5772478041\" visible=\"true\" version=\"1\" changeset=\"60894603\" timestamp=\"2018-07-20T07:37:45Z\" user=\"akhil2\" uid=\"7795526\" lat=\"10.7851391\" lon=\"106.6568613\"/>\n" +
                        "<node id=\"5772478042\" visible=\"true\" version=\"1\" changeset=\"60894603\" timestamp=\"2018-07-20T07:37:45Z\" user=\"akhil2\" uid=\"7795526\" lat=\"10.7850567\" lon=\"106.6568772\"/>\n" +
                        "<node id=\"5772478043\" visible=\"true\" version=\"1\" changeset=\"60894603\" timestamp=\"2018-07-20T07:37:45Z\" user=\"akhil2\" uid=\"7795526\" lat=\"10.7848665\" lon=\"106.6568529\"/>\n" +
                        "<node id=\"5772478571\" visible=\"true\" version=\"1\" changeset=\"60894603\" timestamp=\"2018-07-20T07:37:45Z\" user=\"akhil2\" uid=\"7795526\" lat=\"10.7830529\" lon=\"106.6576951\"/>\n" +
                        "<node id=\"6260063331\" visible=\"true\" version=\"1\" changeset=\"66934693\" timestamp=\"2019-02-05T15:04:43Z\" user=\"KimChinhTri\" uid=\"4892796\" lat=\"10.7832164\" lon=\"106.6581781\"/>\n" +
                        "<node id=\"6260063337\" visible=\"true\" version=\"1\" changeset=\"66934693\" timestamp=\"2019-02-05T15:04:43Z\" user=\"KimChinhTri\" uid=\"4892796\" lat=\"10.7829440\" lon=\"106.6584898\"/>\n" +
                        "<node id=\"6260063338\" visible=\"true\" version=\"1\" changeset=\"66934693\" timestamp=\"2019-02-05T15:04:43Z\" user=\"KimChinhTri\" uid=\"4892796\" lat=\"10.7831010\" lon=\"106.6583102\"/>\n" +
                        "<node id=\"6260063340\" visible=\"true\" version=\"1\" changeset=\"66934693\" timestamp=\"2019-02-05T15:04:43Z\" user=\"KimChinhTri\" uid=\"4892796\" lat=\"10.7827270\" lon=\"106.6587985\"/>\n" +
                        "<node id=\"6260063345\" visible=\"true\" version=\"1\" changeset=\"66934693\" timestamp=\"2019-02-05T15:04:43Z\" user=\"KimChinhTri\" uid=\"4892796\" lat=\"10.7829063\" lon=\"106.6585435\"/>\n" +
                        "<node id=\"6260063370\" visible=\"true\" version=\"1\" changeset=\"66934693\" timestamp=\"2019-02-05T15:04:43Z\" user=\"KimChinhTri\" uid=\"4892796\" lat=\"10.7833931\" lon=\"106.6565556\"/>\n" +
                        "<node id=\"11442757425\" visible=\"true\" version=\"1\" changeset=\"145393477\" timestamp=\"2023-12-22T04:55:52Z\" user=\"Grab_Cong\" uid=\"18820325\" lat=\"10.7834156\" lon=\"106.6565315\"/>\n" +
                        "<node id=\"11442757427\" visible=\"true\" version=\"1\" changeset=\"145393477\" timestamp=\"2023-12-22T04:55:52Z\" user=\"Grab_Cong\" uid=\"18820325\" lat=\"10.7837848\" lon=\"106.6564932\"/>\n" +
                        "</osm>";

                String xmlStringnode = "<osm version=\"0.6\" generator=\"CGImap 0.8.10 (2884143 spike-08.openstreetmap.org)\" copyright=\"OpenStreetMap and contributors\" attribution=\"http://www.openstreetmap.org/copyright\" license=\"http://opendatacommons.org/licenses/odbl/1-0/\">\n" +
                        "<node id=\"11442757427\" visible=\"true\" version=\"1\" changeset=\"145393477\" timestamp=\"2023-12-22T04:55:52Z\" user=\"Grab_Cong\" uid=\"18820325\" lat=\"10.7837848\" lon=\"106.6564932\"/>\n" +
                        "</osm>";

                XmlToJson xmlToJson = new XmlToJson.Builder(xmlStringnodes).forceList("/osm/node").build();
                String s;

                //String tt = "<t>Tester beaucoup de choses</t>";
                //JSONObject o = XmlToJsonConverter.convertXmlToJson(tt);


                JSONObject jsonObject = xmlToJson.toJson();
                try {
                    JSONObject osm = jsonObject.getJSONObject("osm");

                    osm.remove("license");
                    osm.remove("copyright");
                    osm.remove("attribution");
                    osm.remove("generator");
                    osm.remove("version");

                    JSONArray nodes = osm.getJSONArray("node");

                    JSONObject node = nodes.getJSONObject(0);

                    s =  node.toString();

                    String id = node.getString("id");
                    String lon = node.getString("lon");
                    String lat = node.getString("lat");
                    String version = node.getString("version");

                    s = "id : " + id + " - lon : " + lon + " - lat : " + lat + " - version : " + version + "\n";

                    mResultTextView.setText(s);

                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                //s =  jsonObject.toString();

                //mResultTextView.setText(mDestinationTextView.getText());
                mResultTextView.setText(s);
            }
        });
        /* End of Setting up OnClickListener on SearchButton */


        /* Request last position */
        @SuppressLint("MissingPermission") Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location != null) {
            mActualPosition = new GeoPoint(location);
        } else {
            mActualPosition = new GeoPoint(106.6570313, 10.7845174);
        }
        //lat="10.7845174" lon="106.6570313 6 Tu Hai
        mMapController.setCenter(mActualPosition);  // map center in actual position
        mActualPositionIcon = new Marker(mMap);
        mActualPositionIcon.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mActualPositionIcon.setPosition(mActualPosition);
        mActualPositionIcon.setTextIcon("Here");
        mMap.getOverlays().add(mActualPositionIcon);
        /* End of Request last position */

        /* Setting up icons From and To */
        mToIcon = new Marker(mMap);
        mToIcon.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mToIcon.setPosition(mActualPosition);
        mMap.getOverlays().add(mToIcon);
        mToIcon = new Marker(mMap);
        mToIcon.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mToIcon.setPosition(mActualPosition);
        mMap.getOverlays().add(mToIcon);
        /* End of Setting up icons From and To */

        /* Initialize polyline mPath and mGonePath */
        mPath = new Polyline();
        mPath.getOutlinePaint().setColor(Color.BLUE);
        mPath.getOutlinePaint().setAntiAlias(true);
        mPath.setPoints(new ArrayList<>());
        mMap.getOverlays().add(mPath);

        mGonePath = new Polyline();
        mGonePath.getOutlinePaint().setColor(Color.GREEN);
        mGonePath.getOutlinePaint().setAntiAlias(true);
        mGonePath.setPoints(new ArrayList<>());
        mMap.getOverlays().add(mGonePath);
        /* End of Initialize polyline mPath and mGonePath */

        /* North Compass */
        mCompassNorth = new CompassNorth(context, mMap);
        mCompassNorth.setCompassCenter(MainActivity.COMPASS_POSITION_X, MainActivity.COMPASS_POSITION_Y);
        mMap.getOverlays().add(mCompassNorth);
        mCompassNorth.enableCompass();
        mCompassNorth.onOrientationChanged(0f, null);
        /* End of North Compass */

        /* Speeder */
        mSpeeder = new SpeederOverlay(context, mMap);
        mSpeeder.setSpeederCenter(50f, 600f);
        mMap.getOverlayManager().add(mSpeeder);
        mSpeeder.onMaxSpeedChanged(50);
        mSpeeder.enableSpeeder();
        /* End of Speeder */

        /* Rotation gesture */
        mRotationGestureOverlay = new RotationGestureOverlay(mMap) {
            @Override
            public void onRotate(float deltaAngle) {
                if (mCompassNorth.isNorthMode()) {
                    return;
                }
                super.onRotate(deltaAngle);
                mCompassNorth.onOrientationChanged(-mMap.getMapOrientation(), null);
            }
        };
        mMap.getOverlays().add(this.mRotationGestureOverlay);
        /* End of Rotation gesture */

        /* LocationListener */
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                GeoPoint oldPosition = mActualPosition;
                mActualPosition = new GeoPoint(location);

                mOSRMRepository.makeOSRMRoutingRequest(oldPosition, mActualPosition);

                mActualPositionIcon.setPosition(mActualPosition);
                mMapController.animateTo(mActualPosition);
                mPath.addPoint(mActualPosition);
                if (location.hasSpeed()) {
                    mSpeeder.onSpeedChanged((int) (location.getSpeed() * 3.6));
                }
                if (location.hasBearing()) {
                    float bearing = location.getBearing();
                    mMap.setMapOrientation(-bearing);
                    mCompassNorth.onOrientationChanged(bearing, null);
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
        /* End of LocationListener */
    }


    @Override
    public void onPause () {
        super.onPause();
        mMap.onPause();
        //mLocationManager.removeUpdates(mLocationListener);
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