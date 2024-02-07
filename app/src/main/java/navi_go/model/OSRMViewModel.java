package navi_go.model;

import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;

import org.osmdroid.util.GeoPoint;

import navi_go.model.HttpGetRequestRepository.Result.Success;

public class OSRMViewModel {

    private static final String OSRM_URL = "project-osrm.org";
    private static final String OSRM_VERSION = "v1";
    private static final String OSRM_SERVICE = "router";
    private static final String OSRM_PROFILE_CAR = "driving";
    private static final String OSRM_PROFILE_BIKE = "bike";
    private static final String OSRM_PROFILE_FOOT = "foot";
    private static final String OSRM_OPTIONS = "steps=true&overview=simplified";

    private final HttpGetRequestRepository mOSRMRepository;
    private Handler resultHandler;
    private TextView textView;

    HttpGetRequestRepository.Parser<String> parser;
    HttpGetRequestRepository.RequestCallback<String> callback;


    public OSRMViewModel(HttpGetRequestRepository mOSRMRepository, Handler resultHandler, TextView textView) {
        this.mOSRMRepository = mOSRMRepository;
        this.resultHandler = resultHandler;
        this.textView = textView;

        callback = new HttpGetRequestRepository.RequestCallback<String>() {
            @Override
            public void onComplete(HttpGetRequestRepository.Result<String> result) {
                if(result instanceof Success) {
                    // Happy path
                    String s = ((HttpGetRequestRepository.Result.Success<String>)result).data;
                    textView.setText(s);
                } else {
                    // Show error in UI
                    Exception exc;
                    exc = ((HttpGetRequestRepository.Result.Error<String>)result).exception;
                    String s = exc.toString();
                    textView.setText(s);
                    //Toast.makeText(mainActivity.ctx, s, Toast.LENGTH_SHORT).show();
                }
            }
        };
        parser = new HttpGetRequestRepository.Parser<String>() {
            @Override
            public String parse(String string) {
                return null;
            }
        };
    }


    public String getOsrmUrl(GeoPoint departure, GeoPoint destination) {
        String result = "https://" + OSRM_SERVICE + "." + OSRM_URL + "/"+ OSRM_VERSION + "/" + OSRM_PROFILE_CAR + "/" +
                departure.getAltitude() + "," + departure.getLatitude() + ";" +
                destination.getAltitude() + "," + destination.getLatitude() + "?" +
                OSRM_OPTIONS;
        return result;
    }

    public  void makeOSRMRoutingRequest(GeoPoint departure, GeoPoint destination, HttpGetRequestRepository.RequestCallback<String> callback) {
        if(callback == null)
            return;
        String url = getOsrmUrl(departure, destination);

        mOSRMRepository.makeAsyncRequest(url, parser, this.callback, resultHandler);
    }

};
