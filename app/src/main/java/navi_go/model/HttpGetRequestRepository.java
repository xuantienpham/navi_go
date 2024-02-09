package navi_go.model;

import android.os.Handler;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpGetRequestRepository {

    public abstract static class Result<T> {
        private Result() {}

        public static final class Success<T> extends HttpGetRequestRepository.Result<T> {
            public T data;

            public Success(T data) {
                this.data = data;
            }
        }

        public static final class Error<T> extends HttpGetRequestRepository.Result<T> {
            public Exception exception;

            public Error(Exception exception) {
                this.exception = exception;
            }
        }
    }


    public interface Callback {
        void onComplete(Result<String> result);
    }


    public interface Parser<T> {
        T parse(String string);
    }


    private final Executor executor;

    /**
     * Constructor of repository of HTTP Request
     * @param executor loop of thread, used for each new request called.
     */
    public HttpGetRequestRepository(Executor executor) {
        if(executor != null) {
            this.executor = executor;
        } else {
            this.executor = Executors.newFixedThreadPool(4);
        }
    }


    /**
     * Make a asynchronous request on network by http in GET methode. When result is ready, callback is called w
     * @param urlString address of server http.
     * @param callback  return point of asynchronous call.
     * @param resultHandler thread execute the callback.
     */
    public void makeAsyncRequest(String urlString, Callback callback, Handler resultHandler) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Result<String> result = makeSyncRequest(urlString);
                    notifyResult(result, callback, resultHandler);
                } catch (Exception e) {
                    Result<String> error = new Result.Error<>(e);
                    notifyResult(error, callback, resultHandler);
                }
            }
        });
    }


    /**
     * make a request on network by http in GET methode. Call blocking thread. Don't use on UI thread.
     * If not, it throw NetWorkOnMainThreadException.
     * @param urlString address of server http.
     * @return result of request in String.
     */
    public Result<String> makeSyncRequest(String urlString) {
        // HttpURLConnection logic
        try {
            URL url = new URL(urlString);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestMethod("GET");

            InputStream inputStream = httpConnection.getInputStream();

            StringBuilder stringBuilder = new StringBuilder();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                stringBuilder.append(new String(buffer, 0, bytesRead));
            }
            String result = stringBuilder.toString();
            return new Result.Success<>(result);
        } catch (Exception e) {
            return new Result.Error<>(e);
        }
    }


    /**
     * notify result to callback that will be executed in handler.
     * @param result    result of request http.
     * @param callback  callback on return.
     * @param resultHandler thread execute callback.
     */
    private void notifyResult(
            final Result<String> result,
            final Callback callback,
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
