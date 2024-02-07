package navi_go.model;

import android.os.Handler;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;

public class HttpGetRequestRepository<T> {

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

    public interface RequestCallback<T> {
        void onComplete(Result<T> result);
    }

    public interface Parser<T> {
        T parse(String string);
    }


    private final Executor executor;


    public HttpGetRequestRepository(Executor executor) {
        this.executor = executor;
    }

    public void makeAsyncRequest(String urlString, Parser<T> parser, RequestCallback<T> callback, Handler resultHandler) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Result<T> result = makeSyncRequest(urlString, parser);
                    notifyResult(result, callback, resultHandler);
                } catch (Exception e) {
                    Result<T> error = new Result.Error<>(e);
                    notifyResult(error, callback, resultHandler);
                }
            }
        });
    }


    public Result<T> makeSyncRequest(String urlString, Parser<T> parser) {
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
            T resultTyped = parser.parse(result);

            return new Result.Success<T>(resultTyped);
        } catch (Exception e) {
            return new Result.Error<T>(e);
        }
    }


    private void notifyResult(
            final Result<T> result,
            final RequestCallback<T> callback,
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
