package navi_go.osrm;

import android.os.Handler;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;

public class OSRMRepository {
    private final String URL_GET = "https://nominatim.openstreetmap.org/search?q=Paris,France&format=jsonv2";

    private final Executor executor;
    private final Handler resultHandler;

    public OSRMRepository(Executor executor, Handler resultHandler) {
        this.executor = executor;
        this.resultHandler = resultHandler;
    }

    public void makeOSRMRequest(OSRMCallback<String> callback, Handler resultHandler) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Result<String> result = makeSynchronousOSRMRequest();
                    notifyResult(result, callback, resultHandler);
                } catch (Exception e) {
                    Result<String> error = new Result.Error<>(e);
                    notifyResult(error, callback, resultHandler);
                }
            }
        });
    };


    public Result<String> makeSynchronousOSRMRequest() {
        // HttpURLConnection logic
        try {
            URL url = new URL(URL_GET);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestMethod("GET");
            //httpConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            //httpConnection.setRequestProperty("Accept", "application/json");
            //httpConnection.setDoOutput(true);
            //httpConnection.getOutputStream().write(jsonBody.getBytes("utf-8"));

            return new Result.Success<>(httpConnection.getInputStream().toString());
        } catch (Exception e) {
            return new Result.Error<>(e);
        }
    }


    private void notifyResult(
            final Result<String> result,
            final OSRMCallback<String> callback,
            final Handler resultHandler
            ) {
        resultHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onComplete(result);
            }
        });
    }

}
