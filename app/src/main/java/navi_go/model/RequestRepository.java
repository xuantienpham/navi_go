package navi_go.model;

import android.os.Handler;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;

public class RequestRepository {
    private final String URL_GET = "https://nominatim.openstreetmap.org/search?q=Paris,France&format=jsonv2";

    private final Executor executor;

    public RequestRepository(Executor executor) {
        this.executor = executor;
    }

    public void makeRequest(String urlString, RequestCallback<String> callback, Handler resultHandler) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Result<String> result = makeSynchronousRequest(urlString);
                    notifyResult(result, callback, resultHandler);
                } catch (Exception e) {
                    Result<String> error = new Result.Error<>(e);
                    notifyResult(error, callback, resultHandler);
                }
            }
        });
    };


    public Result<String> makeSynchronousRequest(String urlString) {
        // HttpURLConnection logic
        try {
            URL url = new URL(urlString);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestMethod("GET");


            return new Result.Success<>(httpConnection.getInputStream().toString());
        } catch (Exception e) {
            return new Result.Error<>(e);
        }
    }


    private void notifyResult(
            final Result<String> result,
            final RequestCallback<String> callback,
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
