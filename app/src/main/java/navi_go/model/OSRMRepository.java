package navi_go.model;

import android.os.Handler;

import org.osmdroid.util.GeoPoint;

public class OSRMRepository {

    public static final String OSRM_URL = "project-osrm.org";
    public static final String OSRM_VERSION = "v1";
    public static final String OSRM_SERVICE_ROUTE = "route";
    public static final String OSRM_PROFILE_CAR = "driving";
    public static final String OSRM_PROFILE_BIKE = "bike";
    public static final String OSRM_PROFILE_FOOT = "foot";
    public static final String OSRM_ROUTE_OPTIONS = "steps=true&overview=simplified&annotations=nodes,speed";

    private final HttpGetRequestRepository mHttpRepository;
    private final HttpGetRequestRepository.Callback callback;
    private Handler resultHandler;


    /**
     * Constructor OSRMRepository pour make a request on OSRM : osrm-project.org
     * @param mHttpRepository   HttpGetRequestRepository.
     * @param callback          callback on return.
     * @param resultHandler     resultHandler.
     */
    public OSRMRepository(HttpGetRequestRepository mHttpRepository, HttpGetRequestRepository.Callback callback, Handler resultHandler) {
        this.mHttpRepository = mHttpRepository;
        this.callback = callback;
        this.resultHandler = resultHandler;
    }


    /**
     * make url for request Http on OSRM : osrm-project.org
     * @param departure     departure's GePoint.
     * @param destination   destination's GePoint.
     * @return  String, url.
     */
    public String getOSRMUrl(GeoPoint departure, GeoPoint destination) {
        String result = "https://router." + OSRM_URL + "/" + OSRM_SERVICE_ROUTE + "/" + OSRM_VERSION + "/" + OSRM_PROFILE_CAR + "/" +
                departure.getLongitude() + "," + departure.getLatitude() + ";" +
                destination.getLongitude() + "," + destination.getLatitude() + "?" +
                OSRM_ROUTE_OPTIONS;
        return result;
    }

    /**
     * make a asynchronous request, calculating path from departure to destination. When result is ready, callback will be executed.
     * Non call in UI thread. If not, it throws NetWorkOnMainThreadException.
     * @param departure     departure's GeoPoint.
     * @param destination   destination's GeoPoint.
     */
    public  void makeOSRMRoutingRequest(GeoPoint departure, GeoPoint destination) {
        if(callback == null)
            return;
        String url = getOSRMUrl(departure, destination);
        mHttpRepository.makeAsyncRequest(url, callback, resultHandler);
    }

};
